package com.api.monitoring.api_monitoring_backend.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.api.monitoring.api_monitoring_backend.service.ApiMonitorService;
import com.api.monitoring.api_monitoring_backend.service.BotService;

@Component
public class ApiMonitorScheduler {
    private final ApiMonitorService apiMonitorService;
    private final BotService botService;

    public ApiMonitorScheduler(ApiMonitorService apiMonitorService, BotService botService) {
        this.apiMonitorService = apiMonitorService;
        this.botService = botService;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void updateStatuses() {
        apiMonitorService.updateApiStatuses();
    }

    @Scheduled(cron = "0 0 9,21 * * *", zone = "Asia/Kolkata")
    public void sendDailyReport() {
        botService.sendActiveMessage();
        apiMonitorService.sendDailyCloudflareStatus();
    }

    @Scheduled(cron = "0 */30 * * * *")
    public void updateTokenBalance() {
        apiMonitorService.checkTokenBalanceStatus();
    }
}
