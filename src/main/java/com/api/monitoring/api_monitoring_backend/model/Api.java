package com.api.monitoring.api_monitoring_backend.model;

import java.util.Date;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "apis")
public class Api {

    @Id
    private String id;

    @Field("api_link")
    @Indexed(unique = true)
    private String apiLink;

    @Field("rpc_address")
    private String rpcAddress;

    @Field("api_name")
    private String apiName;

    @Field("trigger_block")
    private Integer triggerBlock;

    @Field("blocks_behind")
    private Integer blocksBehind;

    @Field("type")
    private String type;

    @Field("status")
    private String status = "inactive";

    @Field("last_uptime")
    private Date lastUptime = new Date();

    @Field("last_downtime")
    private Date lastDowntime;

    @Field("downtime_count")
    private Integer downtimeCount = 0;

    @Field("percentage")
    private Double percentage;

    @Field("is_notified")
    private Boolean isNotified = false;

    @Field("notified_time")
    private Date notifiedTime;

    @Field("rpc_downtime_count")
    private Integer rpcDowntimeCount = 0;

    @Field("chain_id")
    private Integer chainId;

    @Field("contract_address")
    private String contractAddress;

    @Field("token_address")
    private String tokenAddress;

    @Field("api_key")
    private String apiKey;

    @Field("token_balance")
    private Double tokenBalance;

    @Field("is_alert_sent_for_2M")
    private Boolean isAlertSentFor2M = false;

    @Field("is_alert_sent_for_1M")
    private Boolean isAlertSentFor1M = false;

    @Field("is_evm_chain")
    private Boolean isEvmChain = false;

    @Field("base_address")
    private String baseAddress;

    @Field("api_url")
    private String apiUrl;

    @CreatedDate
    @Field("created_at")
    private Date createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Date updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApiLink() {
        return apiLink;
    }

    public void setApiLink(String apiLink) {
        this.apiLink = apiLink;
    }

    public String getRpcAddress() {
        return rpcAddress;
    }

    public void setRpcAddress(String rpcAddress) {
        this.rpcAddress = rpcAddress;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public Integer getTriggerBlock() {
        return triggerBlock;
    }

    public void setTriggerBlock(Integer triggerBlock) {
        this.triggerBlock = triggerBlock;
    }

    public Integer getBlocksBehind() {
        return blocksBehind;
    }

    public void setBlocksBehind(Integer blocksBehind) {
        this.blocksBehind = blocksBehind;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getLastUptime() {
        return lastUptime;
    }

    public void setLastUptime(Date lastUptime) {
        this.lastUptime = lastUptime;
    }

    public Date getLastDowntime() {
        return lastDowntime;
    }

    public void setLastDowntime(Date lastDowntime) {
        this.lastDowntime = lastDowntime;
    }

    public Integer getDowntimeCount() {
        return downtimeCount;
    }

    public void setDowntimeCount(Integer downtimeCount) {
        this.downtimeCount = downtimeCount;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public Boolean getIsNotified() {
        return isNotified;
    }

    public void setIsNotified(Boolean isNotified) {
        this.isNotified = isNotified;
    }

    public Date getNotifiedTime() {
        return notifiedTime;
    }

    public void setNotifiedTime(Date notifiedTime) {
        this.notifiedTime = notifiedTime;
    }

    public Integer getRpcDowntimeCount() {
        return rpcDowntimeCount;
    }

    public void setRpcDowntimeCount(Integer rpcDowntimeCount) {
        this.rpcDowntimeCount = rpcDowntimeCount;
    }

    public Integer getChainId() {
        return chainId;
    }

    public void setChainId(Integer chainId) {
        this.chainId = chainId;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getTokenAddress() {
        return tokenAddress;
    }

    public void setTokenAddress(String tokenAddress) {
        this.tokenAddress = tokenAddress;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Double getTokenBalance() {
        return tokenBalance;
    }

    public void setTokenBalance(Double tokenBalance) {
        this.tokenBalance = tokenBalance;
    }

    public Boolean getIsAlertSentFor2M() {
        return isAlertSentFor2M;
    }

    public void setIsAlertSentFor2M(Boolean isAlertSentFor2M) {
        this.isAlertSentFor2M = isAlertSentFor2M;
    }

    public Boolean getIsAlertSentFor1M() {
        return isAlertSentFor1M;
    }

    public void setIsAlertSentFor1M(Boolean isAlertSentFor1M) {
        this.isAlertSentFor1M = isAlertSentFor1M;
    }

    public Boolean getIsEvmChain() {
        return isEvmChain;
    }

    public void setIsEvmChain(Boolean isEvmChain) {
        this.isEvmChain = isEvmChain;
    }

    public String getBaseAddress() {
        return baseAddress;
    }

    public void setBaseAddress(String baseAddress) {
        this.baseAddress = baseAddress;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
