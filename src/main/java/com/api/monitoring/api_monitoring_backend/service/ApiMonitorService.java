package com.api.monitoring.api_monitoring_backend.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.api.monitoring.api_monitoring_backend.config.AppProperties;
import com.api.monitoring.api_monitoring_backend.exception.ApiException;
import com.api.monitoring.api_monitoring_backend.model.Api;
import com.api.monitoring.api_monitoring_backend.repository.ApiRepository;
import com.api.monitoring.api_monitoring_backend.util.ApiConstants;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class ApiMonitorService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy, h:mm:ss a");
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final ApiRepository apiRepository;
    private final ApiClient apiClient;
    private final BotService botService;
    private final AppProperties appProperties;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final ConcurrentHashMap<String, ScheduledFuture<?>> runningCrons = new ConcurrentHashMap<>();
    private static final BigInteger THRESHOLD_500K = new BigInteger("500000000000000000000000");
    private static final BigInteger THRESHOLD_1M = new BigInteger("1000000000000000000000000");
    private static final BigInteger THRESHOLD_2M = new BigInteger("2000000000000000000000000");
    private static final BigInteger RESET_10M = new BigInteger("10000000000000000000000000");
    private static final BigInteger RESET_20M = new BigInteger("20000000000000000000000000");

    public ApiMonitorService(
        ApiRepository apiRepository,
        ApiClient apiClient,
        BotService botService,
        AppProperties appProperties
    ) {
        this.apiRepository = apiRepository;
        this.apiClient = apiClient;
        this.botService = botService;
        this.appProperties = appProperties;
    }

    public void updateApiStatuses() {
        List<Api> apis = apiRepository.findAll();
        for (Api api : apis) {
            if ("cloudflare".equals(api.getType())) {
                cloudflareStatus(api, false);
            } else if (Boolean.TRUE.equals(api.getIsEvmChain())) {
                updateEvmApiStatus(api);
            } else if (api.getRpcAddress() != null && !api.getRpcAddress().isBlank()) {
                updateApiWithRpc(api);
            } else if (api.getApiLink() != null) {
                updateApi(api);
            }
        }
    }

    public void sendDailyCloudflareStatus() {
        List<Api> apis = apiRepository.findByType("cloudflare");
        for (Api api : apis) {
            Map<String, Object> result = cloudflareStatus(api, true);
            if (result != null) {
                botService.sendCdnStatus(api.getApiName(), result);
            }
        }
    }

    public void checkTokenBalanceStatus() {
        List<Api> apis = apiRepository.findByType("token_balance");
        for (Api api : apis) {
            updateTokenBalance(api);
        }
    }

    public Map<String, Object> cloudflareStatus(Api api, boolean dailyMessage) {
        JsonNode resultNode = apiClient.getCdnStatus(api.getApiLink());
        Map<String, Object> result;
        if (!resultNode.path("count").isMissingNode()) {
            double current = resultNode.path("count").path("current").asDouble();
            double allowed = resultNode.path("count").path("allowed").asDouble();
            if (current == 0 || allowed == 0) {
                throw new ApiException(500, "Error occured while finding cloudflare status");
            }
            double percentage = Double.parseDouble(String.format("%.2f", current / allowed));
            result = Map.of(
                "percentage", percentage,
                "type", "image",
                "count", Map.of("current", current, "allowed", allowed)
            );
        } else {
            double totalStorageMinutes = resultNode.path("totalStorageMinutes").asDouble();
            double totalStorageMinutesLimit = resultNode.path("totalStorageMinutesLimit").asDouble();
            if (totalStorageMinutes == 0 || totalStorageMinutesLimit == 0) {
                throw new ApiException(500, "Error occured while finding cloudflare status");
            }
            double percentage = Double.parseDouble(String.format("%.2f", totalStorageMinutes / totalStorageMinutesLimit));
            result = Map.of(
                "percentage", percentage,
                "type", "video",
                "response", Map.of(
                    "videoCount", resultNode.path("videoCount").asText(),
                    "totalStorageMinutes", totalStorageMinutes,
                    "totalStorageMinutesLimit", totalStorageMinutesLimit
                )
            );
        }

        api.setPercentage((Double) result.get("percentage"));
        api.setStatus("active");
        apiRepository.save(api);

        if (dailyMessage) {
            return result;
        }

        double percentage = (double) result.get("percentage");
        boolean isImage = "image".equals(result.get("type"));
        double limit = isImage ? appProperties.getCloudflare().getImagePercentLimit()
            : appProperties.getCloudflare().getVideoPercentLimit();
        boolean shouldNotify = percentage >= limit;
        boolean alreadyNotified = Boolean.TRUE.equals(api.getIsNotified());

        if (shouldNotify) {
            if (!alreadyNotified || (api.getNotifiedTime() != null
                && (new Date().getTime() - api.getNotifiedTime().getTime()) > appProperties.getCloudflare().getNotifiedTimeLimitMs())) {
                botService.sendMessage(api.getApiLink(), api.getApiName(), 13, result, false);
                botService.sendMessageSlack(api.getApiLink(), api.getApiName(), 13, result, false);
                api.setIsNotified(true);
                api.setNotifiedTime(new Date());
                apiRepository.save(api);
            }
        } else if (alreadyNotified) {
            api.setIsNotified(false);
            apiRepository.save(api);
            botService.sendMessage(api.getApiLink(), api.getApiName(), 3, result, false);
            botService.sendMessageSlack(api.getApiLink(), api.getApiName(), 3, result, false);
        }
        return result;
    }

    public void updateApiWithRpc(Api api) {
        JsonNode apiResponse;
        try {
            apiResponse = apiClient.getApiStatus(api.getApiLink());
        } catch (ApiException ex) {
            botService.sendMessage(api.getApiLink(), api.getApiName(), null, null, null);
            botService.sendMessageSlack(api.getApiLink(), api.getApiName(), null, null, null);
            updateStatus(api, false, null);
            return;
        }

        long blockHeight;
        try {
            blockHeight = apiClient.getBlockHeight(api.getRpcAddress());
        } catch (ApiException ex) {
            handleRpcError(api);
            return;
        }

        long apiHeight = apiResponse.path("result").path("height").asLong();
        int triggerBlock = api.getTriggerBlock() != null ? api.getTriggerBlock() : appProperties.getTriggerBlock();
        int blocksBehind = (int) (blockHeight - apiHeight);
        if (blocksBehind > triggerBlock) {
            String updatedAt = apiResponse.path("result").path("updatedAt").asText();
            String time = updatedAt == null || updatedAt.isBlank()
                ? DATE_FORMATTER.format(new Date().toInstant().atZone(IST))
                : updatedAt;
            boolean recovering = api.getBlocksBehind() != null && blocksBehind < api.getBlocksBehind();
            Map<String, Object> payload = Map.of(
                "blocksBehind", blocksBehind,
                "updatedAt", time
            );
            botService.sendMessage(api.getApiLink(), api.getApiName(), 7, payload, recovering);
            botService.sendMessageSlack(api.getApiLink(), api.getApiName(), 7, payload, recovering);
            if (recovering) {
                initializeCron(api.getApiLink(), api.getRpcAddress(), api.getApiName());
            }
            updateStatus(api, true, blocksBehind);
        } else {
            updateStatus(api, true, blocksBehind);
        }
    }

    public void updateApi(Api api) {
        try {
            apiClient.getApiStatus(api.getApiLink());
            updateStatus(api, true, null);
        } catch (ApiException ex) {
            System.out.println("Error updating API status: " + ex.getMessage());
            botService.sendMessage(api.getApiLink(), api.getApiName(), null, null, null);
            botService.sendMessageSlack(api.getApiLink(), api.getApiName(), null, null, null);
            updateStatus(api, false, null);
        }
    }

    public void updateEvmApiStatus(Api api) {
        JsonNode apiResponse;
        try {
            apiResponse = apiClient.getApiStatus(api.getApiLink());
        } catch (ApiException ex) {
            botService.sendMessage(api.getApiLink(), api.getApiName(), null, null, null);
            botService.sendMessageSlack(api.getApiLink(), api.getApiName(), null, null, null);
            updateStatus(api, false, null);
            return;
        }

        long blockHeight;
        try {
            blockHeight = apiClient.getEvmBlockHeight(api.getBaseAddress());
        } catch (ApiException ex) {
            handleRpcError(api);
            return;
        }

        long apiHeight = apiResponse.path("result").path("height").asLong();
        int triggerBlock = api.getTriggerBlock() != null ? api.getTriggerBlock() : appProperties.getTriggerBlock();
        int blocksBehind = (int) (blockHeight - apiHeight);
        if (blocksBehind > triggerBlock) {
            String updatedAt = apiResponse.path("result").path("updatedAt").asText();
            String time = updatedAt == null || updatedAt.isBlank()
                ? DATE_FORMATTER.format(new Date().toInstant().atZone(IST))
                : updatedAt;
            boolean recovering = api.getBlocksBehind() != null && blocksBehind < api.getBlocksBehind();
            Map<String, Object> payload = Map.of(
                "blocksBehind", blocksBehind,
                "updatedAt", time
            );
            botService.sendMessage(api.getApiLink(), api.getApiName(), 7, payload, recovering);
            botService.sendMessageSlack(api.getApiLink(), api.getApiName(), 7, payload, recovering);
            if (recovering) {
                initializeCron(api.getApiLink(), api.getRpcAddress(), api.getApiName());
            }
            updateStatus(api, true, blocksBehind);
        } else {
            updateStatus(api, true, blocksBehind);
        }
    }

    public void updateStatus(Api api, boolean active, Integer blocksBehind) {
        Api previous = apiRepository.findById(api.getId()).orElse(api);
        if (active) {
            api.setStatus("active");
            api.setLastUptime(new Date());
            api.setDowntimeCount(0);
        } else {
            api.setStatus("inactive");
            api.setLastDowntime(new Date());
            api.setDowntimeCount(previous.getDowntimeCount() == null ? 1 : previous.getDowntimeCount() + 1);
        }
        if (blocksBehind != null) {
            api.setBlocksBehind(blocksBehind);
            api.setRpcDowntimeCount(0);
        }
        System.out.println("Updating status for API: " + api.getApiLink() + " to " + api.getStatus());
        apiRepository.save(api);

        int triggerBlock = previous.getTriggerBlock() != null ? previous.getTriggerBlock() : appProperties.getTriggerBlock();
        if ("inactive".equals(previous.getStatus()) && active) {
            botService.sendMessage(api.getApiLink(), api.getApiName(), 2, null, false);
            botService.sendMessageSlack(api.getApiLink(), api.getApiName(), 2, null, false);
        }
        if (active && previous.getBlocksBehind() != null
            && previous.getBlocksBehind() > triggerBlock
            && blocksBehind != null && blocksBehind <= triggerBlock) {
            botService.sendMessage(api.getApiLink(), api.getApiName(), 1, null, false);
            botService.sendMessageSlack(api.getApiLink(), api.getApiName(), 1, null, false);
        }
        if (active && previous.getRpcDowntimeCount() != null
            && previous.getRpcDowntimeCount() > 2 && blocksBehind != null) {
            botService.sendMessage(api.getApiLink(), api.getApiName(), 2, null, false);
            botService.sendMessageSlack(api.getApiLink(), api.getApiName(), 2, null, false);
        }
    }

    public void handleRpcError(Api api) {
        if (api.getRpcDowntimeCount() != null && api.getRpcDowntimeCount() >= 2) {
            api.setRpcDowntimeCount(api.getRpcDowntimeCount() + 1);
            apiRepository.save(api);
            botService.sendMessage(api.getApiLink(), api.getApiName(), 12, null, null);
            botService.sendMessageSlack(api.getApiLink(), api.getApiName(), 12, null, null);
            return;
        }
        api.setRpcDowntimeCount(api.getRpcDowntimeCount() == null ? 1 : api.getRpcDowntimeCount() + 1);
        apiRepository.save(api);
    }

    public void updateTokenBalance(Api api) {
        String baseUrl = api.getApiUrl() != null ? api.getApiUrl() : api.getApiLink();
        if (baseUrl == null) {
            return;
        }
        String url = String.format(
            "%s?chainid=%s&module=account&action=tokenbalance&contractaddress=%s&address=%s&tag=latest&apikey=%s",
            baseUrl, api.getChainId(), api.getContractAddress(), api.getTokenAddress(), api.getApiKey());

        JsonNode response = apiClient.getTokenBalance(url);
        BigInteger balance = new BigInteger(response.path("result").asText("0"));

        if (balance.compareTo(THRESHOLD_500K) <= 0) {
            botService.sendTokenBalanceMessage(api, 1, balance, THRESHOLD_500K);
            updateTokenBalanceCheckStatus(api, balance, false);
        } else if (balance.compareTo(THRESHOLD_1M) <= 0 && !Boolean.TRUE.equals(api.getIsAlertSentFor1M())) {
            botService.sendTokenBalanceMessage(api, 2, balance, THRESHOLD_1M);
            api.setIsAlertSentFor1M(true);
            updateTokenBalanceCheckStatus(api, balance, false);
        } else if (balance.compareTo(THRESHOLD_2M) <= 0 && !Boolean.TRUE.equals(api.getIsAlertSentFor2M())) {
            botService.sendTokenBalanceMessage(api, 3, balance, THRESHOLD_2M);
            api.setIsAlertSentFor2M(true);
            updateTokenBalanceCheckStatus(api, balance, false);
        } else {
            if ("1".equals(response.path("status").asText())) {
                if (balance.compareTo(THRESHOLD_2M) > 0) {
                    api.setIsAlertSentFor2M(false);
                    api.setIsAlertSentFor1M(false);
                } else if (balance.compareTo(THRESHOLD_1M) > 0) {
                    api.setIsAlertSentFor1M(false);
                }
                updateTokenBalanceCheckStatus(api, balance, true);
            }
        }
    }

    private void updateTokenBalanceCheckStatus(Api api, BigInteger tokenBalance, boolean active) {
        Api previous = apiRepository.findById(api.getId()).orElse(api);
        if (active) {
            api.setStatus("active");
            api.setLastUptime(new Date());
            api.setDowntimeCount(0);
        } else {
            api.setStatus("inactive");
            api.setLastDowntime(new Date());
            api.setDowntimeCount(previous.getDowntimeCount() == null ? 1 : previous.getDowntimeCount() + 1);
        }
        double tokenBalanceEth = new BigDecimal(tokenBalance)
            .divide(new BigDecimal("1000000000000000000"), 6, java.math.RoundingMode.HALF_UP)
            .doubleValue();
        api.setTokenBalance(tokenBalanceEth);
        if (previous.getTokenBalance() != null && tokenBalanceEth > previous.getTokenBalance()) {
            botService.sendTokenBalanceMessage(api, 4, tokenBalance, null);
            if (tokenBalance.compareTo(RESET_10M) > 0) {
                api.setIsAlertSentFor1M(false);
            }
            if (tokenBalance.compareTo(RESET_20M) > 0) {
                api.setIsAlertSentFor2M(false);
            }
        }
        apiRepository.save(api);
    }

    private void initializeCron(String link, String rpc, String name) {
        String key = link + "-" + rpc;
        if (runningCrons.containsKey(key)) {
            return;
        }
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
            boolean synced;
            try {
                synced = apiClient.checkBlockHeights(link, rpc);
            } catch (Exception ex) {
                stopCron(key);
                return;
            }
            if (synced) {
                botService.sendMessage(link, name, 1, null, false);
                botService.sendMessageSlack(link, name, 1, null, false);
                stopCron(key);
            }
        }, 0, 10, TimeUnit.SECONDS);
        runningCrons.put(key, future);
    }

    private void stopCron(String key) {
        ScheduledFuture<?> future = runningCrons.remove(key);
        if (future != null) {
            future.cancel(true);
        }
    }
}
