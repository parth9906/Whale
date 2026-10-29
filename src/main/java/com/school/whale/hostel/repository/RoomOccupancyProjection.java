package com.school.whale.hostel.repository;

import java.util.UUID;

public interface RoomOccupancyProjection {

    UUID getRoomId();

    Long getOccupied();
}
