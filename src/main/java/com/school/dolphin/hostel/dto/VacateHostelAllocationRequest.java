package com.school.whale.hostel.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record VacateHostelAllocationRequest(
        LocalDate endDate,
        @NotNull Long version
) {}
