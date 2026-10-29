package com.school.whale.leave.repository;


import com.school.whale.leave.entity.StaffLeaveAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StaffLeaveAuditRepository
        extends JpaRepository<StaffLeaveAudit, UUID> {

    List<StaffLeaveAudit>
    findByLeaveRequest_IdOrderByChangedAtDesc(UUID leaveRequestId);
}
