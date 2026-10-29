package com.school.whale.identity.dto;

import com.school.whale.identity.validation.BcryptPassword;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ProvisionUserRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 100)
        String username,

        @NotBlank(message = "Email is required")
        @Email
        @Size(max = 255)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100)
        @BcryptPassword
        String password,

        @NotBlank(message = "First name is required")
        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @NotNull
        UUID roleId,

        @NotEmpty(message = "At least one organization scope is required")
        List<@Valid CreateOrganizationScopeRequest> scopes
) {}
