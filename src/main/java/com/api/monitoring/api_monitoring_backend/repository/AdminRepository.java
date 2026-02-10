package com.api.monitoring.api_monitoring_backend.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.api.monitoring.api_monitoring_backend.model.Admin;

public interface AdminRepository extends MongoRepository<Admin, String> {
    Optional<Admin> findByEmailAddress(String emailAddress);
}
