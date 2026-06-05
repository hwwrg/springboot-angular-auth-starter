package com.example.authstarter.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

class NotificationEmailProviderConfigurationTests {

    private final NotificationEmailProviderConfiguration configuration = new NotificationEmailProviderConfiguration();

    @Test
    void selectsLocalMockProviderByDefault() {
        NotificationEmailProperties properties = new NotificationEmailProperties();

        NotificationEmailProvider provider = configuration.notificationEmailProvider(properties);

        assertThat(provider).isInstanceOf(LocalMockEmailNotificationProvider.class);
        assertThat(provider.providerCode()).isEqualTo("LOCAL_MOCK_EMAIL");
    }

    @Test
    void rejectsUnsupportedProviderValues() {
        NotificationEmailProperties properties = new NotificationEmailProperties();
        properties.setProvider("ses");

        assertThatThrownBy(() -> configuration.notificationEmailProvider(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Supported values: local-mock, smtp");
    }

    @Test
    void selectsSmtpProvider() {
        NotificationEmailProperties properties = new NotificationEmailProperties();
        properties.setProvider("smtp");

        NotificationEmailProvider provider = configuration.notificationEmailProvider(properties);

        assertThat(provider).isInstanceOf(SmtpEmailNotificationProvider.class);
        assertThat(provider.providerCode()).isEqualTo("SMTP");
    }

    @Test
    void bindsLocalNotificationEmailProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("authstarter.notification.email.provider", "local-mock")
                .withProperty("authstarter.notification.email.from-email", "no-reply@authstarter.local")
                .withProperty("authstarter.notification.email.from-name", "Auth Starter")
                .withProperty("authstarter.notification.email.reply-to-email", "support@authstarter.local")
                .withProperty("authstarter.notification.email.smtp.host", "localhost")
                .withProperty("authstarter.notification.email.smtp.port", "1025")
                .withProperty("authstarter.notification.email.smtp.username", "demo")
                .withProperty("authstarter.notification.email.smtp.password", "demo-password")
                .withProperty("authstarter.notification.email.smtp.protocol", "smtp")
                .withProperty("authstarter.notification.email.smtp.auth", "true")
                .withProperty("authstarter.notification.email.smtp.start-tls", "true");

        NotificationEmailProperties properties = Binder.get(environment)
                .bind("authstarter.notification.email", Bindable.of(NotificationEmailProperties.class))
                .orElseThrow(() -> new AssertionError("Notification email properties should bind"));

        assertThat(properties.getProvider()).isEqualTo("local-mock");
        assertThat(properties.getFromEmail()).isEqualTo("no-reply@authstarter.local");
        assertThat(properties.getFromName()).isEqualTo("Auth Starter");
        assertThat(properties.getReplyToEmail()).isEqualTo("support@authstarter.local");
        assertThat(properties.getSmtp().getHost()).isEqualTo("localhost");
        assertThat(properties.getSmtp().getPort()).isEqualTo(1025);
        assertThat(properties.getSmtp().getUsername()).isEqualTo("demo");
        assertThat(properties.getSmtp().getPassword()).isEqualTo("demo-password");
        assertThat(properties.getSmtp().getProtocol()).isEqualTo("smtp");
        assertThat(properties.getSmtp().isAuth()).isTrue();
        assertThat(properties.getSmtp().isStartTls()).isTrue();
    }
}
