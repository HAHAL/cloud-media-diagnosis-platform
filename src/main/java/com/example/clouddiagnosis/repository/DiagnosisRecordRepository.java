package com.example.clouddiagnosis.repository;

import com.example.clouddiagnosis.entity.DiagnosisRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DiagnosisRecordRepository extends JpaRepository<DiagnosisRecord, Long>, JpaSpecificationExecutor<DiagnosisRecord> {
}
