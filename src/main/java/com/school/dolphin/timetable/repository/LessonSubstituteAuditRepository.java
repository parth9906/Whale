package com.school.whale.timetable.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.school.whale.timetable.entity.LessonSubstituteAudit;

public interface LessonSubstituteAuditRepository
        extends JpaRepository<LessonSubstituteAudit, UUID> {

    List<LessonSubstituteAudit>
    findByOccurrence_IdOrderByChangedAtDesc(UUID occurrenceId);
}