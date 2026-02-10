package com.api.monitoring.api_monitoring_backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.monitoring.api_monitoring_backend.exception.ApiException;
import com.api.monitoring.api_monitoring_backend.model.Api;
import com.api.monitoring.api_monitoring_backend.repository.ApiRepository;
import com.api.monitoring.api_monitoring_backend.util.ApiConstants;
import com.api.monitoring.api_monitoring_backend.validation.ValidationPatterns;

@RestController
public class StatusController {
    private static final Pattern OBJECT_ID_PATTERN = Pattern.compile(ValidationPatterns.OBJECT_ID);

    private final ApiRepository apiRepository;
    private final MongoTemplate mongoTemplate;

    public StatusController(ApiRepository apiRepository, MongoTemplate mongoTemplate) {
        this.apiRepository = apiRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/api/{id}")
    public ResponseEntity<Map<String, Object>> getStatusApi(@PathVariable("id") String id) {
        if (!OBJECT_ID_PATTERN.matcher(id).matches()) {
            throw new ApiException(422, "Invalid id");
        }
        Api api = apiRepository.findById(id)
            .orElseThrow(() -> new ApiException(500, "API does not exist with the given id"));
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("result", api);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/apis/status")
    public ResponseEntity<Map<String, Object>> getStatusApis(
        @RequestParam(value = "limit", required = false) String limitParam,
        @RequestParam(value = "skip", required = false) String skipParam,
        @RequestParam(value = "order", required = false) String order,
        @RequestParam(value = "sortBy", required = false) String sortBy,
        @RequestParam(value = "filterByStatus", required = false) String filterByStatus,
        @RequestParam(value = "filterByType", required = false) String filterByType
    ) {
        int limit = ApiConstants.DEFAULT_LIMIT;
        int skip = ApiConstants.DEFAULT_SKIP;

        if (limitParam != null) {
            try {
                limit = Integer.parseInt(limitParam);
            } catch (NumberFormatException ex) {
                throw new ApiException(422, "Invalid limit");
            }
        }
        if (skipParam != null) {
            try {
                skip = Integer.parseInt(skipParam);
            } catch (NumberFormatException ex) {
                throw new ApiException(422, "Invalid skip");
            }
        }
        if (limit < 0 || skip < 0) {
            throw new ApiException(422, "Invalid pagination");
        }

        if (order != null && !order.equals("asc") && !order.equals("desc")) {
            throw new ApiException(422, "Invalid order");
        }
        if (sortBy != null && !ApiConstants.VALID_SORT_FIELDS.contains(sortBy)) {
            throw new ApiException(422, "Invalid sortBy");
        }
        if (filterByStatus != null && !ApiConstants.VALID_STATUS.contains(filterByStatus)) {
            throw new ApiException(422, "Invalid filterByStatus");
        }
        if (filterByType != null && !ApiConstants.VALID_API_TYPES_CREATE.contains(filterByType)) {
            throw new ApiException(422, "Invalid filterByType");
        }

        Criteria criteria = new Criteria();
        if (filterByStatus != null) {
            criteria = criteria.and("status").is(filterByStatus);
        }
        if (filterByType != null) {
            criteria = criteria.and("type").is(filterByType);
        }
        Query query = new Query(criteria);
        if (sortBy != null) {
            Sort.Direction direction = "desc".equals(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
            query.with(Sort.by(direction, sortBy));
        }
        long total = mongoTemplate.count(query, Api.class);
        query.skip(skip).limit(limit);
        List<Api> apis = mongoTemplate.find(query, Api.class);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        Map<String, Object> result = new HashMap<>();
        result.put("list", apis);
        result.put("count", total);
        response.put("result", result);
        return ResponseEntity.ok(response);
    }
}
