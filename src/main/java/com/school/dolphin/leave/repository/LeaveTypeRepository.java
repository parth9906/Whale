package com.school.whale.leave.repository;


import com.school.whale.leave.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeaveTypeRepository
        extends JpaRepository<LeaveType, UUID> {

    List<LeaveType> findByActiveTrueOrderByNameAsc();
}