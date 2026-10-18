package com.school.whale.organization.repository;

import com.school.whale.organization.entity.Enterprise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnterpriseRepository extends JpaRepository<Enterprise, UUID> {

    boolean existsByCode(String code);
    List<Enterprise> findAllByActiveTrue();
}