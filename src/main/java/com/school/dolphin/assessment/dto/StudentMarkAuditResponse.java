
package com.school.whale.assessment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentMarkAuditResponse(
        UUID id,
        UUID studentMarkId,
        BigDecimal previousMarks,
        BigDecimal newMarks,
        String previousRemarks,
        String newRemarks,
        UUID changedByUserId,
        OffsetDateTime changedAt
) {}