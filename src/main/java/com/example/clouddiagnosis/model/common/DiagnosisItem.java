package com.example.clouddiagnosis.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisItem {
    private String rule;
    private RiskLevel riskLevel;
    private String message;
}
