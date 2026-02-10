package com.api.monitoring.api_monitoring_backend.security;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.api.monitoring.api_monitoring_backend.dto.ApiErrorResponse;
import com.api.monitoring.api_monitoring_backend.model.Admin;
import com.api.monitoring.api_monitoring_backend.model.UserLogin;
import com.api.monitoring.api_monitoring_backend.repository.AdminRepository;
import com.api.monitoring.api_monitoring_backend.repository.UserLoginRepository;
import com.api.monitoring.api_monitoring_backend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserLoginRepository userLoginRepository;
    private final AdminRepository adminRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(
        JwtService jwtService,
        UserLoginRepository userLoginRepository,
        AdminRepository adminRepository,
        ObjectMapper objectMapper
    ) {
        this.jwtService = jwtService;
        this.userLoginRepository = userLoginRepository;
        this.adminRepository = adminRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "JWT token is required for this request");
            return;
        }

        JwtPayload payload;
        try {
            payload = jwtService.verify(token);
        } catch (Exception ex) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid JWT token");
            return;
        }

        Optional<UserLogin> loginOpt = userLoginRepository.findByIdAndUserIdAndAccessToken(
            payload.getLoginId(), payload.getUserId(), token);
        if (loginOpt.isEmpty()) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "User and JWT combination not found");
            return;
        }

        Optional<Admin> userOpt = adminRepository.findById(payload.getUserId());
        if (userOpt.isEmpty()) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "User and JWT combination not found");
            return;
        }

        Admin admin = userOpt.get();
        request.setAttribute("authenticatedUser", admin);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            admin.getId(), null, java.util.List.of());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/login")
            || path.equals("/auth/refresh-token")
            || path.equals("/swagger-ui.html")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/v3/api-docs");
    }

    private String resolveToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken;
        }
        return null;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        ApiErrorResponse error = new ApiErrorResponse(false, message);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), error);
    }
}
