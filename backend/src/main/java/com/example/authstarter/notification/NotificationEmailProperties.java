package com.example.authstarter.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "authstarter.notification.email")
public class NotificationEmailProperties {

    private String provider = "local-mock";
    private String fromEmail = "no-reply@authstarter.local";
    private String fromName = "Spring Boot Angular Auth Starter";
    private String replyToEmail;
    private Smtp smtp = new Smtp();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getReplyToEmail() {
        return replyToEmail;
    }

    public void setReplyToEmail(String replyToEmail) {
        this.replyToEmail = replyToEmail;
    }

    public Smtp getSmtp() {
        return smtp;
    }

    public void setSmtp(Smtp smtp) {
        this.smtp = smtp;
    }

    public static class Smtp {

        private String host = "localhost";
        private int port = 1025;
        private String username;
        private String password;
        private String protocol = "smtp";
        private boolean auth;
        private boolean startTls;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public boolean isAuth() {
            return auth;
        }

        public void setAuth(boolean auth) {
            this.auth = auth;
        }

        public boolean isStartTls() {
            return startTls;
        }

        public void setStartTls(boolean startTls) {
            this.startTls = startTls;
        }
    }
}
