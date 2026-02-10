package com.api.monitoring.api_monitoring_backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.api.monitoring.api_monitoring_backend.model.Admin;
import com.api.monitoring.api_monitoring_backend.repository.AdminRepository;

@Component
public class AdminSeeder implements CommandLineRunner {
    private final AppProperties appProperties;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(
        AppProperties appProperties,
        AdminRepository adminRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.appProperties = appProperties;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!appProperties.getSeedAdmin().isEnabled()) {
            return;
        }
        String email = appProperties.getSeedAdmin().getEmail();
        String password = appProperties.getSeedAdmin().getPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        adminRepository.findByEmailAddress(email.toLowerCase()).ifPresentOrElse(
            existing -> {},
            () -> {
                Admin admin = new Admin();
                admin.setName(appProperties.getSeedAdmin().getName());
                admin.setEmailAddress(email.toLowerCase());
                admin.setProfilePassword(passwordEncoder.encode(password));
                adminRepository.save(admin);
            }
        );
    }
}
