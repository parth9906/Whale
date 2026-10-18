package com.school.whale.identity.service;

import com.school.whale.common.exception.BusinessRuleViolationException;
import com.school.whale.common.exception.DuplicateResourceException;
import com.school.whale.common.exception.ResourceNotFoundException;
import com.school.whale.identity.entity.Permission;
import com.school.whale.identity.entity.Role;
import com.school.whale.identity.entity.RolePermission;
import com.school.whale.identity.entity.RolePermissionId;
import com.school.whale.identity.repository.PermissionRepository;
import com.school.whale.identity.repository.RolePermissionRepository;
import com.school.whale.identity.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Transactional
    public void assignPermission(
            UUID roleId,
            UUID permissionId
    ) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Role not found: " + roleId
                        )
                );

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Permission not found: " + permissionId
                        )
                );

        if (!role.isActive()) {
            throw new BusinessRuleViolationException(
                    "Cannot assign permission to inactive role"
            );
        }

        if (!permission.isActive()) {
            throw new BusinessRuleViolationException(
                    "Cannot assign inactive permission"
            );
        }

        if (rolePermissionRepository.existsByRoleIdAndPermissionId(
                roleId,
                permissionId
        )) {
            throw new DuplicateResourceException(
                    "Permission is already assigned to this role"
            );
        }

        RolePermission rolePermission = RolePermission.builder()
                .id(new RolePermissionId(roleId, permissionId))
                .role(role)
                .permission(permission)
                .build();

        rolePermissionRepository.save(rolePermission);
    }
}