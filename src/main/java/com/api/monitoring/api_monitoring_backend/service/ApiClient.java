package com.api.monitoring.api_monitoring_backend.service;

import java.math.BigInteger;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.api.monitoring.api_monitoring_backend.config.AppProperties;
import com.api.monitoring.api_monitoring_backend.exception.ApiException;
import com.api.monitoring.api_monitoring_backend.util.HttpClientUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ApiClient {

    private final ObjectMapper objectMapper;
    private final HttpClientUtil httpClientUtil;
    private final AppProperties appProperties;

    public ApiClient(ObjectMapper objectMapper, HttpClientUtil httpClientUtil, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.httpClientUtil = httpClientUtil;
        this.appProperties = appProperties;
    }

    public long getBlockHeight(String rpcAddress) {
        try {
            ResponseEntity<String> response = httpClientUtil.getWithRetries(
                rpcAddress + "/block?height", new HttpHeaders(), 3);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode heightNode = root.path("result").path("block").path("header").path("height");
            if (heightNode.isMissingNode() || heightNode.asText().isEmpty()) {
                throw new ApiException(500, "Not able to find latest block height");
            }
            return heightNode.asLong();
        } catch (Exception ex) {
            throw new ApiException(500, "Unable to connect to the RPC server");
        }
    }

    public long getEvmBlockHeight(String baseAddress) {
        try {
            String url = baseAddress + "/v3/" + appProperties.getInfuraKey();
            String payload = objectMapper.writeValueAsString(
                Map.of("jsonrpc", "2.0", "id", 1, "method", "eth_blockNumber", "params", new Object[] {}));
            ResponseEntity<String> response = httpClientUtil.postJsonWithRetries(url, payload, 3);
            JsonNode root = objectMapper.readTree(response.getBody());
            String hex = root.path("result").asText();
            if (hex == null || hex.isBlank()) {
                throw new ApiException(500, "Unable to connect to the RPC server");
            }
            return new BigInteger(hex.substring(2), 16).longValue();
        } catch (Exception ex) {
            throw new ApiException(500, "Unable to connect to the RPC server");
        }
    }

    public JsonNode getApiStatus(String url) {
        try {
            ResponseEntity<String> response = httpClientUtil.getWithRetries(url, new HttpHeaders(), 3);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.path("success").asBoolean(false)) {
                return root;
            }
            throw new ApiException(500, "Error occured while finding API");
        } catch (Exception ex) {
            throw new ApiException(500, "API does not exist with the given url");
        }
    }

    public boolean checkBlockHeights(String url, String rpcAddress) {
        long rpcHeight = getBlockHeight(rpcAddress);
        JsonNode apiStatus = getApiStatus(url);
        long height = apiStatus.path("result").path("height").asLong();
        return Math.abs(height - rpcHeight) <= 5;
    }

    public JsonNode getCdnStatus(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + appProperties.getCloudflare().getToken());
            ResponseEntity<String> response = httpClientUtil.getWithRetries(url, headers, 3);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.path("success").asBoolean(false)) {
                throw new ApiException(500, "Error occured while finding cloudflare status");
            }
            return root.path("result");
        } catch (Exception ex) {
            throw new ApiException(500, "Error occured while finding cloudflare status");
        }
    }

    public JsonNode getTokenBalance(String url) {
        try {
            ResponseEntity<String> response = httpClientUtil.getWithRetries(url, new HttpHeaders(), 3);
            JsonNode root = objectMapper.readTree(response.getBody());
            if ("1".equals(root.path("status").asText())) {
                return root;
            }
            throw new ApiException(500, "Error occured while finding API");
        } catch (Exception ex) {
            throw new ApiException(500, "API does not exist with the given url");
        }
    }
}
