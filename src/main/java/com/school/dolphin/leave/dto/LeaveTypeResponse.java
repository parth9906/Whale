package com.school.whale.leave.dto;

import java.util.UUID;

public record LeaveTypeResponse(
        UUID id,
        String code,
        String name
) {
}