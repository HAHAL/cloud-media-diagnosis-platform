package com.example.clouddiagnosis.service;

import com.example.clouddiagnosis.config.DiagnosisCacheProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisCacheService {
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final DiagnosisCacheProperties properties;
    private final ObjectMapper objectMapper;

    public <T> Optional<T> get(String key, Class<T> type) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        try {
            String value = redisTemplateProvider.getObject().opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, type));
        } catch (Exception ex) {
            log.warn("Redis cache read failed, fallback to direct diagnosis. key={}, reason={}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    public void put(String key, Object value) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            redisTemplateProvider.getObject().opsForValue().set(key, objectMapper.writeValueAsString(value),
                    Duration.ofSeconds(properties.getTtlSeconds()));
        } catch (Exception ex) {
            log.warn("Redis cache write failed, diagnosis result is still returned. key={}, reason={}", key, ex.getMessage());
        }
    }

    public long ttlSeconds() {
        return properties.getTtlSeconds();
    }

    public static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).substring(0, 24);
        } catch (Exception ex) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
