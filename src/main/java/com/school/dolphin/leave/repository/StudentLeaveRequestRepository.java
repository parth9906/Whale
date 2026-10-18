package com.school.whale.leave.repository;

import com.school.whale.leave.entity.LeaveRequestStatus;
import com.school.whale.leave.entity.StudentLeaveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

public interface StudentLeaveRequestRepository
        extends JpaRepository<StudentLeaveRequest, UUID> {

    Page<StudentLeaveRequest>
    findByInstitution_IdAndStatusOrderByStartDateAsc(
            UUID institutionId,
            LeaveRequestStatus status,
            Pageable pageable
    );

    boolean existsByStudent_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID studentId,
            Collection<LeaveRequestStatus> statuses,
            LocalDate endDate,
            LocalDate startDate
    );
}
