package com.example.clouddiagnosis.service;

import com.example.clouddiagnosis.model.request.StatusCodeDiagnoseRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusCodeDiagnoseServiceTest {

    private final StatusCodeDiagnoseService service = new StatusCodeDiagnoseService();

    @Test
    void shouldCoverCommonWebStatusCodes() {
        int[] codes = {400, 401, 403, 404, 408, 429, 500, 502, 503, 504};
        for (int code : codes) {
            StatusCodeDiagnoseRequest request = new StatusCodeDiagnoseRequest();
            request.setStatusCode(code);
            var response = service.diagnose(request);
            assertEquals(code, response.getStatusCode());
            assertTrue(response.getPossibleCauses().size() >= 3);
            assertTrue(response.getTroubleshootingSteps().size() >= 3);
        }
    }
}
