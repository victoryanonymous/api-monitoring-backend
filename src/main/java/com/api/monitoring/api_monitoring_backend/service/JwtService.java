package com.api.monitoring.api_monitoring_backend.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.api.monitoring.api_monitoring_backend.config.AppProperties;
import com.api.monitoring.api_monitoring_backend.security.JwtPayload;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final long ACCESS_TOKEN_SECONDS = 4 * 60 * 60;
    private static final long REFRESH_TOKEN_SECONDS = 30L * 24 * 60 * 60;

    private final AppProperties appProperties;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String generateAccessToken(String userId, String loginId) {
        return generateToken(userId, loginId, ACCESS_TOKEN_SECONDS);
    }

    public String generateRefreshToken(String userId, String loginId) {
        return generateToken(userId, loginId, REFRESH_TOKEN_SECONDS);
    }

    public JwtPayload verify(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token)
            .getBody();

        String userId = String.valueOf(claims.get("user_id"));
        String loginId = String.valueOf(claims.get("login_id"));
        return new JwtPayload(userId, loginId);
    }

    private String generateToken(String userId, String loginId, long expiresInSeconds) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", userId);
        claims.put("login_id", loginId);

        long nowMs = System.currentTimeMillis();
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(new Date(nowMs))
            .setExpiration(new Date(nowMs + expiresInSeconds * 1000))
            .signWith(Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)),
                SignatureAlgorithm.HS256)
            .compact();
    }
}
