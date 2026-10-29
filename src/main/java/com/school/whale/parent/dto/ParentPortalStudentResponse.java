
package com.school.whale.parent.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ParentPortalStudentResponse(
        UUID studentId,
        String admissionNumber,
        String firstName,
        String lastName,
        LocalDate dateOfBirth
) {}