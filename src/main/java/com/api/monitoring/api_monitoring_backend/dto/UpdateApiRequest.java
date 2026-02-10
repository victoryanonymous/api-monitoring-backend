package com.api.monitoring.api_monitoring_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import com.api.monitoring.api_monitoring_backend.validation.ValidationPatterns;

public class UpdateApiRequest {
    @Pattern(regexp = ValidationPatterns.URL)
    private String apiLink;

    @Pattern(regexp = ValidationPatterns.NAME)
    private String apiName;

    @Pattern(regexp = ValidationPatterns.URL)
    private String rpcAddress;

    @Min(50)
    private Integer triggerBlock;

    private String type;

    public String getApiLink() {
        return apiLink;
    }

    public void setApiLink(String apiLink) {
        this.apiLink = apiLink;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getRpcAddress() {
        return rpcAddress;
    }

    public void setRpcAddress(String rpcAddress) {
        this.rpcAddress = rpcAddress;
    }

    public Integer getTriggerBlock() {
        return triggerBlock;
    }

    public void setTriggerBlock(Integer triggerBlock) {
        this.triggerBlock = triggerBlock;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
