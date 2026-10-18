package com.school.whale.discipline.dto;

import com.school.whale.discipline.entity.DisciplineIncidentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDisciplineIncidentStatusRequest(
        @NotNull DisciplineIncidentStatus status
) {}
