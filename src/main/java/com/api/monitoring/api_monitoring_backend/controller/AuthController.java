package com.api.monitoring.api_monitoring_backend.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.api.monitoring.api_monitoring_backend.dto.RefreshTokenRequest;
import com.api.monitoring.api_monitoring_backend.dto.LoginRequest;
import com.api.monitoring.api_monitoring_backend.exception.ApiException;
import com.api.monitoring.api_monitoring_backend.model.Admin;
import com.api.monitoring.api_monitoring_backend.service.AuthService;

@RestController
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
        HttpServletRequest httpServletRequest) {
        AuthService.TokenPair tokens = authService.login(
            request.getEmailAddress(),
            request.getProfilePassword(),
            httpServletRequest
        );
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("access_token", tokens.getAccessToken());
        response.put("refresh_token", tokens.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshToken(@Valid @RequestBody RefreshTokenRequest request,
        HttpServletRequest httpServletRequest) {
        String accessToken = authService.refreshToken(request.getRefreshToken(), httpServletRequest);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("access_token", accessToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
        @RequestAttribute("authenticatedUser") Admin admin,
        HttpServletRequest httpServletRequest
    ) {
        String authHeader = httpServletRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ApiException(500, "JWT token is required for this request");
        }
        String token = authHeader.substring(7);
        authService.logout(admin, token);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User logged out successfully");
        return ResponseEntity.ok(response);
    }
}
