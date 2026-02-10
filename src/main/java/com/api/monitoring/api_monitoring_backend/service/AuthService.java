package com.api.monitoring.api_monitoring_backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.api.monitoring.api_monitoring_backend.exception.ApiException;
import com.api.monitoring.api_monitoring_backend.model.Admin;
import com.api.monitoring.api_monitoring_backend.model.UserLogin;
import com.api.monitoring.api_monitoring_backend.repository.AdminRepository;
import com.api.monitoring.api_monitoring_backend.repository.UserLoginRepository;
import com.api.monitoring.api_monitoring_backend.security.JwtPayload;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {
    private final AdminRepository adminRepository;
    private final UserLoginRepository userLoginRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        AdminRepository adminRepository,
        UserLoginRepository userLoginRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.adminRepository = adminRepository;
        this.userLoginRepository = userLoginRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public TokenPair login(String emailAddress, String profilePassword, HttpServletRequest request) {
        Admin admin = adminRepository.findByEmailAddress(emailAddress.toLowerCase())
            .orElseThrow(() -> new ApiException(500, "Invalid email or password"));

        if (!passwordEncoder.matches(profilePassword, admin.getProfilePassword())) {
            throw new ApiException(500, "Invalid email or password");
        }

        UserLogin login = new UserLogin();
        login.setUserId(admin.getId());
        login = userLoginRepository.save(login);

        String accessToken = jwtService.generateAccessToken(admin.getId(), login.getId());
        String refreshToken = jwtService.generateRefreshToken(admin.getId(), login.getId());
        login.setAccessToken(accessToken);
        login.setRefreshToken(refreshToken);
        login.setRemoteAddress(request.getHeader("x-forwarded-for") != null
            ? request.getHeader("x-forwarded-for")
            : request.getRemoteAddr());
        userLoginRepository.save(login);

        return new TokenPair(accessToken, refreshToken);
    }

    public String refreshToken(String refreshToken, HttpServletRequest request) {
        JwtPayload payload;
        try {
            payload = jwtService.verify(refreshToken);
        } catch (Exception ex) {
            throw new ApiException(500, "JWT token is expired or invalid");
        }

        UserLogin login = userLoginRepository.findByIdAndUserId(payload.getLoginId(), payload.getUserId())
            .orElseThrow(() -> new ApiException(500, "User Login not found with the given id"));

        String accessToken = jwtService.generateAccessToken(payload.getUserId(), payload.getLoginId());
        String newRefreshToken = jwtService.generateRefreshToken(payload.getUserId(), payload.getLoginId());
        login.setAccessToken(accessToken);
        login.setRefreshToken(newRefreshToken);
        login.setRemoteAddress(request.getHeader("x-forwarded-for") != null
            ? request.getHeader("x-forwarded-for")
            : request.getRemoteAddr());
        userLoginRepository.save(login);

        return accessToken;
    }

    public void logout(Admin admin, String accessToken) {
        UserLogin login = userLoginRepository.findByUserIdAndAccessToken(admin.getId(), accessToken)
            .orElseThrow(() -> new ApiException(500, "User login not found with the given id"));
        userLoginRepository.delete(login);
    }

    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;

        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}
