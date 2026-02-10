package com.api.monitoring.api_monitoring_backend.validation;

public final class ValidationPatterns {
    private ValidationPatterns() {}

    public static final String PASSWORD = "^(?=.*[0-9])(?=.*[!@#$%^&*])(?=.*[a-zA-Z]).+$";
    public static final String URL = "^(http|https|ftp|tcp)://[^ \"\\\\]+$";
    public static final String OBJECT_ID = "^([0-9a-fA-F]){24}$";
    public static final String NAME = "^[a-zA-Z-_\\.][a-zA-Z-_ \\.]*[a-zA-Z-_\\.]$";
}
