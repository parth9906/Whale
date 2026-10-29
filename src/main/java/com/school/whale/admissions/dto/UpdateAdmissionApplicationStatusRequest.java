package com.school.whale.admissions.dto;

import com.school.whale.admissions.entity.AdmissionApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAdmissionApplicationStatusRequest(
        @NotNull AdmissionApplicationStatus status
) {}
