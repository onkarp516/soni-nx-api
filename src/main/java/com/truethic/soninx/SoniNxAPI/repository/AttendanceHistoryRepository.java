package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.AttendanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceHistoryRepository extends JpaRepository<AttendanceHistory, Long> {
}
