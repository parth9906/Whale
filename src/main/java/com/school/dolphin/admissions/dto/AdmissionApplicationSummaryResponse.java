package com.school.whale.admissions.dto;

public record AdmissionApplicationSummaryResponse(
        long appliedCount,
        long underReviewCount,
        long acceptedCount,
        long rejectedCount,
        long waitlistedCount,
        long enrolledCount
) {}
