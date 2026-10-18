package com.school.whale.admissions.service;

import com.school.whale.academic.entity.AcademicYearStatus;
import com.school.whale.academic.entity.SchoolClass;
import com.school.whale.academic.entity.ClassSection;
import com.school.whale.academic.dto.EnrollStudentRequest;
import com.school.whale.academic.service.AcademicStructureService;
import com.school.whale.academic.repository.ClassSectionRepository;
import com.school.whale.academic.repository.SchoolClassRepository;
import com.school.whale.admissions.dto.AdmissionApplicationResponse;
import com.school.whale.admissions.dto.AdmissionApplicationSummaryResponse;
import com.school.whale.admissions.dto.CreateAdmissionApplicationRequest;
import com.school.whale.admissions.dto.EnrollAdmissionApplicationRequest;
import com.school.whale.admissions.dto.UpdateAdmissionApplicationRequest;
import com.school.whale.admissions.dto.UpdateAdmissionApplicationStatusRequest;
import com.school.whale.admissions.entity.AdmissionApplication;
import com.school.whale.admissions.entity.AdmissionApplicationStatus;
import com.school.whale.admissions.repository.AdmissionApplicationRepository;
import com.school.whale.common.exception.BusinessRuleViolationException;
import com.school.whale.common.exception.DuplicateResourceException;
import com.school.whale.common.exception.ResourceNotFoundException;
import com.school.whale.identity.entity.UserAccount;
import com.school.whale.identity.security.AuthenticatedUserResolver;
import com.school.whale.identity.security.OrganizationScopeAuthorization;
import com.school.whale.organization.entity.Campus;
import com.school.whale.organization.entity.Institution;
import com.school.whale.organization.repository.CampusRepository;
import com.school.whale.organization.repository.InstitutionRepository;
import com.school.whale.student.entity.Student;
import com.school.whale.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdmissionApplicationService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final List<AdmissionApplicationStatus>
            ACTIVE_DUPLICATE_STATUSES = List.of(
                    AdmissionApplicationStatus.APPLIED,
                    AdmissionApplicationStatus.UNDER_REVIEW);

    private static final Map<AdmissionApplicationStatus,
            Set<AdmissionApplicationStatus>> ALLOWED_TRANSITIONS = Map.of(
                    AdmissionApplicationStatus.APPLIED, Set.of(
                            AdmissionApplicationStatus.UNDER_REVIEW,
                            AdmissionApplicationStatus.ACCEPTED,
                            AdmissionApplicationStatus.REJECTED,
                            AdmissionApplicationStatus.WAITLISTED),
                    AdmissionApplicationStatus.UNDER_REVIEW, Set.of(
                            AdmissionApplicationStatus.ACCEPTED,
                            AdmissionApplicationStatus.REJECTED,
                            AdmissionApplicationStatus.WAITLISTED),
                    AdmissionApplicationStatus.WAITLISTED, Set.of(
                            AdmissionApplicationStatus.ACCEPTED,
                            AdmissionApplicationStatus.REJECTED),
                    AdmissionApplicationStatus.ACCEPTED, Set.of(),
                    AdmissionApplicationStatus.REJECTED, Set.of(),
                    AdmissionApplicationStatus.ENROLLED, Set.of()
            );

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final InstitutionRepository institutionRepository;
    private final CampusRepository campusRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassSectionRepository classSectionRepository;
    private final StudentRepository studentRepository;
    private final AcademicStructureService academicStructureService;
    private final OrganizationScopeAuthorization scopeAuthorization;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Transactional
    public AdmissionApplicationResponse create(
            CreateAdmissionApplicationRequest request,
            Authentication authentication
    ) {
        Institution institution = institutionRepository
                .findById(request.institutionId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Institution not found"));

        if (!institution.isActive()) {
            throw new BusinessRuleViolationException(
                    "Cannot accept applications for an inactive institution");
        }

        requireInstitutionAccess(authentication, institution.getId());

        Campus campus = resolveCampus(
                request.campusId(), institution.getId());

        SchoolClass applyingClass = requireApplyingClass(
                request.applyingClassId(), institution.getId());

        LocalDate applicationDate = request.applicationDate() == null
                ? LocalDate.now() : request.applicationDate();

        if (applicationDate.isAfter(LocalDate.now())) {
            throw new BusinessRuleViolationException(
                    "Application date cannot be in the future");
        }

        String firstName = request.applicantFirstName().trim();
        String lastName = request.applicantLastName().trim();

        requireNoActiveDuplicate(
                institution.getId(),
                firstName,
                lastName,
                request.applicantDateOfBirth());

        UserAccount creator =
                authenticatedUserResolver.requireUserAccount(authentication);

        AdmissionApplication application = AdmissionApplication.builder()
                .institution(institution)
                .campus(campus)
                .applyingClass(applyingClass)
                .applicantFirstName(firstName)
                .applicantLastName(lastName)
                .applicantDateOfBirth(request.applicantDateOfBirth())
                .parentName(normalize(request.parentName()))
                .parentPhone(normalize(request.parentPhone()))
                .parentEmail(normalize(request.parentEmail()))
                .notes(normalize(request.notes()))
                .applicationDate(applicationDate)
                .status(AdmissionApplicationStatus.APPLIED)
                .createdBy(creator)
                .build();

        return toResponse(admissionApplicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public AdmissionApplicationResponse getById(
            UUID applicationId,
            Authentication authentication
    ) {
        AdmissionApplication application = admissionApplicationRepository
                .findById(applicationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Admission application not found"));

        requireInstitutionAccess(
                authentication, application.getInstitution().getId());

        return toResponse(application);
    }

    @Transactional
    public AdmissionApplicationResponse updateStatus(
            UUID applicationId,
            UpdateAdmissionApplicationStatusRequest request,
            Authentication authentication
    ) {
        AdmissionApplication application = admissionApplicationRepository
                .findById(applicationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Admission application not found"));

        requireInstitutionAccess(
                authentication, application.getInstitution().getId());

        AdmissionApplicationStatus current = application.getStatus();
        AdmissionApplicationStatus target = request.status();

        if (current == target) {
            throw new BusinessRuleViolationException(
                    "Application is already in status " + target);
        }

        if (!ALLOWED_TRANSITIONS
                .getOrDefault(current, Set.of())
                .contains(target)) {
            throw new BusinessRuleViolationException(
                    "Cannot transition application from " + current
                            + " to " + target);
        }

        application.setStatus(target);

        return toResponse(admissionApplicationRepository.save(application));
    }

    @Transactional
    public AdmissionApplicationResponse update(
            UUID applicationId,
            UpdateAdmissionApplicationRequest request,
            Authentication authentication
    ) {
        AdmissionApplication application = admissionApplicationRepository
                .findById(applicationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Admission application not found"));

        requireInstitutionAccess(
                authentication, application.getInstitution().getId());

        if (application.getStatus() != AdmissionApplicationStatus.APPLIED
                && application.getStatus()
                != AdmissionApplicationStatus.UNDER_REVIEW) {
            throw new BusinessRuleViolationException(
                    "Only APPLIED or UNDER_REVIEW applications can be edited");
        }

        Campus campus = resolveCampus(
                request.campusId(), application.getInstitution().getId());

        SchoolClass applyingClass = requireApplyingClass(
                request.applyingClassId(),
                application.getInstitution().getId());

        application.setCampus(campus);
        application.setApplyingClass(applyingClass);
        application.setParentName(normalize(request.parentName()));
        application.setParentPhone(normalize(request.parentPhone()));
        application.setParentEmail(normalize(request.parentEmail()));
        application.setNotes(normalize(request.notes()));

        return toResponse(admissionApplicationRepository.save(application));
    }

    @Transactional
    public AdmissionApplicationResponse enroll(
            UUID applicationId,
            EnrollAdmissionApplicationRequest request,
            Authentication authentication
    ) {
        AdmissionApplication application = admissionApplicationRepository
                .findById(applicationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Admission application not found"));

        requireInstitutionAccess(
                authentication, application.getInstitution().getId());

        if (application.getStatus()
                != AdmissionApplicationStatus.ACCEPTED) {
            throw new BusinessRuleViolationException(
                    "Only accepted applications can be enrolled");
        }

        String admissionNumber = request.admissionNumber().trim();

        if (studentRepository.existsByInstitution_IdAndAdmissionNumber(
                application.getInstitution().getId(),
                admissionNumber)) {
            throw new DuplicateResourceException(
                    "Admission number already exists in this institution");
        }

        ClassSection section = classSectionRepository
                .findById(request.sectionId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Section not found"));

        if (!section.getSchoolClass().getId()
                .equals(application.getApplyingClass().getId())) {
            throw new BusinessRuleViolationException(
                    "Section does not belong to the application's class");
        }

        Student student = Student.builder()
                .institution(application.getInstitution())
                .campus(application.getCampus())
                .admissionNumber(admissionNumber)
                .firstName(application.getApplicantFirstName())
                .lastName(application.getApplicantLastName())
                .dateOfBirth(application.getApplicantDateOfBirth())
                .active(true)
                .build();

        Student savedStudent = studentRepository.save(student);

        academicStructureService.enrollStudent(
                new EnrollStudentRequest(
                        savedStudent.getId(),
                        request.sectionId(),
                        request.startDate()),
                authentication);

        application.setEnrolledStudent(savedStudent);
        application.setStatus(AdmissionApplicationStatus.ENROLLED);

        return toResponse(admissionApplicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public Page<AdmissionApplicationResponse> search(
            UUID institutionId,
            UUID campusId,
            AdmissionApplicationStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable,
            Authentication authentication
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessRuleViolationException(
                    "fromDate must not be after toDate");
        }

        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new BusinessRuleViolationException(
                    "Page size cannot exceed " + MAX_PAGE_SIZE);
        }

        requireInstitutionAccess(authentication, institutionId);

        if (campusId != null) {
            requireCampusInInstitution(campusId, institutionId);
        }

        return admissionApplicationRepository
                .search(
                        institutionId,
                        campusId,
                        status,
                        fromDate,
                        toDate,
                        pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AdmissionApplicationSummaryResponse summary(
            UUID institutionId,
            UUID campusId,
            LocalDate fromDate,
            LocalDate toDate,
            Authentication authentication
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessRuleViolationException(
                    "fromDate must not be after toDate");
        }

        requireInstitutionAccess(authentication, institutionId);

        if (campusId != null) {
            requireCampusInInstitution(campusId, institutionId);
        }

        Map<AdmissionApplicationStatus, Long> counts =
                new EnumMap<>(AdmissionApplicationStatus.class);

        for (AdmissionApplicationStatus status
                : AdmissionApplicationStatus.values()) {
            counts.put(status, 0L);
        }

        admissionApplicationRepository
                .countByStatus(institutionId, campusId, fromDate, toDate)
                .forEach(projection ->
                        counts.put(projection.getStatus(), projection.getCnt()));

        return new AdmissionApplicationSummaryResponse(
                counts.get(AdmissionApplicationStatus.APPLIED),
                counts.get(AdmissionApplicationStatus.UNDER_REVIEW),
                counts.get(AdmissionApplicationStatus.ACCEPTED),
                counts.get(AdmissionApplicationStatus.REJECTED),
                counts.get(AdmissionApplicationStatus.WAITLISTED),
                counts.get(AdmissionApplicationStatus.ENROLLED)
        );
    }

    private void requireNoActiveDuplicate(
            UUID institutionId,
            String firstName,
            String lastName,
            LocalDate dateOfBirth
    ) {
        long duplicates = admissionApplicationRepository
                .countByInstitution_IdAndApplicantFirstNameIgnoreCaseAndApplicantLastNameIgnoreCaseAndApplicantDateOfBirthAndStatusIn(
                        institutionId,
                        firstName,
                        lastName,
                        dateOfBirth,
                        ACTIVE_DUPLICATE_STATUSES);

        if (duplicates > 0) {
            throw new DuplicateResourceException(
                    "An active application already exists for this applicant");
        }
    }

    private Campus resolveCampus(
            UUID campusId,
            UUID institutionId
    ) {
        if (campusId == null) {
            return null;
        }

        Campus campus = requireCampusInInstitution(campusId, institutionId);

        if (!campus.isActive()) {
            throw new BusinessRuleViolationException(
                    "Cannot accept applications for an inactive campus");
        }

        return campus;
    }

    private Campus requireCampusInInstitution(
            UUID campusId,
            UUID institutionId
    ) {
        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Campus not found"));

        if (!campus.getInstitution().getId().equals(institutionId)) {
            throw new BusinessRuleViolationException(
                    "Campus does not belong to the requested institution");
        }

        return campus;
    }

    private SchoolClass requireApplyingClass(
            UUID classId,
            UUID institutionId
    ) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "School class not found"));

        if (!schoolClass.getAcademicYear()
                .getInstitution()
                .getId()
                .equals(institutionId)) {
            throw new BusinessRuleViolationException(
                    "Class does not belong to the requested institution");
        }

        if (!schoolClass.isActive()) {
            throw new BusinessRuleViolationException(
                    "Cannot accept applications for an inactive class");
        }

        if (schoolClass.getAcademicYear().getStatus()
                == AcademicYearStatus.CLOSED) {
            throw new BusinessRuleViolationException(
                    "Cannot accept applications for a closed academic year");
        }

        return schoolClass;
    }

    private void requireInstitutionAccess(
            Authentication authentication,
            UUID institutionId
    ) {
        if (!scopeAuthorization.canAccessInstitution(
                authentication, institutionId)) {
            throw new AccessDeniedException(
                    "You do not have access to this institution.");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private AdmissionApplicationResponse toResponse(
            AdmissionApplication application
    ) {
        SchoolClass schoolClass = application.getApplyingClass();

        return new AdmissionApplicationResponse(
                application.getId(),
                application.getInstitution().getId(),
                application.getCampus() == null
                        ? null : application.getCampus().getId(),
                schoolClass.getId(),
                schoolClass.getName(),
                schoolClass.getCode(),
                application.getApplicantFirstName(),
                application.getApplicantLastName(),
                application.getApplicantDateOfBirth(),
                application.getParentName(),
                application.getParentPhone(),
                application.getParentEmail(),
                application.getNotes(),
                application.getApplicationDate(),
                application.getStatus(),
                application.getEnrolledStudent() == null
                        ? null : application.getEnrolledStudent().getId(),
                application.getCreatedBy().getId(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
