package com.example.clouddiagnosis.model.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DiagnosisRecordResponse {
    private Long id;
    private String requestUrl;
    private String diagnosisType;
    private Integer statusCode;
    private String riskLevel;
    private String summary;
    private String suggestions;
    private String rawResultJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
