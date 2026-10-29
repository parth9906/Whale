package com.school.whale.attendance.dto;

import java.util.UUID;

import com.school.whale.attendance.entity.LessonAttendanceStatus;

public record LessonAttendanceRosterItemResponse(
        UUID studentId,
        String studentName,
        UUID attendanceRecordId,
        LessonAttendanceStatus status,
        String remarks) {
}