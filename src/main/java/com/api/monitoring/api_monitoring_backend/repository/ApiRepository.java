package com.api.monitoring.api_monitoring_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.api.monitoring.api_monitoring_backend.model.Api;

public interface ApiRepository extends MongoRepository<Api, String> {
    Optional<Api> findByApiLink(String apiLink);
    List<Api> findByType(String type);
}
