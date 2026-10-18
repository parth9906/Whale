
package com.school.dolphin.identity.service;

import com.school.dolphin.common.exception.BusinessRuleViolationException;
import com.school.dolphin.common.exception.DuplicateResourceException;
import com.school.dolphin.common.exception.ResourceNotFoundException;
import com.school.dolphin.guardian.service.InvitationTokenService;
import com.school.dolphin.identity.dto.AcceptUserInvitationRequest;
import com.school.dolphin.identity.dto.AcceptUserInvitationResponse;
import com.school.dolphin.identity.dto.CreateOrganizationScopeRequest;
import com.school.dolphin.identity.dto.CreateUserInvitationRequest;
import com.school.dolphin.identity.dto.CreateUserInvitationRequest.InvitationScopeRequest;
import com.school.dolphin.identity.dto.UserInvitationResponse;
import com.school.dolphin.identity.dto.UserInvitationResponse.InvitationScopeResponse;
import com.school.dolphin.identity.entity.OrganizationScopeType;
import com.school.dolphin.identity.entity.Role;
import com.school.dolphin.identity.entity.UserAccount;
import com.school.dolphin.identity.entity.UserInvitation;
import com.school.dolphin.identity.entity.UserInvitationScope;
import com.school.dolphin.identity.repository.RoleRepository;
import com.school.dolphin.identity.repository.UserAccountRepository;
import com.school.dolphin.identity.repository.UserInvitationRepository;
import com.school.dolphin.identity.security.AuthenticatedUser;
import com.school.dolphin.identity.security.OrganizationScopeAuthorization;
import com.school.dolphin.organization.repository.CampusRepository;
import com.school.dolphin.organization.repository.ClusterRepository;
import com.school.dolphin.organization.repository.EnterpriseRepository;
import com.school.dolphin.organization.repository.InstitutionRepository;
import com.school.dolphin.organization.repository.RegionRepository;
import com.school.dolphin.security.SecurityUtils;
import com.school.dolphin.staff.entity.StaffMember;
import com.school.dolphin.staff.repository.StaffMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserInvitationService {

    private final UserInvitationRepository invitationRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;

    private final InvitationTokenService tokenService;
    private final InvitationEmailService invitationEmailService;
    private final PasswordEncoder passwordEncoder;

    private final UserRoleService userRoleService;
    private final UserOrganizationScopeService userOrganizationScopeService;
    private final OrganizationScopeAuthorization scopeAuthorization;

    private final EnterpriseRepository enterpriseRepository;
    private final RegionRepository regionRepository;
    private final ClusterRepository clusterRepository;
    private final InstitutionRepository institutionRepository;
    private final CampusRepository campusRepository;
    private final StaffMemberRepository staffMemberRepository;

    @Value("${app.invitation.email.validity-hours:24}")
    private long invitationValidityHours;

    @Transactional
    public UserInvitationResponse createInvitation(
            CreateUserInvitationRequest request,
            Authentication authentication
    ) {
        UUID creatorId = getAuthenticatedUserId(authentication);

        UserAccount creator = userAccountRepository.findById(creatorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inviting user not found: " + creatorId
                        )
                );

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String username = request.username().trim();

        validateNoExistingAccountOrInvitation(email, username);

        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Role not found: " + request.roleId()
                        )
                );

        if (!role.isActive()) {
            throw new BusinessRuleViolationException(
                    "Cannot invite a user with an inactive role"
            );
        }

        validateRoleAssignable(role, authentication);

        List<InvitationScopeRequest> requestedScopes =
                resolveRequestedScopes(request);

        for (InvitationScopeRequest scope : requestedScopes) {
            validateScopeExistsAndActive(
                    scope.scopeType(),
                    scope.scopeId()
            );

            if (!canAccessScope(
                    authentication,
                    scope.scopeType(),
                    scope.scopeId()
            )) {
                throw new AccessDeniedException(
                        "You are not authorized to invite users into scope "
                                + scope.scopeType() + ":" + scope.scopeId()
                );
            }
        }

        // Keep the existing columns populated for legacy consumers.
        InvitationScopeRequest primaryScope = requestedScopes.get(0);

        OffsetDateTime now = OffsetDateTime.now();
        String rawToken = tokenService.generateToken();
        String tokenHash = tokenService.hashToken(rawToken);
        OffsetDateTime expiresAt =
                now.plusHours(invitationValidityHours);

        UserInvitation invitation = UserInvitation.create(
                email,
                username,
                request.firstName().trim(),
                request.lastName() == null
                        ? null
                        : request.lastName().trim(),
                tokenHash,
                role,
                primaryScope.scopeType(),
                primaryScope.scopeId(),
                expiresAt,
                creator
        );

        requestedScopes.forEach(scope ->
                invitation.addScope(
                        scope.scopeType(),
                        scope.scopeId()
                )
        );

        invitationRepository.save(invitation);

        String inviterName = (
                creator.getFirstName() + " " +
                        (creator.getLastName() == null
                                ? ""
                                : creator.getLastName())
        ).trim();

        // Keep the existing delivery behavior: failure rolls back creation.
        // Never return or log the raw invitation token.
        invitationEmailService.sendInvitation(
                email,
                invitation.getFirstName(),
                inviterName,
                role.getName(),
                primaryScope.scopeType().name(),
                rawToken
        );

        return toResponse(invitation, "PENDING");
    }

    @Transactional
    public UserInvitationResponse createTeacherInvitation(
            CreateUserInvitationRequest request,
            Authentication authentication,
            StaffMember staffMember
    ) {
        if (staffMember == null || staffMember.getId() == null) {
            throw new IllegalArgumentException(
                    "A persisted staff profile is required"
            );
        }

        invitationEmailService.ensureEmailDeliveryEnabled();

        UUID creatorId = getAuthenticatedUserId(authentication);

        UserAccount creator = userAccountRepository.findById(creatorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inviting user not found: " + creatorId
                        )
                );

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String username = request.username().trim();

        validateNoExistingAccountOrInvitation(email, username);

        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Role not found: " + request.roleId()
                        )
                );

        if (!role.isActive() || !"TEACHER".equals(role.getCode())) {
            throw new BusinessRuleViolationException(
                    "Teacher onboarding requires the active TEACHER role"
            );
        }

        if (request.scopeType() != OrganizationScopeType.INSTITUTION
                || request.scopeId() == null) {
            throw new BusinessRuleViolationException(
                    "Teacher invitation must use institution scope"
            );
        }

        validateScopeExistsAndActive(
                request.scopeType(),
                request.scopeId()
        );

        if (!canAccessScope(
                authentication,
                request.scopeType(),
                request.scopeId()
        )) {
            throw new AccessDeniedException(
                    "You are not authorized to invite a teacher "
                            + "into this institution"
            );
        }

        if (staffMember.getUserAccount() != null) {
            throw new BusinessRuleViolationException(
                    "Staff profile is already linked to an account"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();
        String rawToken = tokenService.generateToken();
        String tokenHash = tokenService.hashToken(rawToken);
        OffsetDateTime expiresAt =
                now.plusHours(invitationValidityHours);

        UserInvitation invitation = UserInvitation.create(
                email,
                username,
                request.firstName().trim(),
                request.lastName() == null
                        ? null
                        : request.lastName().trim(),
                tokenHash,
                role,
                request.scopeType(),
                request.scopeId(),
                expiresAt,
                creator
        );

        // Teacher invitations remain single-institution invitations.
        invitation.addScope(
                request.scopeType(),
                request.scopeId()
        );

        invitation.linkStaffMember(staffMember);
        invitationRepository.save(invitation);

        String inviterName = (
                creator.getFirstName() + " " +
                        (creator.getLastName() == null
                                ? ""
                                : creator.getLastName())
        ).trim();

        // The enclosing onboarding transaction must commit before email.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        invitationEmailService.sendInvitation(
                                email,
                                invitation.getFirstName(),
                                inviterName,
                                role.getName(),
                                request.scopeType().name(),
                                rawToken
                        );
                    }
                }
        );

        return toResponse(invitation, "PENDING");
    }

    @Transactional
    public AcceptUserInvitationResponse acceptInvitation(
            AcceptUserInvitationRequest request
    ) {
        String tokenHash = tokenService.hashToken(request.token());

        UserInvitation invitation = invitationRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() ->
                        new BusinessRuleViolationException(
                                "Invitation is invalid or no longer available"
                        )
                );

        OffsetDateTime now = OffsetDateTime.now();

        if (!invitation.isPendingAt(now)) {
            throw new BusinessRuleViolationException(
                    "Invitation has expired, been revoked, "
                            + "or already been accepted"
            );
        }

        String email = invitation.getInvitationEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userAccountRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new DuplicateResourceException(
                    "An account already exists for this email"
            );
        }

        if (userAccountRepository.existsByUsername(
                invitation.getUsername()
        )) {
            throw new DuplicateResourceException(
                    "Username is already in use"
            );
        }

        UserAccount user = UserAccount.builder()
                .id(UUID.randomUUID())
                .username(invitation.getUsername())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(invitation.getFirstName())
                .lastName(invitation.getLastName())
                .active(true)
                .build();

        userAccountRepository.save(user);

        userRoleService.assignRole(
                user.getId(),
                invitation.getRole().getId()
        );

        List<UserInvitationScope> scopes = invitation.getScopes();

        if (scopes.isEmpty()) {
            // Backward compatibility for old invitations.
            userOrganizationScopeService.assignScope(
                    user.getId(),
                    new CreateOrganizationScopeRequest(
                            invitation.getScopeType(),
                            invitation.getScopeId()
                    )
            );
        } else {
            for (UserInvitationScope scope : scopes) {
                userOrganizationScopeService.assignScope(
                        user.getId(),
                        new CreateOrganizationScopeRequest(
                                scope.getScopeType(),
                                scope.getScopeId()
                        )
                );
            }
        }

        if (invitation.getStaffMember() != null) {
            StaffMember staffMember = staffMemberRepository
                    .findByIdAndUserAccountIsNull(
                            invitation.getStaffMember().getId()
                    )
                    .orElseThrow(() ->
                            new BusinessRuleViolationException(
                                    "The staff profile is already linked "
                                            + "to an account"
                            )
                    );

            staffMember.setUserAccount(user);
            staffMemberRepository.save(staffMember);
        }

        invitation.markAccepted(now);

        return new AcceptUserInvitationResponse(
                user.getId(),
                user.getUsername(),
                "Invitation accepted successfully"
        );
    }

    private void validateNoExistingAccountOrInvitation(
            String email,
            String username
    ) {
        if (userAccountRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new DuplicateResourceException(
                    "An account already exists for this email"
            );
        }

        if (userAccountRepository.existsByUsername(username)) {
            throw new DuplicateResourceException(
                    "Username is already in use"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (invitationRepository
                .existsByUsernameAndAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(
                        username,
                        now
                )) {
            throw new DuplicateResourceException(
                    "A pending invitation already exists for this username"
            );
        }

        boolean emailHasPendingInvitation = invitationRepository
                .findByInvitationEmailIgnoreCaseAndAcceptedAtIsNullAndRevokedAtIsNull(
                        email
                )
                .stream()
                .anyMatch(invitation -> invitation.isPendingAt(now));

        if (emailHasPendingInvitation) {
            throw new DuplicateResourceException(
                    "A pending invitation already exists for this email"
            );
        }
    }

    private List<InvitationScopeRequest> resolveRequestedScopes(
            CreateUserInvitationRequest request
    ) {
        boolean hasLegacyScope =
                request.scopeType() != null || request.scopeId() != null;

        boolean hasNewScopes =
                request.scopes() != null && !request.scopes().isEmpty();

        if (hasLegacyScope && hasNewScopes) {
            throw new BusinessRuleViolationException(
                    "Provide either scopes or legacy scopeType/scopeId, "
                            + "not both"
            );
        }

        if (hasLegacyScope) {
            if (request.scopeType() == null || request.scopeId() == null) {
                throw new BusinessRuleViolationException(
                        "Legacy scopeType and scopeId must be provided together"
                );
            }

            return List.of(new InvitationScopeRequest(
                    request.scopeType(),
                    request.scopeId()
            ));
        }

        if (!hasNewScopes) {
            throw new BusinessRuleViolationException(
                    "At least one organization scope is required"
            );
        }

        Set<String> uniqueScopes = new HashSet<>();

        for (InvitationScopeRequest scope : request.scopes()) {
            if (scope == null
                    || scope.scopeType() == null
                    || scope.scopeId() == null) {
                throw new BusinessRuleViolationException(
                        "Every scope must have a scopeType and scopeId"
                );
            }

            String key = scope.scopeType().name() + ":" + scope.scopeId();

            if (!uniqueScopes.add(key)) {
                throw new BusinessRuleViolationException(
                        "Duplicate organization scope: " + key
                );
            }
        }

        return List.copyOf(request.scopes());
    }

    private void validateRoleAssignable(
            Role role,
            Authentication authentication
    ) {
        if ("PLATFORM_ADMIN".equals(role.getCode())
                && !SecurityUtils.isPlatformAdmin(authentication)) {
            throw new AccessDeniedException(
                    "Only a platform administrator can assign "
                            + "the PLATFORM_ADMIN role"
            );
        }
    }

    private UUID getAuthenticatedUserId(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof AuthenticatedUser authenticatedUser)) {
            throw new AccessDeniedException(
                    "Authenticated user information is unavailable"
            );
        }

        return authenticatedUser.userId();
    }

    private boolean canAccessScope(
            Authentication authentication,
            OrganizationScopeType scopeType,
            UUID scopeId
    ) {
        return switch (scopeType) {
            case ENTERPRISE ->
                    scopeAuthorization.canAccessEnterprise(
                            authentication, scopeId
                    );
            case REGION ->
                    scopeAuthorization.canAccessRegion(
                            authentication, scopeId
                    );
            case CLUSTER ->
                    scopeAuthorization.canAccessCluster(
                            authentication, scopeId
                    );
            case INSTITUTION ->
                    scopeAuthorization.canAccessInstitution(
                            authentication, scopeId
                    );
            case CAMPUS ->
                    scopeAuthorization.canAccessCampus(
                            authentication, scopeId
                    );
        };
    }

    private void validateScopeExistsAndActive(
            OrganizationScopeType scopeType,
            UUID scopeId
    ) {
        boolean active = switch (scopeType) {
            case ENTERPRISE ->
                    enterpriseRepository.findById(scopeId)
                            .map(entity -> entity.isActive())
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Enterprise not found: " + scopeId
                                    )
                            );
            case REGION ->
                    regionRepository.findById(scopeId)
                            .map(entity -> entity.isActive())
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Region not found: " + scopeId
                                    )
                            );
            case CLUSTER ->
                    clusterRepository.findById(scopeId)
                            .map(entity -> entity.isActive())
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Cluster not found: " + scopeId
                                    )
                            );
            case INSTITUTION ->
                    institutionRepository.findById(scopeId)
                            .map(entity -> entity.isActive())
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Institution not found: " + scopeId
                                    )
                            );
            case CAMPUS ->
                    campusRepository.findById(scopeId)
                            .map(entity -> entity.isActive())
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Campus not found: " + scopeId
                                    )
                            );
        };

        if (!active) {
            throw new BusinessRuleViolationException(
                    "Cannot invite a user into an inactive organization scope"
            );
        }
    }

    private UserInvitationResponse toResponse(
            UserInvitation invitation,
            String status
    ) {
        List<InvitationScopeResponse> scopes =
                invitation.getScopes().isEmpty()
                        ? List.of(new InvitationScopeResponse(
                        invitation.getScopeType().name(),
                        invitation.getScopeId()
                ))
                        : invitation.getScopes().stream()
                        .map(scope -> new InvitationScopeResponse(
                                scope.getScopeType().name(),
                                scope.getScopeId()
                        ))
                        .toList();

        return new UserInvitationResponse(
                invitation.getId(),
                invitation.getInvitationEmail(),
                invitation.getUsername(),
                invitation.getRole().getCode(),
                invitation.getRole().getName(),
                invitation.getScopeType().name(),
                invitation.getScopeId(),
                invitation.getExpiresAt(),
                status,
                scopes
        );
    }
}
