package com.school.whale.timetable.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LessonSubstituteAuditResponse(
        UUID id,
        UUID occurrenceId,
        UUID previousSubstituteStaffAssignmentId,
        UUID newSubstituteStaffAssignmentId,
        UUID changedByUserId,
        String comment,
        OffsetDateTime changedAt) {
}