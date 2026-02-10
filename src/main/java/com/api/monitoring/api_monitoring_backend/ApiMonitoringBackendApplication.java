package com.api.monitoring.api_monitoring_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.api.monitoring.api_monitoring_backend.config.AppProperties;

@SpringBootApplication
@EnableScheduling
@EnableMongoAuditing
@EnableConfigurationProperties(AppProperties.class)
public class ApiMonitoringBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiMonitoringBackendApplication.class, args);
	}

}
