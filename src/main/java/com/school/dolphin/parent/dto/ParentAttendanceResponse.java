
package com.school.whale.parent.dto;

import com.school.whale.attendance.entity.AttendanceStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ParentAttendanceResponse(
        UUID studentId,
        LocalDate attendanceDate,
        AttendanceStatus status,
        String remarks
) {}