package com.example.clouddiagnosis.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cloudDiagnosisOpenApi() {
        return new OpenAPI().info(new Info()
                .title("云产品网络诊断与排障平台 API")
                .description("面向 CDN、DNS、源站和视频资源访问问题的轻量级诊断接口")
                .version("v1.0.0"));
    }
}
