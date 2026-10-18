
package com.school.whale.identity.dto;

import java.util.UUID;

public record AcceptUserInvitationResponse(
        UUID userId,
        String username,
        String message
) {
}
