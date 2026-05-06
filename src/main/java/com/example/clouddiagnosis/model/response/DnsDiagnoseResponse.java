package com.example.clouddiagnosis.model.response;

import com.example.clouddiagnosis.model.common.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DnsDiagnoseResponse {
    private String domain;
    private List<String> ips;
    private List<String> cnames;
    private Long resolveTimeMs;
    private Boolean cdnLike;
    private RiskLevel riskLevel;
    private List<String> diagnosis;
    private List<String> suggestions;
    private Boolean cached;
    private String cacheKey;
    private Long cacheTtlSeconds;
}
