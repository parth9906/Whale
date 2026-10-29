
package com.school.whale.attendance.dto;

import com.school.whale.attendance.entity.AttendanceStatus;

import java.time.LocalDate;
import java.util.UUID;

public record AttendanceResponse(
        UUID id,
        UUID studentId,
        UUID institutionId,
        LocalDate attendanceDate,
        AttendanceStatus status,
        String remarks
) {}