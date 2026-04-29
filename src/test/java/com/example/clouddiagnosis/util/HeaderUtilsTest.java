package com.example.clouddiagnosis.util;

import com.example.clouddiagnosis.model.common.CacheStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeaderUtilsTest {

    @Test
    void shouldDetectCdnAndRange() {
        Map<String, String> headers = Map.of(
                "Via", "cache-node",
                "X-Cache", "HIT from cdn",
                "Accept-Ranges", "bytes"
        );

        assertTrue(HeaderUtils.isCdnLike(headers));
        assertTrue(HeaderUtils.isRangeSupported(headers));
        assertEquals(CacheStatus.HIT, HeaderUtils.judgeCacheStatus(headers));
    }

    @Test
    void shouldDetectBypassCache() {
        Map<String, String> headers = Map.of("Cache-Control", "private, no-store");
        assertEquals(CacheStatus.BYPASS, HeaderUtils.judgeCacheStatus(headers));
    }
}
