package com.example.authstarter.notification;

import com.example.authstarter.auth.AuthPrincipal;
import com.example.authstarter.foundation.OrganizationContextPayload;
import com.example.authstarter.foundation.CurrentUserContextService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

public interface NotificationService {

    void sendUserInvitation(
            AuthPrincipal requestedBy,
            OrganizationContextPayload organization,
            UUID userId,
            String recipientDisplayName,
            String recipientEmail,
            String setupUrl);

    void sendPasswordReset(
            OrganizationContextPayload organization,
            UUID userId,
            String recipientDisplayName,
            String recipientEmail,
            String resetUrl);

    List<NotificationEventPayload> listNotificationEvents(AuthPrincipal principal);
}

@Service
@ConditionalOnExpression("environment.getProperty('spring.autoconfigure.exclude', '').contains('DataSourceAutoConfiguration')")
class NoopNotificationService implements NotificationService {

    @Override
    public void sendUserInvitation(
            AuthPrincipal requestedBy,
            OrganizationContextPayload organization,
            UUID userId,
            String recipientDisplayName,
            String recipientEmail,
            String setupUrl) {
    }

    @Override
    public void sendPasswordReset(
            OrganizationContextPayload organization,
            UUID userId,
            String recipientDisplayName,
            String recipientEmail,
            String resetUrl) {
    }

    @Override
    public List<NotificationEventPayload> listNotificationEvents(AuthPrincipal principal) {
        return List.of();
    }
}

@Service
@ConditionalOnExpression("!environment.getProperty('spring.autoconfigure.exclude', '').contains('DataSourceAutoConfiguration')")
class DefaultNotificationService implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultNotificationService.class);
    private static final String USER_INVITED_EVENT = "USER_INVITED";
    private static final String PASSWORD_RESET_REQUESTED_EVENT = "PASSWORD_RESET_REQUESTED";

    private final CurrentUserContextService currentUserContextService;
    private final NotificationEventRepository notificationEventRepository;
    private final NotificationEmailProvider notificationEmailProvider;
    private final Clock clock;

    @Autowired
    DefaultNotificationService(
            CurrentUserContextService currentUserContextService,
            NotificationEventRepository notificationEventRepository,
            NotificationEmailProvider notificationEmailProvider) {
        this(currentUserContextService, notificationEventRepository, notificationEmailProvider, Clock.systemUTC());
    }

    DefaultNotificationService(
            CurrentUserContextService currentUserContextService,
            NotificationEventRepository notificationEventRepository,
            NotificationEmailProvider notificationEmailProvider,
            Clock clock) {
        this.currentUserContextService = currentUserContextService;
        this.notificationEventRepository = notificationEventRepository;
        this.notificationEmailProvider = notificationEmailProvider;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void sendUserInvitation(
            AuthPrincipal requestedBy,
            OrganizationContextPayload organization,
            UUID userId,
            String recipientDisplayName,
            String recipientEmail,
            String setupUrl) {
        if (organization == null) {
            throw new IllegalArgumentException("Organization context is required for user invitation notification.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User id is required for user invitation notification.");
        }
        String subject = "Set up your Spring Boot Angular Auth Starter account";
        String body = """
                Hello %s,

                Your Spring Boot Angular Auth Starter account has been created. Set your password using this secure setup link:

                %s

                If you were not expecting this invitation, contact your administrator.
                """.formatted(
                StringUtils.hasText(recipientDisplayName) ? recipientDisplayName.trim() : "there",
                setupUrl);
        saveAndDeliver(organization, userId, USER_INVITED_EVENT, recipientDisplayName, recipientEmail, subject, body);
    }

    @Override
    @Transactional
    public void sendPasswordReset(
            OrganizationContextPayload organization,
            UUID userId,
            String recipientDisplayName,
            String recipientEmail,
            String resetUrl) {
        if (organization == null) {
            throw new IllegalArgumentException("Organization context is required for password reset notification.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User id is required for password reset notification.");
        }
        String subject = "Reset your Spring Boot Angular Auth Starter password";
        String body = """
                Hello %s,

                Use this secure link to reset your Spring Boot Angular Auth Starter password:

                %s

                If you did not request a password reset, you can ignore this email.
                """.formatted(
                StringUtils.hasText(recipientDisplayName) ? recipientDisplayName.trim() : "there",
                resetUrl);
        saveAndDeliver(organization, userId, PASSWORD_RESET_REQUESTED_EVENT, recipientDisplayName, recipientEmail, subject, body);
    }

    @Override
    public List<NotificationEventPayload> listNotificationEvents(AuthPrincipal principal) {
        return notificationEventRepository.listEvents(requireCurrentOrganization(principal));
    }

    private void saveAndDeliver(
            OrganizationContextPayload organization,
            UUID userId,
            String eventType,
            String recipientDisplayName,
            String recipientEmail,
            String subject,
            String body) {
        NotificationEventInsert event = new NotificationEventInsert(
                UUID.randomUUID(),
                UUID.fromString(organization.organizationId()),
                UUID.fromString(organization.workspaceId()),
                "USER",
                userId,
                eventType,
                "USER",
                userId,
                recipientDisplayName,
                recipientEmail,
                notificationEmailProvider.providerCode());

        OffsetDateTime now = OffsetDateTime.now(clock);
        notificationEventRepository.savePending(event, now);
        NotificationDeliveryResult deliveryResult = deliver(event, eventType, subject, body);
        notificationEventRepository.markDelivered(event.id(), deliveryResult, OffsetDateTime.now(clock));
        LOGGER.info(
                "Account notification event {} recorded with status {} for user {}.",
                eventType,
                deliveryResult.status(),
                userId);
        if (!"SENT".equals(deliveryResult.status())) {
            throw new AccountNotificationDeliveryException(
                    "Email delivery failed: " + deliveryResult.failureReason());
        }
    }

    private NotificationDeliveryResult deliver(
            NotificationEventInsert event,
            String eventType,
            String subject,
            String body) {
        try {
            return notificationEmailProvider.send(new NotificationEmailMessage(
                    event.id(),
                    eventType,
                    event.recipientDisplayName(),
                    event.recipientEmail(),
                    subject,
                    body));
        } catch (RuntimeException ex) {
            LOGGER.warn("Notification email provider {} failed for event {}.",
                    notificationEmailProvider.providerCode(),
                    event.id(),
                    ex);
            return NotificationDeliveryResult.failed("Provider failure: " + ex.getClass().getSimpleName());
        }
    }

    private OrganizationContextPayload requireCurrentOrganization(AuthPrincipal principal) {
        return currentUserContextService.findCurrentOrganizationContext(principal)
                .filter(organization -> "ACTIVE".equals(organization.organizationStatus()))
                .filter(organization -> "ACTIVE".equals(organization.workspaceStatus()))
                .orElseThrow(() -> new AccessDeniedException("An active current organization context is required."));
    }
}
