package com.example.authstarter.notification;

import java.util.UUID;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.StringUtils;

interface NotificationEmailProvider {

    String providerCode();

    NotificationDeliveryResult send(NotificationEmailMessage message);
}

record NotificationEmailMessage(
        UUID notificationEventId,
        String eventType,
        String recipientDisplayName,
        String recipientEmail,
        String subject,
        String body) {
}

record NotificationDeliveryResult(
        String status,
        String providerMessageId,
        String failureReason) {

    private static final int MAX_FAILURE_REASON_LENGTH = 500;

    static NotificationDeliveryResult sent(String providerMessageId) {
        return new NotificationDeliveryResult("SENT", providerMessageId, null);
    }

    static NotificationDeliveryResult failed(String failureReason) {
        return new NotificationDeliveryResult("FAILED", null, truncateFailureReason(failureReason));
    }

    private static String truncateFailureReason(String failureReason) {
        if (!StringUtils.hasText(failureReason)) {
            return "Email provider did not return a failure reason.";
        }
        String normalized = failureReason.trim();
        if (normalized.length() <= MAX_FAILURE_REASON_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_FAILURE_REASON_LENGTH - 3) + "...";
    }
}

class LocalMockEmailNotificationProvider implements NotificationEmailProvider {

    static final String PROVIDER_CODE = "LOCAL_MOCK_EMAIL";

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public NotificationDeliveryResult send(NotificationEmailMessage message) {
        if (!StringUtils.hasText(message.recipientEmail())) {
            return NotificationDeliveryResult.failed("Recipient email is required for baseline email delivery.");
        }
        return NotificationDeliveryResult.sent(
                "LOCAL-NOTIF-" + message.notificationEventId().toString().substring(0, 12).toUpperCase());
    }
}

class SmtpEmailNotificationProvider implements NotificationEmailProvider {

    static final String PROVIDER_CODE = "SMTP";

    private final JavaMailSender mailSender;
    private final NotificationEmailProperties properties;

    SmtpEmailNotificationProvider(JavaMailSender mailSender, NotificationEmailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public NotificationDeliveryResult send(NotificationEmailMessage message) {
        if (!StringUtils.hasText(message.recipientEmail())) {
            return NotificationDeliveryResult.failed("Recipient email is required for SMTP email delivery.");
        }
        if (!StringUtils.hasText(properties.getFromEmail())) {
            return NotificationDeliveryResult.failed("From email is required for SMTP email delivery.");
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(formatSender());
        mail.setTo(message.recipientEmail().trim());
        if (StringUtils.hasText(properties.getReplyToEmail())) {
            mail.setReplyTo(properties.getReplyToEmail().trim());
        }
        mail.setSubject(message.subject());
        mail.setText(message.body());

        try {
            mailSender.send(mail);
            return NotificationDeliveryResult.sent("SMTP-" + message.notificationEventId());
        } catch (MailException ex) {
            return NotificationDeliveryResult.failed("SMTP delivery failed: " + ex.getClass().getSimpleName());
        }
    }

    private String formatSender() {
        String fromEmail = properties.getFromEmail().trim();
        if (!StringUtils.hasText(properties.getFromName())) {
            return fromEmail;
        }
        return properties.getFromName().trim() + " <" + fromEmail + ">";
    }
}
