package com.school.whale.notification.service;

import com.school.whale.notification.entity.NotificationOutbox;

public interface OutboxEventHandler {

    String eventType();

    void handle(NotificationOutbox event);
}
