package com.example.authstarter.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class LocalMockEmailNotificationProviderTests {

    private final LocalMockEmailNotificationProvider provider = new LocalMockEmailNotificationProvider();

    @Test
    void returnsSentResultWithoutCallingExternalProvider() {
        NotificationDeliveryResult result = provider.send(new NotificationEmailMessage(
                UUID.fromString("95000000-0000-4000-8000-000000000001"),
                "USER_INVITED",
                "Invited User",
                "invited@authstarter.local",
                "Set up your account",
                "Use this setup link."));

        assertThat(provider.providerCode()).isEqualTo("LOCAL_MOCK_EMAIL");
        assertThat(result.status()).isEqualTo("SENT");
        assertThat(result.providerMessageId()).isEqualTo("LOCAL-NOTIF-95000000-000");
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void failsExplicitlyWhenEmailRecipientCannotBeResolved() {
        NotificationDeliveryResult result = provider.send(new NotificationEmailMessage(
                UUID.fromString("95000000-0000-4000-8000-000000000002"),
                "USER_INVITED",
                "Invited User",
                null,
                "Set up your account",
                "Use this setup link."));

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.providerMessageId()).isNull();
        assertThat(result.failureReason()).contains("Recipient email is required");
    }
}
