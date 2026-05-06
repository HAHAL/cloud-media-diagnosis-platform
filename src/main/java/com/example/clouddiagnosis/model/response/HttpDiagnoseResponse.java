package com.example.clouddiagnosis.model.response;

import com.example.clouddiagnosis.model.common.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class HttpDiagnoseResponse {
    private String url;
    private Integer statusCode;
    private Long responseTimeMs;
    private Boolean redirect;
    private Boolean cdnLike;
    private Boolean rangeSupported;
    private Map<String, String> headers;
    private RiskLevel riskLevel;
    private List<String> diagnosis;
    private List<String> suggestions;
    private String errorMessage;
    private Boolean cached;
    private String cacheKey;
    private Long cacheTtlSeconds;
}
