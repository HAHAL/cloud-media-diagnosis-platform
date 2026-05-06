package com.example.clouddiagnosis.service;

import com.example.clouddiagnosis.entity.DiagnosisRecord;
import com.example.clouddiagnosis.exception.BizException;
import com.example.clouddiagnosis.model.response.DiagnosisRecordResponse;
import com.example.clouddiagnosis.model.response.FullDiagnoseResponse;
import com.example.clouddiagnosis.repository.DiagnosisRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisRecordService {
    private final DiagnosisRecordRepository repository;
    private final ObjectMapper objectMapper;

    public Long saveFullDiagnosis(FullDiagnoseResponse response) {
        try {
            DiagnosisRecord record = new DiagnosisRecord();
            record.setRequestUrl(response.getUrl());
            record.setDiagnosisType("FULL");
            record.setStatusCode(response.getHttp() == null ? null : response.getHttp().getStatusCode());
            record.setRiskLevel(response.getOverallRiskLevel() == null ? null : response.getOverallRiskLevel().name());
            record.setSummary(response.getSummary());
            record.setSuggestions(String.join("\n", response.getNextActions()));
            record.setRawResultJson(objectMapper.writeValueAsString(response));
            return repository.save(record).getId();
        } catch (Exception ex) {
            log.warn("Save diagnosis record failed. url={}, reason={}", response.getUrl(), ex.getMessage());
            throw new BizException(503, "诊断历史功能依赖数据库，请确认数据库服务已启动：" + ex.getMessage());
        }
    }

    public Page<DiagnosisRecordResponse> search(String url, Integer statusCode, String riskLevel,
                                                LocalDateTime startTime, LocalDateTime endTime,
                                                int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findAll(buildSpec(url, statusCode, riskLevel, startTime, endTime), pageable)
                .map(this::toResponse);
    }

    public DiagnosisRecordResponse get(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BizException(404, "诊断记录不存在"));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new BizException(404, "诊断记录不存在");
        }
        repository.deleteById(id);
    }

    private Specification<DiagnosisRecord> buildSpec(String url, Integer statusCode, String riskLevel,
                                                     LocalDateTime startTime, LocalDateTime endTime) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (url != null && !url.isBlank()) {
                predicates.add(cb.like(root.get("requestUrl"), "%" + url + "%"));
            }
            if (statusCode != null) {
                predicates.add(cb.equal(root.get("statusCode"), statusCode));
            }
            if (riskLevel != null && !riskLevel.isBlank()) {
                predicates.add(cb.equal(root.get("riskLevel"), riskLevel.toUpperCase()));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endTime));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private DiagnosisRecordResponse toResponse(DiagnosisRecord record) {
        return DiagnosisRecordResponse.builder()
                .id(record.getId())
                .requestUrl(record.getRequestUrl())
                .diagnosisType(record.getDiagnosisType())
                .statusCode(record.getStatusCode())
                .riskLevel(record.getRiskLevel())
                .summary(record.getSummary())
                .suggestions(record.getSuggestions())
                .rawResultJson(record.getRawResultJson())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}
