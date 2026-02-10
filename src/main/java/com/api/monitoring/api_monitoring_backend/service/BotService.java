package com.api.monitoring.api_monitoring_backend.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import com.api.monitoring.api_monitoring_backend.config.AppProperties;
import com.api.monitoring.api_monitoring_backend.model.Api;
import com.api.monitoring.api_monitoring_backend.model.BotUser;
import com.api.monitoring.api_monitoring_backend.repository.BotUserRepository;
import com.api.monitoring.api_monitoring_backend.util.HttpClientUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BotService {
    private final BotUserRepository botUserRepository;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClientUtil httpClientUtil;

    public BotService(
        BotUserRepository botUserRepository,
        AppProperties appProperties,
        ObjectMapper objectMapper,
        HttpClientUtil httpClientUtil
    ) {
        this.botUserRepository = botUserRepository;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.httpClientUtil = httpClientUtil;
    }

    public void sendMessage(String link, String name, Integer code, Map<String, Object> errorMessage, Boolean recovering) {
        List<BotUser> users = botUserRepository.findAll();
        String message = buildTelegramMessage(link, name, code, errorMessage, recovering);
        System.out.println("Sending Telegram message: " + message);
        System.out.println("Users: " + users);
        for (BotUser user : users) {
            sendTelegramMessage(user.getUserId(), message);
        }
    }

    public void sendMessageSlack(String link, String name, Integer code, Map<String, Object> errorMessage, Boolean recovering) {
        String message = buildSlackMessage(link, name, code, errorMessage, recovering);
        sendSlackMessage(message);
    }

    public void sendActiveMessage() {
        sendSlackMessage("*API monitor is Active ✅*");
        List<BotUser> users = botUserRepository.findAll();
        for (BotUser user : users) {
            sendTelegramMessage(user.getUserId(), "***API monitor is Active ✅***");
        }
    }

    public void sendCdnStatus(String apiName, Map<String, Object> result) {
        String message;
        if ("image".equals(result.get("type"))) {
            Map<String, Object> count = castMap(result.get("count"));
            message = String.format("*INFO!*\n*API*: %s\n*CURRENT_USAGE:* %s\n*ALLOWED_USAGE:* %s\n*PERCENTAGE:* %s\n",
                apiName, count.get("current"), count.get("allowed"), result.get("percentage"));
        } else {
            Map<String, Object> response = castMap(result.get("response"));
            message = String.format("*INFO!*\n*API*: %s\n*VIDEO_COUNT:* %s\n*TOTAL_STORAGE_MINUTES:* %s\n*TOTAL_STORAGE_MINUTES_LIMIT:* %s\n*PERCENTAGE:* %s\n",
                apiName, response.get("videoCount"), response.get("totalStorageMinutes"),
                response.get("totalStorageMinutesLimit"), result.get("percentage"));
        }
        sendSlackMessage(message);

        List<BotUser> users = botUserRepository.findAll();
        String telegram = message.replace("*", "***");
        for (BotUser user : users) {
            sendTelegramMessage(user.getUserId(), telegram);
        }
    }

    public void sendTokenBalanceMessage(Api api, int code, BigInteger tokenBalance, BigInteger minimumRequired) {
        String title;
        String emoji;
        String severity;
        if (code == 1) {
            emoji = "⚠️";
            title = "LOW BALANCE ALERT";
            severity = "Low Balance";
        } else if (code == 2) {
            emoji = "⚠️";
            title = "LOW TOKEN BALANCE ALERT";
            severity = "Low Balance";
        } else if (code == 3) {
            emoji = "🟠";
            title = "WARNING: TOKEN BALANCE LOW";
            severity = "Low Balance";
        } else if (code == 4) {
            emoji = "✅";
            title = "TOKEN BALANCE RECOVERED";
            severity = "OK";
        } else {
            emoji = "❌";
            title = "API INACTIVE ALERT";
            severity = "Inactive";
        }

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(4);
        nf.setMaximumFractionDigits(4);
        String balance = nf.format(toEther(tokenBalance));
        String minRequired = minimumRequired == null ? "" : nf.format(toEther(minimumRequired));

        String message = String.format(
            "%s *%s* %s\n\n*API Name:* %s\n*Chain ID:* %s\n*Contract Address:* `%s`\n*Wallet Address:* `%s`\n\n*Token Balance:* %s\n%s\n*Status:* %s\n*Checked At:* %s\n\n🔗 [View on BaseScan](https://basescan.org/token/%s?a=%s)",
            emoji, title, emoji,
            api.getApiName(),
            api.getChainId(),
            api.getContractAddress(),
            api.getTokenAddress(),
            balance,
            minimumRequired == null ? "" : "*Minimum Required:* " + minRequired + "\n",
            severity,
            ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toString(),
            api.getContractAddress(),
            api.getTokenAddress()
        );

        List<BotUser> users = botUserRepository.findAll();
        for (BotUser user : users) {
            sendTelegramMessage(user.getUserId(), message);
        }
    }

    private void sendTelegramMessage(Long chatId, String message) {
        if (appProperties.getTelegram().getAccessToken() == null
            || appProperties.getTelegram().getAccessToken().isBlank()) {
            return;
        }
        try {
            String url = "https://api.telegram.org/bot" + appProperties.getTelegram().getAccessToken() + "/sendMessage";
            Map<String, Object> payload = Map.of(
                "chat_id", chatId,
                "text", message,
                "parse_mode", "Markdown"
            );
            httpClientUtil.postJsonWithRetries(url, objectMapper.writeValueAsString(payload), 1);
        } catch (Exception ignored) {
        }
    }

    private BigDecimal toEther(BigInteger value) {
        return new BigDecimal(value)
            .divide(new BigDecimal("1000000000000000000"), 4, RoundingMode.HALF_UP);
    }

    private void sendSlackMessage(String message) {
        if (appProperties.getSlack().getWebhookUrl() == null
            || appProperties.getSlack().getWebhookUrl().isBlank()) {
            return;
        }
        try {
            Map<String, Object> payload = Map.of(
                "text", message,
                "mrkdwn", true
            );
            httpClientUtil.postJsonWithRetries(appProperties.getSlack().getWebhookUrl(),
                objectMapper.writeValueAsString(payload), 1);
        } catch (Exception ignored) {
        }
    }

    private String buildTelegramMessage(String link, String name, Integer code,
        Map<String, Object> errorMessage, Boolean recovering) {
        String cleanLink = link.replace("/status", "").replace("/health", "");
        if (code == null) {
            return String.format("***ALERT!\nAPI***: %s\n***ROUTE:*** %s \n***STATUS: INACTIVE***\n", name, cleanLink);
        }
        if (code == 12) {
            return String.format("***ALERT!\nAPI***: %s\n***ROUTE:*** %s \n***STATUS: RPC DOWN***\n", name, cleanLink);
        }
        if (code == 7) {
            if (Boolean.TRUE.equals(recovering)) {
                return String.format(
                    "***ALERT!\nAPI***: %s\n***ROUTE:*** %s \n***STATUS: RECOVERING*** \n***BLOCKS BEHIND:*** %s\n***UPDATED AT:*** %s",
                    name, cleanLink, errorMessage.get("blocksBehind"), errorMessage.get("updatedAt"));
            }
            return String.format(
                "***ALERT!\nAPI***: %s\n***ROUTE:*** %s \n***STATUS: OUT OF SYNC*** \n***BLOCKS BEHIND:*** %s\n***UPDATED AT:*** %s",
                name, cleanLink, errorMessage.get("blocksBehind"), errorMessage.get("updatedAt"));
        }
        if (code == 1) {
            return String.format("***INFO!\nAPI***: %s\n***ROUTE:*** %s \n***STATUS: INDEXER IS NOW SYNCED***\n", name, cleanLink);
        }
        if (code == 2) {
            return String.format("***INFO!\nAPI***: %s\n***ROUTE:*** %s \n***STATUS: API IS ACTIVE***\n", name, cleanLink);
        }
        if (code == 13) {
            if ("image".equals(errorMessage.get("type"))) {
                Map<String, Object> count = castMap(errorMessage.get("count"));
                return String.format(
                    "***ALERT!\nAPI***: %s\n***CURRENT_USAGE:*** %s\n***ALLOWED_USAGE:*** %s\n***PERCENTAGE:*** %s\n",
                    name, count.get("current"), count.get("allowed"), errorMessage.get("percentage"));
            }
            Map<String, Object> response = castMap(errorMessage.get("response"));
            return String.format(
                "***ALERT!\nAPI***: %s\n***TOTAL_STORAGE_MINUTES:*** %s\n***TOTAL_STORAGE_MINUTES_LIMIT:*** %s\n***PERCENTAGE:*** %s\n",
                name, response.get("totalStorageMinutes"), response.get("totalStorageMinutesLimit"),
                errorMessage.get("percentage"));
        }
        if (code == 3) {
            if ("image".equals(errorMessage.get("type"))) {
                Map<String, Object> count = castMap(errorMessage.get("count"));
                return String.format(
                    "***INFO!\nAPI***: %s\n***CURRENT_USAGE:*** %s\n***ALLOWED_USAGE:*** %s\n***PERCENTAGE:*** %s\n***LIMIT INCREASED! USAGE WITHIN RANGE***",
                    name, count.get("current"), count.get("allowed"), errorMessage.get("percentage"));
            }
            Map<String, Object> response = castMap(errorMessage.get("response"));
            return String.format(
                "***INFO!\nAPI***: %s\n***VIDEO_COUNT:*** %s\n***TOTAL_STORAGE_MINUTES:*** %s\n***TOTAL_STORAGE_MINUTES_LIMIT:*** %s\n***PERCENTAGE:*** %s\n***LIMIT INCREASED! USAGE WITHIN RANGE***",
                name, response.get("videoCount"), response.get("totalStorageMinutes"),
                response.get("totalStorageMinutesLimit"), errorMessage.get("percentage"));
        }
        return String.format("***ALERT!\nAPI***: %s\n***ROUTE:*** %s \n***STATUS: INACTIVE***\n", name, cleanLink);
    }

    private String buildSlackMessage(String link, String name, Integer code,
        Map<String, Object> errorMessage, Boolean recovering) {
        String cleanLink = link.replace("/status", "").replace("/health", "");
        if (code == null) {
            return String.format("*ALERT!*\n*API*: %s\n*ROUTE:* %s \n*STATUS:* INACTIVE\n", name, cleanLink);
        }
        if (code == 12) {
            return String.format("*ALERT!*\n*API*: %s\n*ROUTE:* %s \n*STATUS:* RPC DOWN\n", name, cleanLink);
        }
        if (code == 7) {
            if (Boolean.TRUE.equals(recovering)) {
                return String.format(
                    "*ALERT!*\n*API*: %s\n*ROUTE:* %s \n*STATUS:* RECOVERING \n*BLOCKS BEHIND:* %s\n*UPDATED AT:* %s",
                    name, cleanLink, errorMessage.get("blocksBehind"), errorMessage.get("updatedAt"));
            }
            return String.format(
                "*ALERT!*\n*API*: %s\n*ROUTE:* %s \n*STATUS:* OUT OF SYNC \n*BLOCKS BEHIND:* %s\n*UPDATED AT:* %s",
                name, cleanLink, errorMessage.get("blocksBehind"), errorMessage.get("updatedAt"));
        }
        if (code == 1) {
            return String.format("*INFO!*\n*API*: %s\n*ROUTE:* %s \n*STATUS:* INDEXER IS NOW SYNCED\n", name, cleanLink);
        }
        if (code == 2) {
            return String.format("*INFO!*\n*API*: %s\n*ROUTE:* %s \n*STATUS:* API IS ACTIVE\n", name, cleanLink);
        }
        if (code == 13) {
            if ("image".equals(errorMessage.get("type"))) {
                Map<String, Object> count = castMap(errorMessage.get("count"));
                return String.format(
                    "*ALERT!*\n*API*: %s\n*CURRENT_USAGE:* %s\n*ALLOWED_USAGE:* %s\n*PERCENTAGE:* %s\n",
                    name, count.get("current"), count.get("allowed"), errorMessage.get("percentage"));
            }
            Map<String, Object> response = castMap(errorMessage.get("response"));
            return String.format(
                "*ALERT!*\n*API*: %s\n*TOTAL_STORAGE_MINUTES:* %s\n*TOTAL_STORAGE_MINUTES_LIMIT:* %s\n*PERCENTAGE:* %s\n",
                name, response.get("totalStorageMinutes"), response.get("totalStorageMinutesLimit"),
                errorMessage.get("percentage"));
        }
        if (code == 3) {
            if ("image".equals(errorMessage.get("type"))) {
                Map<String, Object> count = castMap(errorMessage.get("count"));
                return String.format(
                    "*INFO!*\n*API*: %s\n*CURRENT_USAGE:* %s\n*ALLOWED_USAGE:* %s\n*PERCENTAGE:* %s\n*LIMIT INCREASED! USAGE WITHIN RANGE*\n",
                    name, count.get("current"), count.get("allowed"), errorMessage.get("percentage"));
            }
            Map<String, Object> response = castMap(errorMessage.get("response"));
            return String.format(
                "*INFO!*\n*API*: %s\n*VIDEO_COUNT:* %s\n*TOTAL_STORAGE_MINUTES:* %s\n*TOTAL_STORAGE_MINUTES_LIMIT:* %s\n*PERCENTAGE:* %s\n*LIMIT INCREASED! USAGE WITHIN RANGE*\n",
                name, response.get("videoCount"), response.get("totalStorageMinutes"),
                response.get("totalStorageMinutesLimit"), errorMessage.get("percentage"));
        }
        return String.format("*ALERT!*\n*API*: %s\n*ROUTE:* %s \n*STATUS:* INACTIVE\n", name, cleanLink);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        return (Map<String, Object>) obj;
    }
}
