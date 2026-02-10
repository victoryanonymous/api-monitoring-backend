package com.api.monitoring.api_monitoring_backend.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.api.monitoring.api_monitoring_backend.model.UserLogin;

public interface UserLoginRepository extends MongoRepository<UserLogin, String> {
    Optional<UserLogin> findByIdAndUserIdAndAccessToken(String id, String userId, String accessToken);
    Optional<UserLogin> findByIdAndUserId(String id, String userId);
    Optional<UserLogin> findByUserIdAndAccessToken(String userId, String accessToken);
}
