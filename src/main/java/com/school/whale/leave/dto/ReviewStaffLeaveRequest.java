package com.school.whale.leave.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewStaffLeaveRequest(
        @NotBlank @Size(max = 1000) String comment
) {}
