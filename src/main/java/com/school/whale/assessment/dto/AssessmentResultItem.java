package com.school.whale.assessment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AssessmentResultItem(
        UUID assessmentId,
        String assessmentName,
        String subjectName,
        LocalDate assessmentDate,
        BigDecimal marksObtained,
        BigDecimal maximumMarks,
        BigDecimal percentage,
        String grade
) {}