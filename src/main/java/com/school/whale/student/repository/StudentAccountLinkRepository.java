
package com.school.whale.student.repository;

import com.school.whale.student.entity.StudentAccountLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StudentAccountLinkRepository
        extends JpaRepository<StudentAccountLink, UUID> {

    Optional<StudentAccountLink> findByUserAccount_Id(
            UUID userAccountId
    );

    Optional<StudentAccountLink> findByStudent_Id(
            UUID studentId
    );

    boolean existsByStudent_Id(UUID studentId);

    boolean existsByUserAccount_Id(UUID userAccountId);
}
