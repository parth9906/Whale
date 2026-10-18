package com.school.whale.organization.dto;

import java.util.UUID;

public record EnterpriseResponse(
        UUID id,
        String code,
        String name,
        boolean active
) {
}