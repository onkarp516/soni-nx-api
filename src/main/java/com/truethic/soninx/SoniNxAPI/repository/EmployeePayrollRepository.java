package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.EmployeePayroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeePayrollRepository extends JpaRepository<EmployeePayroll, Long> {
    EmployeePayroll findByEmployeeIdAndYearMonth(Long employeeId, String yearMonth);
    EmployeePayroll findByEmployeeIdAndInstituteIdAndYearMonth(Long employeeId, long instituteId, String yearMonth);
    @Query(value = "SELECT * FROM `employee_payroll_tbl` WHERE employee_id=?1 AND `year_month` IN (?2, ?3)", nativeQuery = true)
    EmployeePayroll findByEmployeeIdAndDateRange(Long employeeId, String fromDate, String toDate);
}
