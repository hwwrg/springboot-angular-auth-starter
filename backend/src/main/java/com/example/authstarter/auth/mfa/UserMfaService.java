package com.example.authstarter.auth.mfa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * DB-backed TOTP enrollment, verification, and recovery-code lifecycle. Only
 * available when auth persistence is configured; break-glass/configured
 * principals (which have no persisted credentials) are never MFA-gated.
 */
@Service
@EnableConfigurationProperties(MfaProperties.class)
@ConditionalOnExpression("!environment.getProperty('spring.autoconfigure.exclude', '').contains('DataSourceAutoConfiguration')")
public class UserMfaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserMfaService.class);

    private final JdbcClient jdbcClient;
    private final TotpService totpService;
    private final RecoveryCodeGenerator recoveryCodeGenerator;
    private final MfaProperties properties;

    public UserMfaService(
            JdbcClient jdbcClient,
            TotpService totpService,
            RecoveryCodeGenerator recoveryCodeGenerator,
            MfaProperties properties) {
        this.jdbcClient = jdbcClient;
        this.totpService = totpService;
        this.recoveryCodeGenerator = recoveryCodeGenerator;
        this.properties = properties;
    }

    public boolean isMfaEnabled(UUID userId) {
        return jdbcClient.sql("select status from user_mfa_totp where user_id = :userId")
                .param("userId", userId)
                .query(String.class)
                .optional()
                .filter("ENABLED"::equals)
                .isPresent();
    }

    public MfaStatus status(UUID userId) {
        Optional<String> totpStatus = jdbcClient.sql("select status from user_mfa_totp where user_id = :userId")
                .param("userId", userId)
                .query(String.class)
                .optional();
        if (totpStatus.isEmpty()) {
            return MfaStatus.disabled();
        }
        boolean enabled = "ENABLED".equals(totpStatus.get());
        return new MfaStatus(enabled, !enabled, enabled ? remainingRecoveryCodes(userId) : 0);
    }

    /**
     * Starts (or restarts) enrollment by storing a fresh PENDING secret,
     * replacing any prior unconfirmed attempt. Rejected when MFA is already
     * enabled to avoid silently rotating a working configuration.
     */
    @Transactional
    public MfaEnrollment startEnrollment(UUID userId, String accountEmail) {
        if (isMfaEnabled(userId)) {
            throw new MfaOperationException("Multi-factor authentication is already enabled.");
        }

        String secret = totpService.generateSecret();
        jdbcClient.sql("""
                        insert into user_mfa_totp (user_id, secret, status, confirmed_at, created_at, updated_at)
                        values (:userId, :secret, 'PENDING', null, now(), now())
                        on conflict (user_id) do update
                           set secret = excluded.secret,
                               status = 'PENDING',
                               confirmed_at = null,
                               updated_at = now()
                        """)
                .param("userId", userId)
                .param("secret", secret)
                .update();

        return new MfaEnrollment(secret, totpService.otpAuthUri(properties.issuer(), accountEmail, secret));
    }

    /**
     * Confirms a pending enrollment by validating a code against the stored
     * secret, enables MFA, and issues a fresh set of recovery codes (returned
     * in plaintext exactly once).
     */
    @Transactional
    public List<String> confirmEnrollment(UUID userId, String submittedCode) {
        String secret = jdbcClient.sql("select secret from user_mfa_totp where user_id = :userId and status = 'PENDING'")
                .param("userId", userId)
                .query(String.class)
                .optional()
                .orElseThrow(() -> new MfaOperationException("No pending enrollment to confirm. Start enrollment first."));

        if (!totpService.verify(secret, submittedCode, properties.verificationStepTolerance())) {
            throw new MfaOperationException("The verification code is incorrect. Try again.");
        }

        jdbcClient.sql("update user_mfa_totp set status = 'ENABLED', confirmed_at = now(), updated_at = now() where user_id = :userId")
                .param("userId", userId)
                .update();

        return regenerateRecoveryCodes(userId);
    }

    @Transactional
    public MfaStatus disable(UUID userId) {
        jdbcClient.sql("delete from user_mfa_recovery_codes where user_id = :userId")
                .param("userId", userId)
                .update();
        jdbcClient.sql("delete from user_mfa_totp where user_id = :userId")
                .param("userId", userId)
                .update();
        LOGGER.info("Disabled multi-factor authentication for user {}.", userId);
        return MfaStatus.disabled();
    }

    /**
     * Verifies a login-time challenge: a current TOTP code, or failing that a
     * single-use recovery code (which is consumed on success).
     */
    @Transactional
    public boolean verifyChallenge(UUID userId, String submittedCode) {
        String secret = jdbcClient.sql("select secret from user_mfa_totp where user_id = :userId and status = 'ENABLED'")
                .param("userId", userId)
                .query(String.class)
                .optional()
                .orElse(null);
        if (secret == null) {
            return false;
        }

        if (totpService.verify(secret, submittedCode, properties.verificationStepTolerance())) {
            return true;
        }
        return consumeRecoveryCode(userId, submittedCode);
    }

    private boolean consumeRecoveryCode(UUID userId, String submittedCode) {
        if (!StringUtils.hasText(submittedCode)) {
            return false;
        }
        String codeHash = MfaSecretHasher.sha256(RecoveryCodeGenerator.normalize(submittedCode));
        int consumed = jdbcClient.sql("""
                        update user_mfa_recovery_codes
                           set consumed_at = now()
                         where user_id = :userId
                           and code_hash = :codeHash
                           and consumed_at is null
                        """)
                .param("userId", userId)
                .param("codeHash", codeHash)
                .update();
        if (consumed > 0) {
            LOGGER.info("Consumed a recovery code for user {} during MFA verification.", userId);
            return true;
        }
        return false;
    }

    private List<String> regenerateRecoveryCodes(UUID userId) {
        jdbcClient.sql("delete from user_mfa_recovery_codes where user_id = :userId")
                .param("userId", userId)
                .update();

        List<String> codes = recoveryCodeGenerator.generate(properties.recoveryCodeCount());
        for (String code : codes) {
            jdbcClient.sql("""
                            insert into user_mfa_recovery_codes (id, user_id, code_hash, code_hash_algorithm, created_at)
                            values (:id, :userId, :codeHash, 'SHA256', now())
                            """)
                    .param("id", UUID.randomUUID())
                    .param("userId", userId)
                    .param("codeHash", MfaSecretHasher.sha256(RecoveryCodeGenerator.normalize(code)))
                    .update();
        }
        return codes;
    }

    private int remainingRecoveryCodes(UUID userId) {
        return jdbcClient.sql("select count(*) from user_mfa_recovery_codes where user_id = :userId and consumed_at is null")
                .param("userId", userId)
                .query(Integer.class)
                .single();
    }
}
