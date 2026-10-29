package com.school.whale.attendance.dto;

import com.school.whale.attendance.entity.LessonAttendanceStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CorrectLessonAttendanceRequest(
        @NotNull LessonAttendanceStatus status,
        @Size(max = 500) String remarks) {
}