package com.api.monitoring.api_monitoring_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String baseUrl;
    private Jwt jwt = new Jwt();
    private Telegram telegram = new Telegram();
    private Slack slack = new Slack();
    private Cloudflare cloudflare = new Cloudflare();
    private int triggerBlock = 50;
    private String infuraKey;
    private SeedAdmin seedAdmin = new SeedAdmin();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Telegram getTelegram() {
        return telegram;
    }

    public void setTelegram(Telegram telegram) {
        this.telegram = telegram;
    }

    public Slack getSlack() {
        return slack;
    }

    public void setSlack(Slack slack) {
        this.slack = slack;
    }

    public Cloudflare getCloudflare() {
        return cloudflare;
    }

    public void setCloudflare(Cloudflare cloudflare) {
        this.cloudflare = cloudflare;
    }

    public int getTriggerBlock() {
        return triggerBlock;
    }

    public void setTriggerBlock(int triggerBlock) {
        this.triggerBlock = triggerBlock;
    }

    public String getInfuraKey() {
        return infuraKey;
    }

    public void setInfuraKey(String infuraKey) {
        this.infuraKey = infuraKey;
    }

    public SeedAdmin getSeedAdmin() {
        return seedAdmin;
    }

    public void setSeedAdmin(SeedAdmin seedAdmin) {
        this.seedAdmin = seedAdmin;
    }

    public static class Jwt {
        private String secret = "secret";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    public static class Telegram {
        private String accessToken;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }

    public static class Slack {
        private String webhookUrl;

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }
    }

    public static class Cloudflare {
        private String token;
        private double imagePercentLimit = 0.9;
        private double videoPercentLimit = 0.9;
        private long notifiedTimeLimitMs = 3600000;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public double getImagePercentLimit() {
            return imagePercentLimit;
        }

        public void setImagePercentLimit(double imagePercentLimit) {
            this.imagePercentLimit = imagePercentLimit;
        }

        public double getVideoPercentLimit() {
            return videoPercentLimit;
        }

        public void setVideoPercentLimit(double videoPercentLimit) {
            this.videoPercentLimit = videoPercentLimit;
        }

        public long getNotifiedTimeLimitMs() {
            return notifiedTimeLimitMs;
        }

        public void setNotifiedTimeLimitMs(long notifiedTimeLimitMs) {
            this.notifiedTimeLimitMs = notifiedTimeLimitMs;
        }
    }

    public static class SeedAdmin {
        private boolean enabled;
        private String email;
        private String password;
        private String name = "Admin";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
