
package com.school.whale.assessment.repository;

import com.school.whale.assessment.entity.StudentMarkAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentMarkAuditRepository
        extends JpaRepository<StudentMarkAudit, UUID> {

    List<StudentMarkAudit>
    findByStudentMark_IdOrderByChangedAtDesc(UUID studentMarkId);
}