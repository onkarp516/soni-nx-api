package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.TaskMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TaskMasterRepository extends JpaRepository<TaskMaster, Long> {
    List<TaskMaster> findByStatus(boolean b);

    TaskMaster findByIdAndStatus(Long taskId, boolean b);

    List<TaskMaster> findByEmployeeIdAndAttendanceIdAndTaskTypeAndStatusOrderByIdDesc(Long id, Long attendanceId, int i, boolean b);

    List<TaskMaster> findByEmployeeIdAndAttendanceIdAndTaskTypeNotAndStatusOrderByIdDesc(Long id, Long attendanceId, int i, boolean b);

    TaskMaster findTop1ByTaskMasterIdOrderByIdDesc(Long id);

    @Query(
            value = " SELECT IFNULL(SUM(total_time),0) FROM task_master_tbl as a WHERE a.task_master_id=?1",
            nativeQuery = true
    )
    double getSumOfBreakTime(Long id);

    @Query(
            value = " SELECT IFNULL(SUM(actual_work_time),0) FROM task_master_tbl as a WHERE a.attendance_id=?1",
            nativeQuery = true
    )
    double getSumOfActualWorkTime(Long id);

    @Query(
            value = " SELECT IFNULL(SUM(actual_production),0) FROM task_master_tbl as a WHERE a.machine_id=?1 AND a.task_date=?2",
            nativeQuery = true
    )
    double getSumOfActualProductionOfMachineForDate(Long id, LocalDate localDate);

    @Query(
            value = " SELECT IFNULL(SUM(required_production),0) FROM task_master_tbl as a WHERE a.machine_id=?1 AND a.task_date=?2",
            nativeQuery = true
    )
    double getSumOfRequiredProductionOfMachineForDate(Long id, LocalDate localDate);

    TaskMaster findByIdAndStatusOrderByIdDesc(Long taskId, boolean b);

    TaskMaster findByEmployeeIdAndAttendanceIdAndTaskTypeAndStartTimeNotNullAndEndTimeNull(Long id, Long id1, int i);

    @Query(
//            value="SELECT * from task_master_tbl as a  WHERE a.employee_id=?1 AND work_break_id=?2",
            value = "SELECT task_master_tbl.work_break_id,task_master_tbl.employee_id from task_master_tbl" +
                    " INNER JOIN break_tbl ON task_master_tbl.work_break_id=break_tbl.id WHERE task_master_tbl.work_break_id=8",

            nativeQuery = true
    )
    List<TaskMaster> findWorkBreakLike(Long id, Long id1);


    @Query(value = "SELECT IFNULL(SUM(ok_qty),0), IFNULL(SUM(wages_point_basis),0), IFNULL(SUM(wages_pcs_basis),0), " +
            "  IFNULL(SUM(total_point),0), IFNULL(SUM(prod_working_hour),0), IFNULL(SUM(working_hour_with_setting),0)," +
            "  IFNULL(SUM(break_wages),0) FROM `task_master_tbl` WHERE attendance_id=?1 AND work_done=1" +
            " AND status=1", nativeQuery = true)
    String getTaskWagesData(Long id);

//    @Query(value = "SELECT start_time FROM `task_master_tbl` WHERE attendance_id=?1 AND work_done=1 AND status=1 ORDER BY start_time ASC LIMIT 1", nativeQuery = true)
    @Query(value = "SELECT start_time FROM `task_master_tbl` WHERE attendance_id=?1 AND status=1 ORDER BY start_time ASC LIMIT 1", nativeQuery = true)
    LocalDateTime getInTime(Long id);

//    @Query(value = "SELECT end_time FROM `task_master_tbl` WHERE attendance_id=?1 AND work_done=1 AND status=1 ORDER BY end_time DESC LIMIT 1", nativeQuery = true)
    @Query(value = "SELECT end_time FROM `task_master_tbl` WHERE attendance_id=?1 AND status=1 ORDER BY end_time DESC LIMIT 1", nativeQuery = true)
    LocalDateTime getOutTime(Long id);

    List<TaskMaster> findByAttendanceIdAndStatusAndEndTimeIsNull(Long id, boolean b);

    List<TaskMaster> findByAttendanceIdAndStatus(Long attendanceId, boolean b);

    @Modifying
    @Transactional
    @Query(value = "UPDATE task_master_tbl SET status=?2 WHERE task_master_id=?1", nativeQuery = true)
    void updateTaskBreaksStatus(Long taskId, boolean b);

    List<TaskMaster> findByTaskMasterIdAndStatus(Long taskId, boolean b);

    TaskMaster findByAttendanceIdAndWorkBreakIdAndStatusAndWorkDone(Long id, Long id1, boolean b, boolean b1);

    TaskMaster findTop1ByAttendanceIdAndStatusAndTaskStatus(Long attendanceId, boolean b, String s);

    TaskMaster findByEmployeeIdAndAttendanceIdAndTaskTypeAndMachineIdAndJobIdAndJobOperationIdAndTaskStatusAndStatus(Long id, Long attendanceId, Integer taskType, Long machineId, Long jobId, Long jobOperationId, String s, boolean b);

    @Query(value = "SELECT machine_number, job_name, operation_name, SUM( machine_reject_qty), " +
            "SUM(rework_qty), SUM(doubtful_qty), SUM(un_machined_qty) FROM `task_view` WHERE task_date BETWEEN ?1 AND ?2 AND " +
            "(machine_reject_qty > 0 OR rework_qty > 0) GROUP BY machine_id", nativeQuery = true)
    List<Object[]> getRejectionMachineListBetweenDates(String date1, String date2);

    @Query(value = "SELECT IFNULL(SUM(machine_reject_qty),0) FROM `task_master_tbl` WHERE task_master_tbl.status=1" +
            " AND YEAR(task_date)=?1 AND MONTH(task_date)=?2 AND machine_id=?3 GROUP BY machine_id", nativeQuery = true)
    Double getRejectionCountMachineWise(String year, String month, String s);


    @Query(value = "SELECT job_name, operation_name, machine_number, SUM( machine_reject_qty), SUM(rework_qty), " +
            "SUM(doubtful_qty), SUM(un_machined_qty) FROM `task_view` WHERE task_date BETWEEN ?1 AND ?2 AND " +
            "(machine_reject_qty > 0 OR rework_qty > 0) GROUP BY job_operation_id", nativeQuery = true)
    List<Object[]> getRejectionItemListBetweenDates(String date1, String date2);

    @Query(value = "SELECT IFNULL(SUM(machine_reject_qty),0) FROM `task_master_tbl` WHERE task_master_tbl.status=1" +
            " AND YEAR(task_date)=?1 AND MONTH(task_date)=?2 AND job_id=?3 GROUP BY job_id", nativeQuery = true)
    Double getRejectionCountItemWise(String s, String s1, String toString);

//    @Query(value = "SELECT task_master_tbl.job_operation_id, job_operation_tbl.operation_name FROM `task_master_tbl`" +
//            " LEFT JOIN job_operation_tbl ON task_master_tbl.job_operation_id=job_operation_tbl.id WHERE" +
//            " task_master_tbl.status=1 AND machine_reject_qty > 0 AND task_date BETWEEN ?1 AND ?2 GROUP BY" +
//            " job_operation_id ORDER BY task_master_tbl.job_operation_id ASC", nativeQuery = true)
//    List<Object[]> getRejectionOperationListBetweenDates(String date1, String date2);

    @Query(value = "SELECT IFNULL(SUM(machine_reject_qty),0) FROM `task_master_tbl` WHERE task_master_tbl.status=1"+
            " AND YEAR(task_date)=?1 AND MONTH(task_date)=?2 AND job_operation_id=?3 GROUP BY job_operation_id", nativeQuery = true)
    Double getRejectionCountOperationWise(String s, String s1, String toString);

    @Query(value = "SELECT employee_id, job_name, operation_name, SUM( machine_reject_qty), " +
            "SUM(rework_qty), SUM(doubtful_qty), SUM(un_machined_qty) FROM `task_view` WHERE task_date BETWEEN ?1 AND ?2 AND " +
            "(machine_reject_qty > 0 OR rework_qty > 0) GROUP BY employee_id", nativeQuery = true)
    List<Object[]> getRejectionOperatorListBetweenDates(String date1, String date2);

    @Query(value = "SELECT IFNULL(SUM(machine_reject_qty),0) FROM `task_master_tbl` WHERE task_master_tbl.status=1"+
            " AND YEAR(task_date)=?1 AND MONTH(task_date)=?2 AND employee_id=?3 GROUP BY employee_id", nativeQuery = true)
    Double getRejectionCountOperatorWise(String s, String s1, String toString);

    List<TaskMaster> findByJobOperationIdAndTaskDateGreaterThanEqualAndTaskStatusAndTaskType(Long id, LocalDate effectiveDate, String completed, int i);
    List<TaskMaster> findByEmployeeIdAndAttendanceIdAndStatusOrderByIdDesc(Long id, Long id1, boolean b);
    TaskMaster findTop1ByAttendanceIdAndWorkBreakIdAndStatusAndWorkDone(Long id, Long id1, boolean b, boolean b1);

    @Query(value = "SELECT IFNULL(SUM(actual_work_time),0) FROM `task_master_tbl` WHERE attendance_id=?1 AND" +
            " work_done=?2 AND work_break_id!=?3 AND status=1", nativeQuery = true)
    double getSumOfAllBreaksWithNotWorking(Long id, int i, Long workBreakId);

    List<TaskMaster> findByAttendanceIdAndWorkDoneAndStatus(Long id, boolean b, boolean b1);

    @Query(value = "SELECT * FROM `task_master_tbl` WHERE employee_id=?1 AND task_date=?2 AND " +
            "task_type=?3 AND task_status=?4 AND status =?5", nativeQuery = true)
    List<TaskMaster> findEmployeeTaskListForToday(long employeeId, LocalDate now, int taskType, String s, boolean b);

    List<TaskMaster> findByAttendanceIdAndTaskTypeAndStatus(Long attendanceId, int i, boolean b);

    @Query(value = "SELECT IFNULL(SUM(total_time),0) FROM `task_master_tbl` WHERE attendance_id=?1 AND work_break_id=?2" +
            " AND status=?3 AND work_done=?4", nativeQuery = true)
    double getSumOfLunchTime(Long id, Long id1, boolean b, boolean b1);

    List<TaskMaster> findByAttendanceIdAndTaskTypeAndStatusAndEndTimeNotNull(Long attendanceId, int i, boolean b);

    List<TaskMaster> findByAttendanceIdAndTaskTypeAndStatusAndTaskStatus(Long attendanceId, int i, boolean b, String complete);

    List<TaskMaster> findByTaskMasterIdAndStatusAndWorkDone(Long id, boolean b, boolean b1);

    @Query(value = "SELECT id FROM `task_master_tbl` WHERE attendance_id=?1 ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Long getCurrentBreakId(Long attendanceId);
}
