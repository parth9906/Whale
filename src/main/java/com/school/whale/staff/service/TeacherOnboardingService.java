package com.school.whale.staff.service;

import com.school.whale.common.exception.BusinessRuleViolationException;
import com.school.whale.common.exception.ResourceNotFoundException;
import com.school.whale.identity.dto.CreateOrganizationScopeRequest;
import com.school.whale.identity.dto.CreateUserRequest;
import com.school.whale.identity.dto.UserResponse;
import com.school.whale.identity.entity.OrganizationScopeType;
import com.school.whale.identity.entity.Role;
import com.school.whale.identity.entity.UserAccount;
import com.school.whale.identity.repository.RoleRepository;
import com.school.whale.identity.repository.UserAccountRepository;
import com.school.whale.identity.security.OrganizationScopeAuthorization;
import com.school.whale.identity.service.UserOrganizationScopeService;
import com.school.whale.identity.service.UserRoleService;
import com.school.whale.identity.service.UserService;
import com.school.whale.organization.entity.Campus;
import com.school.whale.organization.entity.Institution;
import com.school.whale.organization.repository.CampusRepository;
import com.school.whale.organization.repository.InstitutionRepository;
import com.school.whale.staff.dto.CreateStaffRequest;
import com.school.whale.staff.dto.StaffAssignmentResponse;
import com.school.whale.staff.dto.OnboardTeacherRequest;
import com.school.whale.staff.dto.TeacherOnboardingResponse;
import com.school.whale.staff.entity.StaffType;
import com.school.whale.staff.repository.StaffMemberRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherOnboardingService {

    private final StaffManagementService staffManagementService;
    private final StaffMemberRepository staffMemberRepository;
    private final InstitutionRepository institutionRepository;
    private final CampusRepository campusRepository;
    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrganizationScopeAuthorization scopeAuthorization;
    private final UserService userService;
    private final UserRoleService userRoleService;
    private final UserOrganizationScopeService userOrganizationScopeService;

    public TeacherOnboardingService(
            StaffManagementService staffManagementService,
            StaffMemberRepository staffMemberRepository,
            InstitutionRepository institutionRepository,
            CampusRepository campusRepository,
            RoleRepository roleRepository,
            UserAccountRepository userAccountRepository,
            OrganizationScopeAuthorization scopeAuthorization,
            UserService userService,
            UserRoleService userRoleService,
            UserOrganizationScopeService userOrganizationScopeService
    ) {
        this.staffManagementService = staffManagementService;
        this.staffMemberRepository = staffMemberRepository;
        this.institutionRepository = institutionRepository;
        this.campusRepository = campusRepository;
        this.roleRepository = roleRepository;
        this.userAccountRepository = userAccountRepository;
        this.scopeAuthorization = scopeAuthorization;
        this.userService = userService;
        this.userRoleService = userRoleService;
        this.userOrganizationScopeService = userOrganizationScopeService;
    }

    @Transactional
    public TeacherOnboardingResponse onboardTeacher(
            OnboardTeacherRequest request,
            Authentication authentication
    ) {
        Institution institution = institutionRepository
                .findById(request.institutionId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Institution not found"));

        if (!institution.isActive()) {
            throw new BusinessRuleViolationException(
                    "Cannot onboard a teacher into an inactive institution");
        }

        if (!scopeAuthorization.canAccessInstitution(
                authentication, institution.getId())) {
            throw new AccessDeniedException(
                    "You do not have access to this institution");
        }

        Campus campus = null;
        if (request.campusId() != null) {
            campus = campusRepository.findById(request.campusId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Campus not found"));

            if (!campus.isActive()
                    || !campus.getInstitution().getId()
                    .equals(institution.getId())) {
                throw new BusinessRuleViolationException(
                        "Campus must be active and belong to the selected institution");
            }

            if (!scopeAuthorization.canAccessCampus(
                    authentication, campus.getId())) {
                throw new AccessDeniedException(
                        "You do not have access to this campus");
            }
        }

        Role teacherRole = roleRepository.findByCode("TEACHER")
                .filter(Role::isActive)
                .orElseThrow(() ->
                        new BusinessRuleViolationException(
                                "TEACHER role is not configured or active"));

        // Create the account using the existing password hashing logic.
        UserResponse createdUser = userService.create(
                new CreateUserRequest(
                        request.username().trim(),
                        request.email().trim(),
                        request.password(),
                        request.firstName().trim(),
                        request.lastName() == null
                                ? null : request.lastName().trim()
                )
        );

        UserAccount userAccount = userAccountRepository
                .findByUsername(request.username().trim())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Created user account could not be found"));

        // Assign the teacher role and institution scope.
        userRoleService.assignRole(
                userAccount.getId(), teacherRole.getId());

        userOrganizationScopeService.assignScope(
                userAccount.getId(),
                new CreateOrganizationScopeRequest(
                        OrganizationScopeType.INSTITUTION,
                        institution.getId()
                )
        );

        CreateStaffRequest staffRequest = new CreateStaffRequest(
                institution.getId(),
                request.campusId(),
                userAccount.getId(),
                request.firstName(),
                request.lastName() == null ? "" : request.lastName(),
                request.email(),
                request.phone(),
                request.employeeNumber(),
                StaffType.TEACHER,
                request.jobTitle(),
                request.startDate()
        );

        StaffAssignmentResponse staffResponse =
                staffManagementService.createStaff(
                        staffRequest, authentication);

        return new TeacherOnboardingResponse(
                userAccount.getId(),
                staffResponse.staffMemberId(),
                staffResponse.assignmentId(),
                institution.getId(),
                request.campusId(),
                userAccount.getUsername(),
                userAccount.getEmail(),
                "TEACHER",
                staffResponse.status().name(),
                "Teacher account created successfully. The user can log in immediately."
        );
    }
}