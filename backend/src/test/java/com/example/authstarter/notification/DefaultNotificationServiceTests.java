package com.example.authstarter.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.authstarter.auth.AuthPrincipal;
import com.example.authstarter.foundation.OrganizationContextPayload;
import com.example.authstarter.foundation.CurrentUserContextService;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

class DefaultNotificationServiceTests {

    private static final AuthPrincipal PRINCIPAL = new AuthPrincipal(
            "30000000-0000-4000-8000-000000000001",
            "operator@authstarter.local",
            "Baseline Operator",
            List.of("ORG_ADMIN"));
    private static final OrganizationContextPayload CURRENT_ORGANIZATION = new OrganizationContextPayload(
            "20000000-0000-4000-8000-000000000001",
            "Auth Starter Local",
            "ACTIVE",
            "10000000-0000-4000-8000-000000000001",
            "local-authstarter",
            "ACTIVE",
            "ORG_ADMIN");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-02T10:00:00Z"), ZoneOffset.UTC);

    private CurrentUserContextService currentUserContextService;
    private InMemoryNotificationEventRepository repository;
    private CapturingEmailProvider emailProvider;
    private DefaultNotificationService notificationService;

    @BeforeEach
    void setUp() {
        currentUserContextService = Mockito.mock(CurrentUserContextService.class);
        repository = new InMemoryNotificationEventRepository();
        emailProvider = new CapturingEmailProvider();
        notificationService = new DefaultNotificationService(
                currentUserContextService,
                repository,
                emailProvider,
                CLOCK);

        when(currentUserContextService.findCurrentOrganizationContext(PRINCIPAL))
                .thenReturn(Optional.of(CURRENT_ORGANIZATION));
    }

    @Test
    void sendsUserInvitationNotificationWithPasswordSetupLink() {
        UUID userId = UUID.fromString("30000000-0000-4000-8000-000000000099");

        notificationService.sendUserInvitation(
                PRINCIPAL,
                CURRENT_ORGANIZATION,
                userId,
                "Invited User",
                "invited@authstarter.local",
                "http://localhost:4200/accept-invite?token=raw-token");

        NotificationEventPayload event = repository.events.getFirst();
        assertThat(event.sourceType()).isEqualTo("USER");
        assertThat(event.sourceId()).isEqualTo(userId.toString());
        assertThat(event.eventType()).isEqualTo("USER_INVITED");
        assertThat(event.recipientType()).isEqualTo("USER");
        assertThat(event.recipientId()).isEqualTo(userId.toString());
        assertThat(event.recipientEmail()).isEqualTo("invited@authstarter.local");
        assertThat(event.deliveryStatus()).isEqualTo("SENT");
        assertThat(emailProvider.messages.getFirst().body())
                .contains("http://localhost:4200/accept-invite?token=raw-token");
    }

    @Test
    void sendsPasswordResetNotificationWithResetLink() {
        UUID userId = UUID.fromString("30000000-0000-4000-8000-000000000099");

        notificationService.sendPasswordReset(
                CURRENT_ORGANIZATION,
                userId,
                "Reset User",
                "reset@authstarter.local",
                "http://localhost:4200/reset-password?token=raw-token");

        NotificationEventPayload event = repository.events.getFirst();
        assertThat(event.eventType()).isEqualTo("PASSWORD_RESET_REQUESTED");
        assertThat(event.recipientEmail()).isEqualTo("reset@authstarter.local");
        assertThat(emailProvider.messages.getFirst().subject())
                .contains("Reset your Spring Boot Angular Auth Starter password");
    }

    @Test
    void exposesNotificationHistoryForActiveCurrentOrganization() {
        UUID userId = UUID.fromString("30000000-0000-4000-8000-000000000099");
        notificationService.sendUserInvitation(
                PRINCIPAL,
                CURRENT_ORGANIZATION,
                userId,
                "Invited User",
                "invited@authstarter.local",
                "http://localhost:4200/accept-invite?token=raw-token");

        List<NotificationEventPayload> events = notificationService.listNotificationEvents(PRINCIPAL);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().organizationId()).isEqualTo(CURRENT_ORGANIZATION.organizationId());
    }

    @Test
    void rejectsNotificationHistoryWithoutActiveOrganizationContext() {
        AuthPrincipal principal = new AuthPrincipal("missing", "missing@example.com", "Missing", List.of("USER"));
        when(currentUserContextService.findCurrentOrganizationContext(principal)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.listNotificationEvents(principal))
                .isInstanceOf(AccessDeniedException.class);
    }

    private static final class CapturingEmailProvider implements NotificationEmailProvider {
        private final List<NotificationEmailMessage> messages = new ArrayList<>();

        @Override
        public String providerCode() {
            return "TEST_EMAIL";
        }

        @Override
        public NotificationDeliveryResult send(NotificationEmailMessage message) {
            messages.add(message);
            return NotificationDeliveryResult.sent("TEST-" + message.notificationEventId());
        }
    }

    private static final class InMemoryNotificationEventRepository implements NotificationEventRepository {
        private final List<NotificationEventPayload> events = new ArrayList<>();

        @Override
        public void savePending(NotificationEventInsert event, OffsetDateTime createdAt) {
            events.add(new NotificationEventPayload(
                    event.id().toString(),
                    event.organizationId().toString(),
                    event.workspaceId().toString(),
                    event.sourceType(),
                    event.sourceId().toString(),
                    event.eventType(),
                    event.recipientType(),
                    event.recipientId().toString(),
                    event.recipientDisplayName(),
                    event.recipientEmail(),
                    "EMAIL",
                    event.provider(),
                    "PENDING",
                    null,
                    null,
                    createdAt.toString(),
                    null,
                    createdAt.toString()));
        }

        @Override
        public void markDelivered(UUID eventId, NotificationDeliveryResult result, OffsetDateTime updatedAt) {
            for (int index = 0; index < events.size(); index++) {
                NotificationEventPayload event = events.get(index);
                if (event.id().equals(eventId.toString())) {
                    events.set(index, new NotificationEventPayload(
                            event.id(),
                            event.organizationId(),
                            event.workspaceId(),
                            event.sourceType(),
                            event.sourceId(),
                            event.eventType(),
                            event.recipientType(),
                            event.recipientId(),
                            event.recipientDisplayName(),
                            event.recipientEmail(),
                            event.channel(),
                            event.provider(),
                            result.status(),
                            result.providerMessageId(),
                            result.failureReason(),
                            event.createdAt(),
                            "SENT".equals(result.status()) ? updatedAt.toString() : null,
                            updatedAt.toString()));
                }
            }
        }

        @Override
        public List<NotificationEventPayload> listEvents(OrganizationContextPayload organization) {
            return events.stream()
                    .filter(event -> event.organizationId().equals(organization.organizationId()))
                    .filter(event -> event.workspaceId().equals(organization.workspaceId()))
                    .toList();
        }
    }
}
