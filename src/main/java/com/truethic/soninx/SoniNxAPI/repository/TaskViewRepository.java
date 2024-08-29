package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.views.TaskView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TaskViewRepository extends JpaRepository<TaskView, Long> {
    List<TaskView> findByEmployeeIdAndTaskDateOrderById(Long employeeId, LocalDate now);

    @Query(value = "SELECT job_operation_id, operation_name, IFNULL(SUM(required_production), 0)," +
            " IFNULL(SUM(actual_production), 0), IFNULL(SUM(total_count), 0), IFNULL(SUM(ok_qty), 0)," +
            " IFNULL(SUM(rework_qty), 0), IFNULL(SUM(machine_reject_qty), 0), IFNULL(SUM(doubtful_qty), 0)," +
            " IFNULL(SUM(un_machined_qty), 0), IFNULL(SUM(actual_work_time), 0), IFNULL(SUM(total_time), 0) FROM" +
            " `task_view` WHERE employee_id=?1 AND attendance_id=?2 AND task_type=?3 AND status=?4 GROUP BY job_operation_id",
            nativeQuery = true)
    List<Object[]> findDataGroupByOperation(Long employeeId, Long id, String s, boolean b);


    @Query(value = "SELECT DISTINCT(job_operation_id), operation_name, machine_name, machine_number, job_name," +
            " cycle_time, pcs_rate FROM `task_view` WHERE employee_id=?1 AND attendance_id=?2 AND job_operation_id=?3 AND status=?4",
            nativeQuery = true)
    Object[] findTaskData(Long employeeId, Long id, String toString, boolean b);

    @Query(value = "SELECT DISTINCT(work_break_id),break_name, IFNULL(SUM(actual_work_time), 0)," +
            " IFNULL(SUM(total_time, 0)) FROM `task_view` WHERE employee_id=?1 AND attendance_id=?2 AND task_type=?3 AND status=?4" +
            " GROUP BY work_break_id", nativeQuery = true)
    List<Object[]> findBreakDataGroupByOperation(Long employeeId, Long id, String s, boolean b);



    @Query(value = "SELECT machine_number,job_name, operation_name,IFNULL(cycle_time, 0),IFNULL(SUM(ok_qty), 0), IFNULL(SUM(prod_working_hour), 0)," +
            " IFNULL(SUM(setting_time_in_min), 0),IFNULL(SUM(setting_time_in_hour), 0), IFNULL(SUM(working_hour_with_setting), 0)," +
            " IFNULL(average_per_shift,0), IFNULL(per_job_point,0)," +
            " IFNULL(SUM(prod_point), 0), IFNULL(SUM(setting_time_point), 0), IFNULL(SUM(total_point), 0)," +
            " IFNULL(wages_per_point, 0), IFNULL(SUM(wages_point_basis), 0), IFNULL(pcs_rate, 0), IFNULL(SUM(wages_pcs_basis), 0)," +
            " job_operation_id, IFNULL(SUM(percentage_of_task),0), COUNT(task_view.id), IFNULL(SUM(break_wages), 0)" +
            " FROM `task_view` WHERE employee_id=?1" +
            " AND attendance_id=?2 AND task_type=?3 AND status=?4 GROUP BY job_operation_id, job_id", nativeQuery = true)
    List<Object[]> findDataGroupByOperationView(Long employeeId, Long attendanceId, String s, boolean b);

    List<TaskView> findByAttendanceIdAndJobOperationId(Long attendanceId, Long jobOperationId);

    @Query(value = "SELECT IFNULL(SUM(total_time), 0) FROM task_view WHERE task_type=2 AND job_operation_id=?1", nativeQuery = true)
    Double findBreakTimeByJobOperationId(String toString);

    @Query(value = "SELECT work_break_id, break_tbl.break_name, IFNULL(SUM(actual_work_time),0), IFNULL(SUM(total_time),0)," +
            " IFNULL(SUM(break_wages),0) FROM task_master_tbl LEFT JOIN break_tbl ON task_master_tbl.work_break_id=break_tbl.id" +
            " WHERE attendance_id=?1 AND task_type=?2 AND task_master_tbl.status=?3 GROUP BY work_break_id", nativeQuery = true)
    List<Object[]> findDataGroupByBreaks(Long id, int i, boolean b);

    @Query(value = "SELECT work_break_id, break_tbl.break_name, IFNULL(SUM(actual_work_time),0), IFNULL(SUM(total_time),0)," +
            " IFNULL(SUM(break_wages),0) FROM task_master_tbl LEFT JOIN break_tbl ON task_master_tbl.work_break_id=break_tbl.id" +
            " WHERE attendance_id=?1 AND task_type=?2 AND task_master_tbl.task_date=?3 AND task_master_tbl.status=?4 GROUP BY work_break_id", nativeQuery = true)
    List<Object[]> findDataByTaskDateAndGroupByBreaks(Long id, int i, String date, boolean b);

    List<TaskView> findByAttendanceIdAndJobOperationIdAndStatus(Long id, Long valueOf, boolean b);

    List<TaskView> findByAttendanceIdAndTaskTypeAndStatus(Long id, int i, boolean b);

    List<TaskView> findByAttendanceIdAndWorkBreakIdAndStatus(Long id, Long valueOf, boolean b);

    List<TaskView> findByEmployeeIdAndAttendanceIdAndStatusOrderById(Long employeeId, Long id, boolean b);

    @Query(value = "SELECT employee_id, employee_name, cycle_time, IFNULL(SUM(total_time),0), IFNULL(SUM(actual_work_time),0)," +
            " IFNULL(SUM(total_count),0), IFNULL(SUM(required_production),0), IFNULL(SUM(actual_production),0)," +
            " operation_name, remark FROM task_view WHERE machine_id=?1 " +
            "AND job_id=?2 AND task_date BETWEEN ?3 AND ?4 AND status=?5 GROUP BY employee_id", nativeQuery = true)
    List<Object[]> findMachineViewData(Long valueOf, Long jobId, LocalDate parse, LocalDate parse1, boolean b);

    @Query(value = "SELECT employee_id, employee_name, cycle_time, IFNULL(SUM(total_time),0), IFNULL(SUM(actual_work_time),0)," +
            " IFNULL(SUM(total_count),0), IFNULL(SUM(required_production),0), IFNULL(SUM(actual_production),0), operation_name" +
            " FROM task_view WHERE job_id=?1 AND job_operation_id=?2 AND task_date BETWEEN ?3 AND ?4 AND status=?5 GROUP BY employee_id", nativeQuery = true)
    List<Object[]> findTaskViewData(Long valueOf, Long jobOperationId, LocalDate fromDate, LocalDate toDate, boolean b);

    @Query(value = "SELECT machine_id, machine_tbl.name, IFNULL(SUM(actual_work_time),0), IFNULL(SUM(total_time),0)," +
            " IFNULL(SUM(break_wages),0) FROM task_view LEFT JOIN machine_tbl ON task_view.machine_id=machine_tbl.id" +
            " WHERE attendance_id=?1 AND task_type=?2 AND task_view.status=?3 GROUP BY machine_id", nativeQuery = true)
    List<Object[]> findDataGroupByMachines(Long id, int i, boolean b);

    List<TaskView> findByAttendanceIdAndMachineIdAndStatus(Long id, Long valueOf, boolean b);
}
