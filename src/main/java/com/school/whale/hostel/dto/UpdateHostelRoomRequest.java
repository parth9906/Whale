package com.school.whale.hostel.dto;

import com.school.whale.hostel.entity.HostelRoomType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateHostelRoomRequest(
        @NotBlank @Size(max = 20) String roomNumber,
        Integer floor,
        @NotNull HostelRoomType roomType,
        @NotNull @Min(1) Integer capacity,
        @NotNull Long version
) {}
