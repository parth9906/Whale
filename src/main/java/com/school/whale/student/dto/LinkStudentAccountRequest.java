package com.school.whale.student.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LinkStudentAccountRequest(
        @NotNull UUID userAccountId
) {
}
