package com.example.clouddiagnosis.model.response;

import com.example.clouddiagnosis.model.common.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OriginCompareResponse {
    private HttpDiagnoseResponse cdnResult;
    private HttpDiagnoseResponse originResult;
    private RiskLevel riskLevel;
    private List<String> diagnosis;
    private List<String> suggestions;
}
