package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.EmployeeInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeInspectionRepository extends JpaRepository<EmployeeInspection, Long> {
    EmployeeInspection findByIdAndStatus(String jobNo, boolean b);

    List<EmployeeInspection> findByTaskMasterIdAndStatus(Long taskId, boolean b);

    @Query(value = "SELECT job_no, employee_id, created_at, supervisor_task_master_id FROM `employee_inspection_tbl` WHERE task_id=?1 GROUP BY job_no", nativeQuery = true)
    List<Object[]> getJobNos(Long taskId);

    @Query(value = "SELECT job_no, employee_id, created_at FROM `employee_inspection_tbl` WHERE task_id=?1 AND" +
            " supervisor_task_master_id=?2 GROUP BY job_no", nativeQuery = true)
    List<Object[]> getJobNosForSupervisor(Long taskId, Long supervisorTaskId);

    List<EmployeeInspection> findByJobNoAndTaskMasterId(String toString, Long taskId);

    @Query(value = "SELECT DISTINCT(job_no) as job_no FROM `employee_inspection_tbl` WHERE task_id=?1 AND inspection_id=?2 AND status=?3", nativeQuery = true)
    List<Object[]> getJobNosListByTaskIdAndInspectionId(Long id, String toString, boolean b);

    @Query(value = "SELECT job_no, employee_id, created_at  FROM `employee_inspection_tbl` WHERE employee_id=?1 AND job_operation_id=?2 AND inspection_date=?3 GROUP BY job_no", nativeQuery = true)
    List<Object[]> getJobNosByEmployeeAndOperationAndDate(Long employeeId, Long jobOperationId, LocalDate now);

    List<EmployeeInspection> findByJobNoAndEmployeeIdAndInspectionDate(String toString, Long employeeId, LocalDate now);

    @Query(value = "SELECT inspection_id, specification,drawing_size FROM `employee_inspection_tbl` WHERE job_id=?1 AND job_operation_id=?2 AND institute_id=?5 AND" +
            " inspection_date BETWEEN ?3 AND ?4 GROUP BY drawing_size", nativeQuery = true)
    List<Object[]> getSpecificationByDateRageAndJobDetailsAndInstituteId(String jobId, String jobOperationId, String fromDate, String toDate,long institute_id);

    @Query(value = "SELECT inspection_id, specification,drawing_size FROM `employee_inspection_tbl` WHERE job_id=?1 AND job_operation_id=?2 AND institute_id=?6 AND" +
            " inspection_date BETWEEN ?4 AND ?5 AND employee_id=?3 GROUP BY drawing_size", nativeQuery = true)
    List<Object[]> getSpecificationByDateRageAndJobDetailsAndEmployeeAndInstituteId(String jobId, String jobOperationId, String employeeId, String fromDate, String toDate,long institute_id);

    @Query(value = "SELECT job_no FROM `employee_inspection_tbl` WHERE job_id=?1 AND job_operation_id=?2 AND institute_id=?5 AND" +
            " inspection_date BETWEEN ?3 AND ?4 GROUP BY job_no", nativeQuery = true)
    List<Object[]> getJobNosByDateRageAndJobDetailsAndInstituteId(String jobId, String jobOperationId, String fromDate, String toDate,long institute_id);

    @Query(value = "SELECT job_no FROM `employee_inspection_tbl` WHERE job_id=?1 AND job_operation_id=?2 AND institute_id=?5 AND" +
            " inspection_date BETWEEN ?4 AND ?5 AND employee_id=?3 GROUP BY job_no", nativeQuery = true)
    List<Object[]> getJobNosByDateRageAndJobDetailsAndEmployeeAndInstituteId(String jobId, String jobOperationId, String employeeId, String fromDate, String toDate,long institute_id);

    @Query(value = "SELECT actual_size, result FROM `employee_inspection_tbl` WHERE job_id=?1 AND job_operation_id=?2 AND" +
            " inspection_date BETWEEN ?4 AND ?5 AND job_no=?3 AND inspection_id=?6", nativeQuery = true)
    String getDataByDateRangeAndJobDetails(String jobId, String jobOperationId, String toString, String fromDate, String toDate, String s);

    @Query(value = "SELECT actual_size, result FROM `employee_inspection_tbl` WHERE job_id=?1 AND job_operation_id=?2 AND" +
            " inspection_date BETWEEN ?5 AND ?6 AND employee_id=?3 AND job_no=?4 AND inspection_id=?7", nativeQuery = true)
    String getDataByDateRangeAndJobDetailsAndEmployee(String jobId, String jobOperationId, String employeeId, String toString, String fromDate, String toDate, String s);

    @Query(value = "SELECT DISTINCT(machine_id), machine_tbl.name FROM `employee_inspection_tbl` LEFT JOIN machine_tbl ON" +
            " employee_inspection_tbl.machine_id=machine_tbl.id WHERE inspection_date BETWEEN ?1 AND ?2", nativeQuery = true)
    List<Object[]> getMachineDataByDateRange(String fromDate, String toDate);

    @Query(value = "SELECT DISTINCT(job_id), job_tbl.job_name FROM `employee_inspection_tbl` LEFT JOIN job_tbl ON" +
            " employee_inspection_tbl.job_id=job_tbl.id WHERE machine_id=?3 AND inspection_date BETWEEN ?1 AND ?2", nativeQuery = true)
    List<Object[]> getJobDataByDateRange(String fromDate, String toDate, String machineId);

    @Query(value = "SELECT DISTINCT(job_operation_id), job_operation_tbl.operation_name FROM `employee_inspection_tbl`" +
            " LEFT JOIN job_operation_tbl ON employee_inspection_tbl.job_operation_id=job_operation_tbl.id WHERE" +
            " employee_inspection_tbl.job_id=?3 AND inspection_date BETWEEN ?1 AND ?2", nativeQuery = true)
    List<Object[]> getJobOperationDataByDateRange(String fromDate, String toDate, String jobId);

    @Query(value = "SELECT DISTINCT(employee_id), employee_tbl.first_name,employee_tbl.last_name, employee_tbl.last_name" +
            " FROM `employee_inspection_tbl` LEFT JOIN employee_tbl ON employee_inspection_tbl.employee_id=employee_tbl.id" +
            " WHERE inspection_date BETWEEN ?1 AND ?2", nativeQuery = true)
    List<Object[]> getEmployeeDataByDateRange(String fromDate, String toDate);
}
