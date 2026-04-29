package com.example.clouddiagnosis.util;

import com.example.clouddiagnosis.model.common.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnoseRuleUtilsTest {

    @Test
    void shouldJudgeRiskByStatus() {
        assertEquals(RiskLevel.LOW, DiagnoseRuleUtils.riskByStatus(200));
        assertEquals(RiskLevel.MEDIUM, DiagnoseRuleUtils.riskByStatus(302));
        assertEquals(RiskLevel.HIGH, DiagnoseRuleUtils.riskByStatus(502));
    }

    @Test
    void shouldAppendStatusDiagnosis() {
        List<String> diagnosis = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        DiagnoseRuleUtils.appendStatusDiagnosis(403, diagnosis, suggestions);

        assertTrue(diagnosis.stream().anyMatch(item -> item.contains("403")));
        assertTrue(suggestions.stream().anyMatch(item -> item.contains("防盗链")));
    }
}
