
package com.school.whale.guardian.dto;

import java.util.UUID;

public record GuardianResponse(
        UUID id,
        UUID institutionId,
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean active
) {}