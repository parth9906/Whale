package com.school.whale.staff.dto;

import com.school.whale.staff.entity.StaffAssignmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStaffAssignmentStatusRequest(
        @NotNull StaffAssignmentStatus status
) {}