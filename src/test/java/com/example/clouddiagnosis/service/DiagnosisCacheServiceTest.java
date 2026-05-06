package com.example.clouddiagnosis.service;

import com.example.clouddiagnosis.config.DiagnosisCacheProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosisCacheServiceTest {

    @Test
    void shouldFallbackWhenRedisUnavailable() {
        DiagnosisCacheProperties properties = new DiagnosisCacheProperties();
        ObjectProvider<StringRedisTemplate> provider = new ObjectProvider<>() {
            @Override
            public StringRedisTemplate getObject(Object... args) {
                throw new IllegalStateException("redis unavailable");
            }

            @Override
            public StringRedisTemplate getIfAvailable() {
                return null;
            }

            @Override
            public StringRedisTemplate getIfUnique() {
                return null;
            }

            @Override
            public StringRedisTemplate getObject() {
                throw new IllegalStateException("redis unavailable");
            }
        };
        DiagnosisCacheService service = new DiagnosisCacheService(provider, properties, new ObjectMapper());

        assertTrue(service.get("diagnosis:http:test", Object.class).isEmpty());
        assertDoesNotThrow(() -> service.put("diagnosis:http:test", java.util.Map.of("ok", true)));
    }
}
