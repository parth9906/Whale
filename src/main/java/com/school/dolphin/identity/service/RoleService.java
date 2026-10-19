package com.school.whale.identity.service;

import com.school.whale.common.exception.DuplicateResourceException;
import com.school.whale.common.exception.ResourceNotFoundException;
import com.school.whale.identity.dto.CreateRoleRequest;
import com.school.whale.identity.dto.RoleResponse;
import com.school.whale.identity.entity.Role;
import com.school.whale.identity.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    @Transactional
    public RoleResponse create(CreateRoleRequest request) {

        if (roleRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException(
                    "Role code already exists: " + request.code()
            );
        }

        Role role = Role.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .active(true)
                .build();

        Role savedRole = roleRepository.save(role);

        return toResponse(savedRole);
    }

    @Transactional(readOnly = true)
    public RoleResponse getById(UUID id) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Role not found: " + id
                        )
                );

        return toResponse(role);
    }

    @Transactional(readOnly = true)
    public Page<RoleResponse> search(
            String search,
            Boolean active,
            Pageable pageable,
            Authentication authentication
    ) {
        String normalizedSearch = search == null
                ? ""
                : search.trim();

        boolean isPlatformAdmin = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_PLATFORM_ADMIN".equals(
                                authority.getAuthority()
                        )
                );

        return roleRepository
                .searchRoles(
                        normalizedSearch,
                        active,
                        !isPlatformAdmin,
                        pageable
                )
                .map(this::toResponse);
    }
    private RoleResponse toResponse(Role role) {

        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.isActive()
        );
    }
}