package com.api.monitoring.api_monitoring_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.api.monitoring.api_monitoring_backend.validation.ValidationPatterns;

public class LoginRequest {
    @NotBlank
    @Email
    private String emailAddress;

    @NotBlank
    @Size(min = 6)
    @Pattern(regexp = ValidationPatterns.PASSWORD)
    private String profilePassword;

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getProfilePassword() {
        return profilePassword;
    }

    public void setProfilePassword(String profilePassword) {
        this.profilePassword = profilePassword;
    }
}
