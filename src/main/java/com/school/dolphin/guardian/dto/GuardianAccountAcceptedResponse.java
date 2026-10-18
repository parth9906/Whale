
package com.school.whale.guardian.dto;

import java.util.UUID;

public record GuardianAccountAcceptedResponse(
        UUID userId,
        String username,
        String message
) {}