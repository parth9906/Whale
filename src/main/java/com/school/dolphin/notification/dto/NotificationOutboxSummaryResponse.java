package com.school.whale.notification.dto;

public record NotificationOutboxSummaryResponse(
        long pending,
        long processing,
        long delivered,
        long dead,
        long total
) {
}