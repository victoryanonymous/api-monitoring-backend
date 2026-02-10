package com.api.monitoring.api_monitoring_backend.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.api.monitoring.api_monitoring_backend.dto.AddApiRequest;
import com.api.monitoring.api_monitoring_backend.dto.UpdateApiRequest;
import com.api.monitoring.api_monitoring_backend.exception.ApiException;
import com.api.monitoring.api_monitoring_backend.model.Api;
import com.api.monitoring.api_monitoring_backend.repository.ApiRepository;
import com.api.monitoring.api_monitoring_backend.util.ApiConstants;
import com.api.monitoring.api_monitoring_backend.validation.ValidationPatterns;

@RestController
public class ApiController {
    private static final Pattern OBJECT_ID_PATTERN = Pattern.compile(ValidationPatterns.OBJECT_ID);

    private final ApiRepository apiRepository;

    public ApiController(ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    @PostMapping("/api")
    public ResponseEntity<Map<String, Object>> addApi(@Valid @RequestBody AddApiRequest request) {
        if (!ApiConstants.VALID_API_TYPES_CREATE.contains(request.getType())) {
            throw new ApiException(422, "Invalid API type");
        }
        String apiLink = request.getApiLink().replaceAll("/$", "");
        Optional<Api> existing = apiRepository.findByApiLink(apiLink);
        if (existing.isPresent()) {
            throw new ApiException(500, "API already exists with the provided link");
        }

        Api api = new Api();
        api.setApiLink(apiLink);
        api.setApiName(request.getApiName());
        api.setType(request.getType());
        if (request.getRpcAddress() != null) {
            api.setRpcAddress(request.getRpcAddress());
            api.setTriggerBlock(request.getTriggerBlock() != null ? request.getTriggerBlock() : 50);
        }
        apiRepository.save(api);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("result", api);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/{id}")
    public ResponseEntity<Map<String, Object>> updateApi(
        @PathVariable("id") String id,
        @Valid @RequestBody UpdateApiRequest request
    ) {
        if (!OBJECT_ID_PATTERN.matcher(id).matches()) {
            throw new ApiException(422, "Invalid id");
        }
        if (request.getType() != null && !ApiConstants.VALID_API_TYPES_CREATE.contains(request.getType())) {
            throw new ApiException(422, "Invalid API type");
        }

        Api api = apiRepository.findById(id)
            .orElseThrow(() -> new ApiException(500, "API does not exist with the given link"));

        if (request.getApiLink() != null) {
            String apiLink = request.getApiLink().replaceAll("/$", "");
            Optional<Api> existing = apiRepository.findByApiLink(apiLink);
            if (existing.isPresent() && !existing.get().getId().equals(api.getId())) {
                throw new ApiException(500, "Error occured while updating API/API already exists with the provided link");
            }
            api.setApiLink(apiLink);
        }
        if (request.getApiName() != null) {
            api.setApiName(request.getApiName());
        }
        if (request.getType() != null) {
            api.setType(request.getType());
        }
        if (request.getRpcAddress() != null) {
            api.setRpcAddress(request.getRpcAddress());
        }
        if (request.getTriggerBlock() != null) {
            api.setTriggerBlock(request.getTriggerBlock());
        }

        apiRepository.save(api);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("result", api);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/{id}")
    public ResponseEntity<Map<String, Object>> deleteApi(@PathVariable("id") String id) {
        if (!OBJECT_ID_PATTERN.matcher(id).matches()) {
            throw new ApiException(422, "Invalid id");
        }
        Api api = apiRepository.findById(id)
            .orElseThrow(() -> new ApiException(500, "API does not exist for the given id"));
        apiRepository.delete(api);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("msg", "API deleted successfully!");
        return ResponseEntity.ok(response);
    }
}
