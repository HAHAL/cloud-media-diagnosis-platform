package com.example.clouddiagnosis.controller;

import com.example.clouddiagnosis.exception.GlobalExceptionHandler;
import com.example.clouddiagnosis.model.common.RiskLevel;
import com.example.clouddiagnosis.model.request.HttpDiagnoseRequest;
import com.example.clouddiagnosis.model.response.DiagnosisRecordResponse;
import com.example.clouddiagnosis.model.response.HttpDiagnoseResponse;
import com.example.clouddiagnosis.repository.DiagnosisRecordRepository;
import com.example.clouddiagnosis.service.DiagnosisRecordService;
import com.example.clouddiagnosis.service.HttpDiagnoseService;
import com.example.clouddiagnosis.service.StatusCodeDiagnoseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiagnoseControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HttpDiagnoseService fakeHttpService = new HttpDiagnoseService(HttpClient.newHttpClient(), null) {
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
                        .cached(false)
                        .build();
            }
        };
        DiagnosisRecordService fakeRecordService = new DiagnosisRecordService((DiagnosisRecordRepository) null, new ObjectMapper()) {
            @Override
            public Page<DiagnosisRecordResponse> search(String url, Integer statusCode, String riskLevel,
                                                        java.time.LocalDateTime startTime, java.time.LocalDateTime endTime,
                                                        int page, int size) {
                return new PageImpl<>(List.of(DiagnosisRecordResponse.builder()
                        .id(1L)
                        .requestUrl("https://example.com")
                        .diagnosisType("FULL")
                        .statusCode(200)
                        .riskLevel("LOW")
                        .summary("ok")
                        .build()));
            }
        };
        DiagnoseController controller = new DiagnoseController(fakeHttpService, null, null, null, null, null,
                new StatusCodeDiagnoseService(), fakeRecordService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper().findAndRegisterModules()))
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

    @Test
    void shouldValidateHttpRequest() throws Exception {
        mockMvc.perform(post("/api/diagnose/http")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldDiagnoseStatusCode() throws Exception {
        mockMvc.perform(post("/api/diagnose/status-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statusCode\":504,\"errorMessage\":\"upstream timed out\",\"path\":\"/api/order/list\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("GATEWAY_TIMEOUT"));
    }

    @Test
    void shouldQueryHistory() throws Exception {
        mockMvc.perform(get("/api/diagnose/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }
}
