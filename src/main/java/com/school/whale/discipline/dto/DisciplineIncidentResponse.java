package com.school.whale.discipline.dto;

import com.school.whale.discipline.entity.DisciplineCategory;
import com.school.whale.discipline.entity.DisciplineIncidentStatus;
import com.school.whale.discipline.entity.DisciplineSeverity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DisciplineIncidentResponse(
        UUID id,
        UUID institutionId,
        UUID campusId,
        UUID studentId,
        String admissionNumber,
        DisciplineCategory category,
        DisciplineSeverity severity,
        String description,
        LocalDate incidentDate,
        DisciplineIncidentStatus status,
        UUID reportedByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
