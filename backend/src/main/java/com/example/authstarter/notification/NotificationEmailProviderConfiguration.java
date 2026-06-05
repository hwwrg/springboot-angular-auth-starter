package com.example.authstarter.notification;

import java.util.Locale;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(NotificationEmailProperties.class)
class NotificationEmailProviderConfiguration {

    @Bean
    NotificationEmailProvider notificationEmailProvider(NotificationEmailProperties properties) {
        String provider = normalizeProvider(properties.getProvider());
        if ("local-mock".equals(provider)) {
            return new LocalMockEmailNotificationProvider();
        }
        if ("smtp".equals(provider)) {
            return new SmtpEmailNotificationProvider(mailSender(properties), properties);
        }
        throw new IllegalStateException(
                "Unsupported notification email provider '" + properties.getProvider()
                        + "'. Supported values: local-mock, smtp.");
    }

    private JavaMailSenderImpl mailSender(NotificationEmailProperties properties) {
        NotificationEmailProperties.Smtp smtp = properties.getSmtp();
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtp.getHost());
        mailSender.setPort(smtp.getPort());
        mailSender.setProtocol(smtp.getProtocol());
        mailSender.setUsername(smtp.getUsername());
        mailSender.setPassword(smtp.getPassword());
        mailSender.getJavaMailProperties().put("mail.smtp.auth", Boolean.toString(smtp.isAuth()));
        mailSender.getJavaMailProperties().put("mail.smtp.starttls.enable", Boolean.toString(smtp.isStartTls()));
        return mailSender;
    }

    private String normalizeProvider(String provider) {
        if (!StringUtils.hasText(provider)) {
            return "local-mock";
        }
        return provider.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
