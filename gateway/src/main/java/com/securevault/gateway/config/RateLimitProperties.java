package com.securevault.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {
    private int requestsPerMinute;
    private int loginRequestsPerMinute;
}
