
package com.school.whale.identity.dto;

import com.school.whale.identity.entity.OrganizationScopeType;

import java.util.UUID;

public record UserOrganizationScopeDetailsResponse(
        UUID id,
        UUID userId,
        OrganizationScopeType scopeType,
        UUID scopeId,
        String scopeName
) {
}
