package com.example.clouddiagnosis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "diagnosis_record", indexes = {
        @Index(name = "idx_diagnosis_status_code", columnList = "status_code"),
        @Index(name = "idx_diagnosis_risk_level", columnList = "risk_level"),
        @Index(name = "idx_diagnosis_created_at", columnList = "created_at"),
        @Index(name = "idx_diagnosis_request_url", columnList = "request_url")
})
public class DiagnosisRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_url", length = 512, nullable = false)
    private String requestUrl;

    @Column(name = "diagnosis_type", length = 64, nullable = false)
    private String diagnosisType;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "risk_level", length = 32)
    private String riskLevel;

    @Column(name = "summary", length = 1024)
    private String summary;

    @Lob
    @Column(name = "suggestions")
    private String suggestions;

    @Lob
    @Column(name = "raw_result_json")
    private String rawResultJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
