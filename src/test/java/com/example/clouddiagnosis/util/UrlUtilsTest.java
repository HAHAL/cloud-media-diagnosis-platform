package com.example.clouddiagnosis.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlUtilsTest {

    @Test
    void shouldValidateHttpUrl() {
        assertTrue(UrlUtils.isValidHttpUrl("https://example.com/video/test.mp4"));
        assertTrue(UrlUtils.isValidHttpUrl("http://www.example.com/index.html"));
        assertFalse(UrlUtils.isValidHttpUrl("ftp://example.com/file"));
        assertFalse(UrlUtils.isValidHttpUrl("not-a-url"));
    }

    @Test
    void shouldValidateDomainAndDetectType() {
        assertTrue(UrlUtils.isValidDomain("www.example.com"));
        assertFalse(UrlUtils.isValidDomain("http://www.example.com"));
        assertEquals("mp4", UrlUtils.detectResourceType("https://example.com/a.mp4", "video/mp4"));
        assertEquals("m3u8", UrlUtils.detectResourceType("https://example.com/live.m3u8", "application/vnd.apple.mpegurl"));
    }
}
