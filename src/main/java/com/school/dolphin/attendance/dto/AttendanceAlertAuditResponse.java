package com.school.whale.attendance.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AttendanceAlertAuditResponse(
        UUID id,
        String previousStatus,
        String newStatus,
        UUID changedByUserId,
        String changedByUsername,
        String comment,
        OffsetDateTime changedAt
) {
}