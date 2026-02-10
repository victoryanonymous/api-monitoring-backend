package com.api.monitoring.api_monitoring_backend.util;

import java.util.List;

public final class ApiConstants {
    private ApiConstants() {}

    public static final int DEFAULT_SKIP = 0;
    public static final int DEFAULT_LIMIT = 10;

    public static final List<String> VALID_SORT_FIELDS = List.of(
        "created_at",
        "updated_at",
        "status",
        "type",
        "api_link",
        "api_name",
        "downtime_count",
        "last_uptime",
        "last_downtime"
    );

    public static final List<String> VALID_API_TYPES = List.of(
        "production",
        "staging",
        "development",
        "cloudflare",
        "token_balance"
    );

    public static final List<String> VALID_API_TYPES_CREATE = List.of(
        "production",
        "staging",
        "development",
        "cloudflare"
    );

    public static final List<String> VALID_STATUS = List.of("active", "inactive");
}
