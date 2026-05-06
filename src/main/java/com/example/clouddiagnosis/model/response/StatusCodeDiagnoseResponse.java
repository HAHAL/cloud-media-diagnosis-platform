package com.example.clouddiagnosis.model.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StatusCodeDiagnoseResponse {
    private Integer statusCode;
    private String category;
    private String severity;
    private String summary;
    private List<String> possibleCauses;
    private List<String> troubleshootingSteps;
    private List<String> needMoreInfo;
}
