package com.school.whale.student.service;

import com.school.whale.common.exception.BusinessRuleViolationException;
import com.school.whale.common.exception.DuplicateResourceException;
import com.school.whale.common.exception.ResourceNotFoundException;
import com.school.whale.guardian.repository.GuardianRepository;
import com.school.whale.identity.entity.OrganizationScopeType;
import com.school.whale.identity.entity.Role;
import com.school.whale.identity.entity.UserAccount;
import com.school.whale.identity.entity.UserOrganizationScope;
import com.school.whale.identity.entity.UserRole;
import com.school.whale.identity.entity.UserRoleId;
import com.school.whale.identity.repository.UserAccountRepository;
import com.school.whale.identity.repository.RoleRepository;
import com.school.whale.identity.repository.UserOrganizationScopeRepository;
import com.school.whale.identity.repository.UserRoleRepository;
import com.school.whale.identity.security.OrganizationScopeAuthorization;
import com.school.whale.staff.repository.StaffMemberRepository;
import com.school.whale.student.dto.LinkStudentAccountRequest;
import com.school.whale.student.dto.StudentAccountLinkDetailsResponse;
import com.school.whale.student.dto.StudentAccountLinkResponse;
import com.school.whale.student.entity.Student;
import com.school.whale.student.entity.StudentAccountLink;
import com.school.whale.student.repository.StudentAccountLinkRepository;
import com.school.whale.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentAccountLinkService {

    private static final String STUDENT_ROLE_CODE = "STUDENT";

    private final StudentRepository studentRepository;
    private final UserAccountRepository userAccountRepository;
    private final StudentAccountLinkRepository studentAccountLinkRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final GuardianRepository guardianRepository;
    private final OrganizationScopeAuthorization scopeAuthorization;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserOrganizationScopeRepository userOrganizationScopeRepository;

    @Transactional
    public StudentAccountLinkResponse link(
            UUID studentId,
            LinkStudentAccountRequest request,
            Authentication authentication
    ) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!student.isActive()) {
            throw new BusinessRuleViolationException(
                    "An inactive student cannot be linked to an account");
        }

        if (!scopeAuthorization.canAccessInstitution(
                authentication, student.getInstitution().getId())) {
            throw new AccessDeniedException(
                    "You do not have access to this student's institution");
        }

        UserAccount userAccount = userAccountRepository
                .findById(request.userAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));

        if (!userAccount.isActive()) {
            throw new BusinessRuleViolationException(
                    "An inactive user account cannot be linked to a student");
        }

        if (studentAccountLinkRepository.existsByStudent_Id(student.getId())) {
            throw new DuplicateResourceException(
                    "This student already has a linked user account");
        }

        if (studentAccountLinkRepository.existsByUserAccount_Id(userAccount.getId())) {
            throw new DuplicateResourceException(
                    "This user account is already linked to a student");
        }

        if (staffMemberRepository.existsByUserAccount_Id(userAccount.getId())
                || guardianRepository.existsByUserAccount_Id(userAccount.getId())) {
            throw new BusinessRuleViolationException(
                    "A staff or guardian account cannot also be linked to a student");
        }

        Role studentRole = roleRepository.findByCode(STUDENT_ROLE_CODE)
                .filter(Role::isActive)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Student role is not configured or inactive"));

        StudentAccountLink link = new StudentAccountLink();
        link.setStudent(student);
        link.setUserAccount(userAccount);

        StudentAccountLink saved = studentAccountLinkRepository.save(link);

        assignRole(userAccount, studentRole);
        assignInstitutionScope(userAccount, student.getInstitution().getId());

        return StudentAccountLinkResponse.from(saved);
    }



    @Transactional(readOnly = true)
    public StudentAccountLinkDetailsResponse getLink(
            UUID studentId,
            Authentication authentication
    ) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student not found"));

        // Enforce organization-scope authorization.
        boolean allowed;

        if (student.getCampus() != null) {
            allowed = scopeAuthorization.canAccessCampus(
                    authentication,
                    student.getCampus().getId()
            );
        } else {
            allowed = scopeAuthorization.canAccessInstitution(
                    authentication,
                    student.getInstitution().getId()
            );
        }

        if (!allowed) {
            throw new AccessDeniedException(
                    "You do not have access to this student's organization"
            );
        }

        return studentAccountLinkRepository.findByStudent_Id(studentId)
                .map(StudentAccountLinkDetailsResponse::from)
                .orElseGet(() ->
                        StudentAccountLinkDetailsResponse.notLinked(studentId));
    }


    private void assignRole(UserAccount userAccount, Role role) {
        if (userRoleRepository.existsByUserIdAndRoleId(
                userAccount.getId(), role.getId())) {
            return;
        }

        userRoleRepository.save(UserRole.builder()
                .id(new UserRoleId(userAccount.getId(), role.getId()))
                .user(userAccount)
                .role(role)
                .build());
    }

    private void assignInstitutionScope(
            UserAccount userAccount,
            UUID institutionId
    ) {
        if (userOrganizationScopeRepository.existsByUserIdAndScopeTypeAndScopeId(
                userAccount.getId(),
                OrganizationScopeType.INSTITUTION,
                institutionId)) {
            return;
        }

        userOrganizationScopeRepository.save(UserOrganizationScope.builder()
                .user(userAccount)
                .scopeType(OrganizationScopeType.INSTITUTION)
                .scopeId(institutionId)
                .build());
    }
}
