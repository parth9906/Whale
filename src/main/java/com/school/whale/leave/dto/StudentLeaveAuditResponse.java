package com.school.whale.leave.dto;

import com.school.whale.leave.entity.LeaveRequestStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentLeaveAuditResponse(
        UUID id,
        LeaveRequestStatus previousStatus,
        LeaveRequestStatus newStatus,
        UUID changedByUserId,
        String comment,
        OffsetDateTime changedAt
) {}
