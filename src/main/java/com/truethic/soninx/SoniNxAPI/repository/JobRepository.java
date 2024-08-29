package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    Job findByIdAndStatus(long jobId, boolean b);

    Page<Job> findByStatusOrderByIdDesc(Pageable pageable, boolean b);

    List<Job> findAllByStatus(boolean b);
    List<Job> findAllByInstituteIdAndStatus(long instituteId, boolean b);


    @Query(value = "SELECT IFNULL(SUM(machine_reject_qty),0), IFNULL(SUM(rework_qty),0), IFNULL(SUM(doubtful_qty),0)" +
            " FROM `task_master_tbl` WHERE task_master_tbl.task_date=?1 AND task_master_tbl.job_id=?2 AND task_master_tbl.institute_id=?3", nativeQuery = true)
    List<Object[]> getItemWiseCounts(LocalDate todayDate, Long b,long institute_id);

    @Query(value = "SELECT job_tbl.id, job_tbl.job_name, IFNULL(SUM(task_master_tbl.machine_reject_qty),0)," +
            " IFNULL(SUM(task_master_tbl.rework_qty),0), IFNULL(SUM(task_master_tbl.doubtful_qty),0) FROM" +
            " `task_master_tbl` LEFT JOIN job_tbl ON task_master_tbl.job_id=job_tbl.id WHERE" +
            " job_tbl.status=1 AND task_master_tbl.institute_id=?2 AND task_master_tbl.task_date=?1 AND (task_master_tbl.machine_reject_qty>0 OR" +
            " task_master_tbl.rework_qty>0 OR task_master_tbl.doubtful_qty>0) GROUP BY job_tbl.id", nativeQuery = true)
    List<Object[]> getItemListWithCounts(LocalDate todayDate, long institute_id);

    @Query(value = "SELECT DISTINCT(job_tbl.id), job_tbl.job_name FROM `job_tbl` LEFT JOIN task_master_tbl ON" +
            " job_tbl.id=task_master_tbl.job_id WHERE task_master_tbl.task_date=?1 AND job_tbl.status=?2 AND job_tbl.institute_id=?3 AND" +
            " (task_master_tbl.machine_reject_qty!=0 OR task_master_tbl.rework_qty!=0 OR task_master_tbl.doubtful_qty!=0)",
            nativeQuery = true)
    List<Object[]> findOnlyWorkingItems(LocalDate todayDate, boolean b, long institute_id);

    @Query(value = "SELECT task_master_tbl.job_id, job_tbl.job_name, job_operation_id, job_operation_tbl.operation_name," +
            " SUM(ok_qty) FROM `task_master_tbl` LEFT JOIN job_tbl ON task_master_tbl.job_id=job_tbl.id LEFT JOIN" +
            " job_operation_tbl ON task_master_tbl.job_operation_id=job_operation_tbl.id WHERE ok_qty>0 AND" +
            " task_date BETWEEN ?1 AND ?2 AND task_master_tbl.status=?3 GROUP BY task_master_tbl.job_operation_id", nativeQuery = true)
    List<Object[]> getItemReports(LocalDate localDate, LocalDate toDate, boolean b);

    @Query(value = "SELECT DISTINCT(job_tbl.id), job_tbl.job_name FROM `job_tbl` LEFT JOIN task_master_tbl ON" +
            " job_tbl.id=task_master_tbl.job_id LEFT JOIN attendance_tbl ON task_master_tbl.attendance_id=attendance_tbl.id" +
            " WHERE task_master_tbl.task_date=?1 AND job_tbl.status=?2 AND attendance_tbl.shift_id=?3 AND task_master_tbl.institute_id=?4 AND" +
            " (task_master_tbl.machine_reject_qty!=0 OR task_master_tbl.rework_qty!=0 OR" +
            " task_master_tbl.doubtful_qty!=0)", nativeQuery = true)
    List<Object[]> findOnlyWorkingItemsByShift(LocalDate todayDate, boolean b, String shiftId, long institute_id);

    @Query(value = "SELECT job_tbl.id, job_tbl.job_name, IFNULL(SUM(task_master_tbl.machine_reject_qty),0)," +
            " IFNULL(SUM(task_master_tbl.rework_qty),0), IFNULL(SUM(task_master_tbl.doubtful_qty),0) FROM" +
            " `task_master_tbl` LEFT JOIN job_tbl ON task_master_tbl.job_id=job_tbl.id LEFT JOIN attendance_tbl ON" +
            " task_master_tbl.attendance_id=attendance_tbl.id WHERE job_tbl.status=1 AND task_master_tbl.task_date=?1 AND" +
            " attendance_tbl.shift_id=?2 AND task_master_tbl.institute_id=?3 AND (task_master_tbl.machine_reject_qty>0 OR task_master_tbl.rework_qty>0 OR" +
            " task_master_tbl.doubtful_qty>0) GROUP BY job_tbl.id", nativeQuery = true)
    List<Object[]> getItemListWithCountsByShift(LocalDate todayDate, String shiftId, long institute_id);

    @Query(value = "SELECT task_master_tbl.job_id, job_operation_id, job_operation_tbl.operation_name, job_operation_tbl.operation_no," +
            " employee_id, employee_tbl.first_name, IFNULL(employee_tbl.middle_name, ''), IFNULL(employee_tbl.last_name, ''), machine_reject_qty," +
            " rework_qty, doubtful_qty FROM `task_master_tbl` LEFT JOIN employee_tbl ON task_master_tbl.employee_id=employee_tbl.id" +
            " LEFT JOIN job_operation_tbl ON task_master_tbl.job_operation_id=job_operation_tbl.id WHERE task_master_tbl.job_id=?2" +
            " AND task_master_tbl.task_date=?1 AND task_master_tbl.institute_id=?3 AND (task_master_tbl.machine_reject_qty>0 OR task_master_tbl.rework_qty>0 OR task_master_tbl.doubtful_qty>0)", nativeQuery = true)
    List<Object[]> getEmpWithItemCounts(LocalDate todayDate, String toString, long institute_id);

    @Query(value = "SELECT task_master_tbl.job_id, job_tbl.job_name, job_operation_id, job_operation_tbl.operation_name," +
            " SUM(ok_qty) FROM `task_master_tbl` LEFT JOIN job_tbl ON task_master_tbl.job_id=job_tbl.id LEFT JOIN" +
            " job_operation_tbl ON task_master_tbl.job_operation_id=job_operation_tbl.id WHERE ok_qty>0 AND" +
            " task_date BETWEEN ?1 AND ?2 AND task_master_tbl.job_id=?3 AND task_master_tbl.status=?4 GROUP BY task_master_tbl.job_operation_id", nativeQuery = true)
    List<Object[]> getItemReportsByJobId(LocalDate fromDate, LocalDate toDate, String jobId, boolean b);
}
