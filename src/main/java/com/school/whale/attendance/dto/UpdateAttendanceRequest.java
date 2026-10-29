
package com.school.whale.attendance.dto;

import com.school.whale.attendance.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAttendanceRequest(
        @NotNull AttendanceStatus status,
        @Size(max = 500) String remarks
) {}