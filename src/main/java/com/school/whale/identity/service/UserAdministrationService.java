
package com.school.whale.identity.service;

import com.school.whale.common.exception.ResourceNotFoundException;
import com.school.whale.identity.dto.OrganizationScopeResponse;
import com.school.whale.identity.dto.CreateOrganizationScopeRequest;
import com.school.whale.identity.dto.CreateUserRequest;
import com.school.whale.identity.dto.ProvisionUserRequest;
import com.school.whale.identity.dto.RoleResponse;
import com.school.whale.identity.dto.UserOrganizationScopeDetailsResponse;
import com.school.whale.identity.dto.UserResponse;
import com.school.whale.identity.entity.OrganizationScopeType;
import com.school.whale.identity.entity.Role;
import com.school.whale.identity.entity.UserAccount;
import com.school.whale.identity.entity.UserOrganizationScope;
import com.school.whale.identity.repository.RoleRepository;
import com.school.whale.identity.repository.UserAccountRepository;
import com.school.whale.identity.repository.UserOrganizationScopeRepository;
import com.school.whale.identity.repository.UserRoleRepository;
import com.school.whale.identity.security.OrganizationScopeAuthorization;
import com.school.whale.organization.repository.CampusRepository;
import com.school.whale.organization.repository.ClusterRepository;
import com.school.whale.organization.repository.EnterpriseRepository;
import com.school.whale.organization.repository.InstitutionRepository;
import com.school.whale.organization.repository.RegionRepository;
import com.school.whale.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdministrationService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserOrganizationScopeRepository userOrganizationScopeRepository;
    private final RoleRepository roleRepository;

    private final UserRoleService userRoleService;
    private final UserService userService;
    private final UserOrganizationScopeService userOrganizationScopeService;
    private final OrganizationScopeAuthorization scopeAuthorization;

    private final EnterpriseRepository enterpriseRepository;
    private final RegionRepository regionRepository;
    private final ClusterRepository clusterRepository;
    private final InstitutionRepository institutionRepository;
    private final CampusRepository campusRepository;

    @Transactional
    public UserResponse provision(
            ProvisionUserRequest request,
            Authentication authentication
    ) {
        if (request.scopes() == null || request.scopes().isEmpty()) {
            throw new com.school.whale.common.exception.BusinessRuleViolationException(
                    "At least one organization scope is required");
        }

        // Validate all scopes before creating the account.
        java.util.Set<String> uniqueScopes = new java.util.HashSet<>();
        for (CreateOrganizationScopeRequest scope : request.scopes()) {
            if (scope.scopeType() == null || scope.scopeId() == null) {
                throw new com.school.whale.common.exception.BusinessRuleViolationException(
                        "Every scope must have a type and ID");
            }

            String key = scope.scopeType().name() + ":" + scope.scopeId();
            if (!uniqueScopes.add(key)) {
                throw new com.school.whale.common.exception.BusinessRuleViolationException(
                        "Duplicate organization scope: " + key);
            }

            assertScopeAccessible(scope.scopeType(), scope.scopeId(), authentication);
        }

        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Role not found: " + request.roleId()));

        if (!role.isActive()) {
            throw new com.school.whale.common.exception.BusinessRuleViolationException(
                    "Cannot assign an inactive role");
        }

        if ("PLATFORM_ADMIN".equals(role.getCode())
                && !SecurityUtils.isPlatformAdmin(authentication)) {
            throw new AccessDeniedException(
                    "Only a platform administrator can assign "
                            + "the PLATFORM_ADMIN role");
        }

        UserResponse created = userService.create(new CreateUserRequest(
                request.username().trim(),
                request.email().trim(),
                request.password(),
                request.firstName().trim(),
                request.lastName() == null ? null : request.lastName().trim()
        ));

        userRoleService.assignRole(created.id(), request.roleId());

        for (CreateOrganizationScopeRequest scope : request.scopes()) {
            userOrganizationScopeService.assignScope(created.id(), scope);
        }

        return created;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> directory(
            String search,
            Boolean active,
            OrganizationScopeType scopeType,
            UUID scopeId,
            Pageable pageable,
            Authentication authentication
    ) {
        if ((scopeType == null) != (scopeId == null)) {
            throw new IllegalArgumentException(
                    "scopeType and scopeId must be provided together");
        }

        if (scopeType == null) {
            // Unfiltered, platform-wide directory access is restricted
            // to explicitly authorized platform administrators.
            if (!SecurityUtils.isPlatformAdmin(authentication)) {
                throw new AccessDeniedException(
                        "A scope filter is required to view the user directory");
            }
        } else {
            assertScopeAccessible(scopeType, scopeId, authentication);
        }

        String searchTerm = search == null || search.isBlank()
                ? ""
                : search.trim();

        return userAccountRepository.searchDirectory(
                        searchTerm,
                        active,
                        scopeType,
                        scopeId,
                        pageable
                )
                .map(this::toUserResponse);
    }

    @Transactional(readOnly = true)
    public void assertUserAccessible(
            UUID userId,
            Authentication authentication
    ) {
        UserAccount user = findUser(userId);

        if (SecurityUtils.isPlatformAdmin(authentication)) {
            return;
        }

        List<UserOrganizationScope> scopes =
                userOrganizationScopeRepository.findByUser_Id(userId);

        boolean accessible = scopes.stream().anyMatch(scope ->
                canAccessScope(
                        scope.getScopeType(),
                        scope.getScopeId(),
                        authentication
                ));

        if (!accessible) {
            throw new AccessDeniedException(
                    "You do not have access to this user's account");
        }
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getUserRoles(
            UUID userId,
            Authentication authentication
    ) {
        assertUserAccessible(userId, authentication);

        return userRoleRepository.findByUser_Id(userId)
                .stream()
                .map(userRole -> toRoleResponse(userRole.getRole()))
                .toList();
    }

    @Transactional
    public void revokeRole(
            UUID userId,
            UUID roleId,
            Authentication authentication
    ) {
        assertUserAccessible(userId, authentication);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Role not found: " + roleId));

        // Prevent a caller from changing assignments to a role that
        // exceeds their own authority.
        if ("PLATFORM_ADMIN".equals(role.getCode())
                && !SecurityUtils.isPlatformAdmin(authentication)) {
            throw new AccessDeniedException(
                    "Only a platform administrator can revoke this role");
        }

        userRoleService.revokeRole(userId, roleId);
    }

    @Transactional(readOnly = true)
    public List<UserOrganizationScopeDetailsResponse> getUserScopes(
            UUID userId,
            Authentication authentication
    ) {
        assertUserAccessible(userId, authentication);

        return userOrganizationScopeRepository.findByUser_Id(userId)
                .stream()
                .map(scope -> new UserOrganizationScopeDetailsResponse(
                        scope.getId(),
                        scope.getUser().getId(),
                        scope.getScopeType(),
                        scope.getScopeId(),
                        resolveScopeName(
                                scope.getScopeType(),
                                scope.getScopeId()
                        )
                ))
                .toList();
    }

    @Transactional
    public void revokeScope(
            UUID userId,
            OrganizationScopeType scopeType,
            UUID scopeId,
            Authentication authentication
    ) {
        assertUserAccessible(userId, authentication);
        assertScopeAccessible(scopeType, scopeId, authentication);

        UserOrganizationScope scope =
                userOrganizationScopeRepository
                        .findByUser_IdAndScopeTypeAndScopeId(
                                userId, scopeType, scopeId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Organization scope is not assigned to this user"));

        userOrganizationScopeRepository.delete(scope);
    }

    @Transactional(readOnly = true)
    public void assertUserCanReceiveScope(
            UUID userId,
            OrganizationScopeType scopeType,
            UUID scopeId,
            Authentication authentication
    ) {
        findUser(userId);
        assertScopeAccessible(scopeType, scopeId, authentication);
        assertUserAccessible(userId, authentication);
    }

    private UserAccount findUser(UUID userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + userId));
    }

    private void assertScopeAccessible(
            OrganizationScopeType scopeType,
            UUID scopeId,
            Authentication authentication
    ) {
        if (!canAccessScope(scopeType, scopeId, authentication)) {
            throw new AccessDeniedException(
                    "You do not have access to the requested organization scope");
        }
    }

    private boolean canAccessScope(
            OrganizationScopeType scopeType,
            UUID scopeId,
            Authentication authentication
    ) {
        if (scopeType == null || scopeId == null) {
            return false;
        }

        return switch (scopeType) {
            case ENTERPRISE ->
                    scopeAuthorization.canAccessEnterprise(
                            authentication, scopeId);
            case REGION ->
                    scopeAuthorization.canAccessRegion(
                            authentication, scopeId);
            case CLUSTER ->
                    scopeAuthorization.canAccessCluster(
                            authentication, scopeId);
            case INSTITUTION ->
                    scopeAuthorization.canAccessInstitution(
                            authentication, scopeId);
            case CAMPUS ->
                    scopeAuthorization.canAccessCampus(
                            authentication, scopeId);
        };
    }

    private String resolveScopeName(
            OrganizationScopeType scopeType,
            UUID scopeId
    ) {
        return switch (scopeType) {
            case ENTERPRISE -> enterpriseRepository.findById(scopeId)
                    .map(entity -> entity.getName())
                    .orElse(null);

            case REGION -> regionRepository.findById(scopeId)
                    .map(entity -> entity.getName())
                    .orElse(null);

            case CLUSTER -> clusterRepository.findById(scopeId)
                    .map(entity -> entity.getName())
                    .orElse(null);

            case INSTITUTION -> institutionRepository.findById(scopeId)
                    .map(entity -> entity.getName())
                    .orElse(null);

            case CAMPUS -> campusRepository.findById(scopeId)
                    .map(entity -> entity.getName())
                    .orElse(null);
        };
    }

    private UserResponse toUserResponse(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isActive()
        );
    }

    private RoleResponse toRoleResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.isActive()
        );
    }
}
