package com.school.whale.admissions.dto;

import com.school.whale.admissions.entity.AdmissionApplicationStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdmissionApplicationResponse(
        UUID id,
        UUID institutionId,
        UUID campusId,
        UUID applyingClassId,
        String className,
        String classCode,
        String applicantFirstName,
        String applicantLastName,
        LocalDate applicantDateOfBirth,
        String parentName,
        String parentPhone,
        String parentEmail,
        String notes,
        LocalDate applicationDate,
        AdmissionApplicationStatus status,
        UUID enrolledStudentId,
        UUID createdByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
