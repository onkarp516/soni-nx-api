package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Attendance findByEmployeeIdAndAttendanceDate(Long id, LocalDate localDate);

    Attendance findByIdAndStatus(Long attendanceId, boolean b);

    @Query(
            value = "SELECT IFNULL(SUM(wages_hour_basis),0) FROM `attendance_tbl` AS a WHERE a.attendance_date " +
                    "BETWEEN ?2 AND ?3 AND a.employee_id=?1",
            nativeQuery = true
    )
    Double getTotalPaymentUpto(Long id, LocalDate firstDateOfMonth, LocalDate currentDate);

    @Query(
            value = "SELECT IFNULL(SUM(request_amount),0) FROM `advance_payment_tbl` AS a WHERE a.date_of_request " +
                    "BETWEEN ?2 AND ?3 AND a.employee_id=?1 AND a.payment_status=?4",
            nativeQuery = true
    )
    Double getTotalPendingPaymentUpto(Long id, LocalDate firstDateOfMonth, LocalDate currentDate, String pending);

    @Query(
            value = "SELECT IFNULL(SUM(paid_amount),0) FROM `advance_payment_tbl` AS a WHERE a.date_of_request " +
                    "BETWEEN ?2 AND ?3 AND a.employee_id=?1 AND a.payment_status=?4",
            nativeQuery = true
    )
    Double getTotalApprovePaymentUpto(Long id, LocalDate firstDateOfMonth, LocalDate currentDate, String approved);

    List<Attendance> findByCheckOutTimeIsNull();

    @Query(value = "SELECT * FROM attendance_tbl WHERE employee_id=?1 AND status =?2 AND check_out_time IS NULL ORDER BY id DESC" +
            " LIMIT 1",
            nativeQuery = true)
    Attendance findLastRecordOfEmployeeWithoutCheckOut(Long id, boolean b);

    @Query(value = "SELECT * FROM attendance_tbl WHERE employee_id=?1 AND attendance_date!=?2 AND check_out_time IS " +
            "NOT NULL ORDER BY id DESC LIMIT 1",
            nativeQuery = true)
    Attendance findLastSecondRecordOfEmployee(Long id, LocalDate localDate);

    @Query(value = "SELECT IFNULL(COUNT(att.id),0) FROM attendance_tbl att LEFT JOIN employee_tbl emp ON att.employee_id=emp.id " +
            "WHERE emp.status=1 AND att.attendance_date=?1 AND att.institute_id=?2 AND att.is_half_day IS NULL",
            nativeQuery = true)
    int getPresentEmployeeCount(LocalDate b, Long instituteId);

    @Query(value = "SELECT IFNULL(COUNT(attendance_tbl.id),0) FROM `attendance_tbl` WHERE attendance_date=?1 AND institute_id=?2 AND is_half_day=1", nativeQuery = true)
    int getHalfDayEmployeeCount(LocalDate date, Long instituteId);

    @Query(value = "SELECT IFNULL(COUNT(attendance_tbl.id),0) FROM `attendance_tbl` WHERE institute_id=?1 AND attendance_date=?2",
            nativeQuery = true)
    int getPresentEmployeeCountOfInstitute(long institute_id, LocalDate b);

    /*@Query(value = "SELECT IFNULL(COUNT(employee_leave_tbl.id),0) FROM `employee_leave_tbl` LEFT JOIN employee_tbl ON" +
            " employee_leave_tbl.employee_id=employee_tbl.id  WHERE leave_status='Approved' AND from_date<=?1 AND to_date>=?1",
            nativeQuery = true)*/
    @Query(value = "SELECT IFNULL(COUNT(DISTINCT(employee_id)),0) FROM `employee_leave_tbl` LEFT JOIN employee_tbl ON" +
            " employee_leave_tbl.employee_id=employee_tbl.id  WHERE leave_status='Approved' AND from_date<=?1 AND" +
            " to_date>=?1 AND employee_leave_tbl.employee_id NOT IN (SELECT attendance_tbl.employee_id from" +
            " attendance_tbl WHERE attendance_date=?1 AND institute_id=?2)",
            nativeQuery = true)
    int getLeaveEmployeeCount(LocalDate localDate, Long instituteId);


    @Query(value = "SELECT IFNULL(COUNT(attendance_tbl.id),0) FROM `attendance_tbl` WHERE attendance_date=?1 AND" +
            " attendance_tbl.shift_id=?2 AND attendance_tbl.status=?3 AND attendance_tbl.institute_id=?4",
            nativeQuery = true)
    int getPresentEmployeeCountByShift(LocalDate b, Long shiftId, boolean b1, long institute_id);

    Attendance findTop1ByEmployeeIdOrderByIdDesc(Long id);

    @Query(value = "SELECT COUNT(id) FROM `attendance_tbl` WHERE YEAR(attendance_date)=?1 AND MONTH(attendance_date)=?2" +
            " AND employee_id=?3 AND status=?4 AND attendance_status=?5", nativeQuery = true)
    double getPresentDaysOfEmployeeOfMonth(int year, int month, Long employeeId, boolean b, String approve);

    @Query(value = "SELECT IFNULL(SUM(working_hours),0), IFNULL(SUM(wages_point_basis),0), IFNULL(SUM(wages_pcs_basis),0), IFNULL(SUM(wages_hour_basis),0)," +
            "  IFNULL(SUM(wages_per_day),0), IFNULL(SUM(final_day_salary),0) FROM `attendance_tbl` WHERE YEAR(attendance_date)=?1 AND MONTH(attendance_date)=?2" +
            " AND employee_id=?3 AND status=?4 AND attendance_status=?5", nativeQuery = true)
    String getSumDataOfEmployeeOfMonth(int year, int month, Long employeeId, boolean b, String approve);

    Attendance findByEmployeeIdAndAttendanceDateAndStatus(Long employeeId, LocalDate attendanceDate, boolean b);

    Attendance findByIdAndEmployeeIdAndStatus(Long attendanceId, Long id, boolean b);

    @Query(value = "SELECT * FROM `attendance_tbl` WHERE YEAR(attendance_date)=?1 AND MONTH(attendance_date)=?2 AND" +
            " employee_id=?3 AND status=?4 AND attendance_status=?5", nativeQuery = true)
    List<Object[]> getAttendanceList(int year, int month, Long employeeId, boolean b, String approve);

    @Query(value = "SELECT * FROM `attendance_tbl` WHERE YEAR(attendance_date)=?1 AND MONTH(attendance_date)=?2 AND" +
            " employee_id=?3 AND status=?4 AND attendance_status=?5", nativeQuery = true)
    List<Attendance> getAttendanceListOfEmployee(int year, int month, Long employeeId, boolean b, String approve);

    @Query(value = "SELECT * FROM `attendance_tbl` LEFT JOIN employee_tbl ON attendance_tbl.employee_id = employee_tbl.id " +
            " LEFT JOIN designation_tbl ON employee_tbl.designation_id = designation_tbl.id WHERE" +
            " attendance_tbl.attendance_date=?1 AND attendance_tbl.status=?2 AND designation_tbl.code=?3 ", nativeQuery = true)
    List<Attendance> findByAttendanceDateAndStatus(LocalDate today, boolean b, String level);

    @Query(value = "SELECT * FROM `attendance_tbl` LEFT JOIN employee_tbl ON attendance_tbl.employee_id = employee_tbl.id " +
            " LEFT JOIN designation_tbl ON employee_tbl.designation_id = designation_tbl.id WHERE" +
            " attendance_tbl.attendance_date=?1 AND attendance_tbl.status=?2 AND designation_tbl.code=?3  AND attendance_tbl.institute_id=?4", nativeQuery = true)
    List<Attendance> findByInstituteIdAndAttendanceDateAndStatus(LocalDate today, boolean b, String level, long instituteId);

    @Query(value = "SELECT * FROM attendance_tbl WHERE employee_id=?1 AND status=?4 AND attendance_date BETWEEN ?3 AND ?2", nativeQuery = true)
    List<Attendance> findByEmployeeIdAndSelectedDate(Long employeeId, String toDate, String fromDate, boolean status);

    @Query(value = "SELECT * from attendance_tbl WHERE status=1 AND institute_id=2 AND employee_id=?1 AND YEAR(attendance_date)=?2 AND MONTH(attendance_date)=?3 AND (is_manual_punch_in=1 OR is_manual_punch_out=1)", nativeQuery = true)
    List<Attendance> getManualAttendanceList(Long employeeId, String year, String month);

    @Query(value = "SELECT * from attendance_tbl WHERE status=1 AND institute_id=2 AND YEAR(attendance_date)=?1 AND MONTH(attendance_date)=?2 AND (is_manual_punch_in=1 OR is_manual_punch_out=1)", nativeQuery = true)
    List<Attendance> getManualAttendanceListOfAll(String year, String month);

    @Query(value = "SELECT * from attendance_tbl WHERE status=1 AND institute_id=2 AND YEAR(attendance_date)=?2 AND MONTH(attendance_date)=?3 AND employee_id=?1 AND is_late=1", nativeQuery = true)
    List<Attendance> getLateAttendanceList(Long employeeId, String year, String month);

    @Query(value = "SELECT * from attendance_tbl WHERE status=1 AND institute_id=2 AND YEAR(attendance_date)=?1 AND MONTH(attendance_date)=?2 AND is_late=1", nativeQuery = true)
    List<Attendance> getLateAttendanceListOfAll(String year, String month);

    @Query(value = "SELECT COUNT(is_late) AS late_count FROM `attendance_tbl` WHERE employee_id=?1 AND is_late=1 AND MONTH(attendance_date)=?2", nativeQuery = true)
    Long getLateCount(Long id, String monthValue);
}
