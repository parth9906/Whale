package com.school.whale.academic.dto;

import com.school.whale.academic.entity.StudentPromotionDecision;

import java.util.UUID;

public record BulkStudentPromotionDecisionResponse(
        UUID batchId,
        UUID sourceSectionId,
        UUID targetSectionId,
        StudentPromotionDecision decision,
        long updatedCount,
        long skippedInactiveCount,
        long alreadyDecidedCount
) {
}