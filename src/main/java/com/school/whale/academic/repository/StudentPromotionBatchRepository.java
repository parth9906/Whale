package com.school.whale.academic.repository;

import com.school.whale.academic.entity.PromotionBatchStatus;
import com.school.whale.academic.entity.StudentPromotionBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentPromotionBatchRepository
        extends JpaRepository<StudentPromotionBatch, UUID> {

    List<StudentPromotionBatch> findByInstitution_IdOrderByCreatedAtDesc(
            UUID institutionId);

    boolean existsBySourceAcademicYear_IdAndTargetAcademicYear_IdAndStatus(
            UUID sourceAcademicYearId,
            UUID targetAcademicYearId,
            PromotionBatchStatus status);
}