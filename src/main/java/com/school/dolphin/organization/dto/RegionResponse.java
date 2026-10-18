package com.school.whale.organization.dto;

import java.util.UUID;

public record RegionResponse(
        UUID id,
        UUID enterpriseId,
        String code,
        String name,
        boolean active
) {
}