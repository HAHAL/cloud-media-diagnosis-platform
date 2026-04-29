package com.example.clouddiagnosis.controller;

import com.example.clouddiagnosis.exception.GlobalExceptionHandler;
import com.example.clouddiagnosis.model.common.RiskLevel;
import com.example.clouddiagnosis.model.request.HttpDiagnoseRequest;
import com.example.clouddiagnosis.model.response.HttpDiagnoseResponse;
import com.example.clouddiagnosis.service.HttpDiagnoseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiagnoseControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HttpDiagnoseService fakeHttpService = new HttpDiagnoseService(HttpClient.newHttpClient()) {
            @Override
            public HttpDiagnoseResponse diagnose(HttpDiagnoseRequest request) {
                return HttpDiagnoseResponse.builder()
                        .url(request.getUrl())
                        .statusCode(200)
                        .responseTimeMs(100L)
                        .redirect(false)
                        .cdnLike(true)
                        .rangeSupported(true)
                        .headers(Map.of("X-Cache", "HIT"))
                        .riskLevel(RiskLevel.LOW)
                        .diagnosis(List.of("HTTP 状态码正常"))
                        .suggestions(List.of("继续检查缓存命中率"))
                        .build();
            }
        };
        DiagnoseController controller = new DiagnoseController(fakeHttpService, null, null, null, null, null);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnHttpDiagnosis() throws Exception {
        mockMvc.perform(post("/api/diagnose/http")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com",
                                  "timeoutMs": 5000,
                                  "followRedirect": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.statusCode").value(200))
                .andExpect(jsonPath("$.data.cdnLike").value(true));
    }
}
