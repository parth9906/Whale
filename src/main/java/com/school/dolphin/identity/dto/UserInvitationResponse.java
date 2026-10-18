
package com.school.dolphin.identity.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UserInvitationResponse(
        UUID invitationId,
        String email,
        String username,
        String roleCode,
        String roleName,
        String scopeType,
        UUID scopeId,
        OffsetDateTime expiresAt,
        String status,
        List<InvitationScopeResponse> scopes
) {
    public record InvitationScopeResponse(
            String scopeType,
            UUID scopeId
    ) {
    }
}
