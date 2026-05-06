package com.example.clouddiagnosis.model.response;

import com.example.clouddiagnosis.model.common.CacheStatus;
import com.example.clouddiagnosis.model.common.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CdnHeaderAnalyzeResponse {
    private String url;
    private Integer statusCode;
    private CacheStatus cacheStatus;
    private RiskLevel riskLevel;
    private Map<String, String> headers;
    private List<String> diagnosis;
    private List<String> suggestions;
    private Boolean cached;
    private String cacheKey;
    private Long cacheTtlSeconds;
}
