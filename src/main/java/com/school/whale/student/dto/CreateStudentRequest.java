package com.school.whale.student.dto;

import com.school.whale.identity.validation.BcryptPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateStudentRequest(

        @NotNull(message = "Institution ID is required")
        UUID institutionId,

        UUID campusId,

        @NotBlank(message = "Admission number is required")
        @Size(max = 50, message = "Admission number must not exceed 50 characters")
        String admissionNumber,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        LocalDate dateOfBirth,

        // Optional login credentials. Supply all three or none.
        @Size(max = 100, message = "Username must not exceed 100 characters")
        String username,

        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @BcryptPassword
        String password
) {}