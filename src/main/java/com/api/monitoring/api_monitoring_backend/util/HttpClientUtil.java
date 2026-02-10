package com.api.monitoring.api_monitoring_backend.util;

import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpClientUtil {
    private final RestTemplate restTemplate = new RestTemplate();

    public ResponseEntity<String> getWithRetries(String url, HttpHeaders headers, int retries) {
        return executeWithRetries(() -> restTemplate.exchange(url, HttpMethod.GET,
            new HttpEntity<>(headers), String.class), retries);
    }

    public ResponseEntity<String> postJsonWithRetries(String url, String payload, int retries) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return executeWithRetries(() -> restTemplate.exchange(url, HttpMethod.POST,
            new HttpEntity<>(payload, headers), String.class), retries);
    }

    private ResponseEntity<String> executeWithRetries(Supplier<ResponseEntity<String>> supplier, int retries) {
        RuntimeException last = null;
        for (int i = 0; i < retries; i++) {
            try {
                return supplier.get();
            } catch (RuntimeException ex) {
                last = ex;
                try {
                    Thread.sleep(Duration.ofMillis((long) (200 * (i + 1))).toMillis());
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw last;
    }
}
