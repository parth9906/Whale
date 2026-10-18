package com.school.whale.leave.dto;


import com.school.whale.leave.entity.LeaveRequestStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StaffLeaveResponse(
        UUID id,
        UUID staffMemberId,
        UUID institutionId,
        UUID leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        LeaveRequestStatus status,
        UUID reviewedByUserId,
        OffsetDateTime reviewedAt,
        String decisionComment,
        OffsetDateTime createdAt
) {}
