package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.EmployeeLeave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeLeaveRepository extends JpaRepository<EmployeeLeave, Long> {
    List<EmployeeLeave> findByEmployeeIdAndStatus(Long id, boolean b);

    EmployeeLeave findByIdAndStatus(Long leaveId, boolean b);

    EmployeeLeave findByEmployeeIdAndFromDateLessThanAndToDateGreaterThan(Long id, LocalDate localDate, LocalDate localDate1);

    EmployeeLeave findByEmployeeIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(Long id, LocalDate localDate, LocalDate localDate1);

    List<EmployeeLeave> findByEmployeeIdAndStatusOrderByIdDesc(Long id, boolean b);

    EmployeeLeave findByEmployeeIdAndFromDateLessThanEqualAndToDateGreaterThanEqualAndLeaveStatus(Long id, LocalDate localDate, LocalDate localDate1, String approved);
}
