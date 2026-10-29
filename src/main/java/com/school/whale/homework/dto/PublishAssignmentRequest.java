package com.school.whale.homework.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PublishAssignmentRequest(
        @NotNull
        @PositiveOrZero
        Long version
) {
}
