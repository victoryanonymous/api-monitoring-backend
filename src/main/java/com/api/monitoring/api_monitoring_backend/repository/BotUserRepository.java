package com.api.monitoring.api_monitoring_backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.api.monitoring.api_monitoring_backend.model.BotUser;

public interface BotUserRepository extends MongoRepository<BotUser, String> {
}
