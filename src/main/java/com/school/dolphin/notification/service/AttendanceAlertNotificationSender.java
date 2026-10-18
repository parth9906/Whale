package com.school.whale.notification.service;

import java.util.UUID;

public interface AttendanceAlertNotificationSender {

    void sendAttendanceAlert(UUID attendanceAlertId);
}