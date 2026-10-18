package com.school.whale.identity.repository;

import com.school.whale.identity.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PermissionRepository
        extends JpaRepository<Permission, UUID> {

    boolean existsByCode(String code);
}