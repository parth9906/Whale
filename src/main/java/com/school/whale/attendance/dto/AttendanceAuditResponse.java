
package com.school.whale.attendance.dto;

import com.school.whale.attendance.entity.AttendanceAuditAction;
import com.school.whale.attendance.entity.AttendanceStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AttendanceAuditResponse(
        UUID id,
        UUID attendanceRecordId,
        AttendanceAuditAction action,
        AttendanceStatus previousStatus,
        AttendanceStatus newStatus,
        String previousRemarks,
        String newRemarks,
        UUID changedByUserId,
        OffsetDateTime changedAt
) {}