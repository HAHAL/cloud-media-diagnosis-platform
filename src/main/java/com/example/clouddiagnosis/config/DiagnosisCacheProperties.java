package com.example.clouddiagnosis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "diagnosis.cache")
public class DiagnosisCacheProperties {
    private boolean enabled = true;
    private long ttlSeconds = 300;
}
