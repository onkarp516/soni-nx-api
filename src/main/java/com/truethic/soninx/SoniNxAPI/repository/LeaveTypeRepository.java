package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    LeaveType findByIdAndStatus(long id, boolean b);

    List<LeaveType> findByStatus(boolean b);
    List<LeaveType> findAllByInstituteIdAndStatus(long instituteId, boolean b);

    @Query(value = "SELECT t1.name, t1.id, t1.leaves_allowed, IFNULL(t2.usedleaves,0) AS usedleaves FROM leave_type_master_tbl t1 " +
            "LEFT JOIN (SELECT leave_type_id, SUM(total_days) AS usedleaves FROM `employee_leave_tbl` WHERE employee_id=?1  AND leave_status='Approved' GROUP BY " +
            "leave_type_id) AS t2 ON t1.id = t2.leave_type_id", nativeQuery = true)
    List<Object[]> getEmployeeLeavesDashboardData(Long employeeId);

    @Query(value = "SELECT IFNULL(SUM(total_days), 0) AS total_leaves FROM `employee_leave_tbl` WHERE employee_id=?1 AND leave_type_id=?2 AND leave_status='Approved'", nativeQuery = true)
    Long getLeavesAlreadyApplied(Long id, Long categoryId);
}
