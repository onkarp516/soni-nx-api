package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.Machine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface MachineRepository extends JpaRepository<Machine, Long> {
    List<Machine> findAllByStatus(boolean b);
    List<Machine> findAllByInstituteIdAndStatus(long instituteId, boolean b);

    Page<Machine> findByStatusOrderByIdDesc(Pageable pageable, boolean b);

    Machine findByIdAndStatus(long id, boolean b);

    @Query(value = "SELECT IFNULL(SUM(task_master_tbl.required_production),0)," +
            " IFNULL(SUM(task_master_tbl.actual_production),0) FROM task_master_tbl WHERE task_master_tbl.machine_id=?2 AND task_master_tbl.institute_id=?3" +
            " AND task_master_tbl.task_date=?1",
            nativeQuery = true)
    List<Object[]> getRequiredAndActualProductionCount(LocalDate todayDate, Long id, long institute_id);

    @Query(value = "SELECT DISTINCT(machine_tbl.id), machine_tbl.name, machine_tbl.number FROM `machine_tbl` LEFT JOIN" +
            " task_master_tbl ON machine_tbl.id=task_master_tbl.machine_id WHERE task_master_tbl.task_date=?1 AND" +
            " machine_tbl.status=?2 AND machine_tbl.institute_id=?3",
            nativeQuery = true)
    List<Object[]> findOnlyWorkingMachines(LocalDate todayDate, boolean b, long institute_id);


    @Query(value = "SELECT task_master_tbl.machine_id, machine_tbl.name, task_master_tbl.job_id, job_tbl.job_name," +
            " SUM(ok_qty), machine_tbl.number FROM `task_master_tbl` LEFT JOIN machine_tbl ON task_master_tbl.machine_id=machine_tbl.id" +
            " LEFT JOIN job_tbl ON task_master_tbl.job_id=job_tbl.id WHERE ok_qty>0 AND task_date BETWEEN ?1 AND ?2 AND task_master_tbl.status=?3 GROUP BY machine_id, job_id",
            nativeQuery = true)
    List<Object[]> getMachineReport(String fromDate, String toDate, boolean b);

    @Query(value = "SELECT DISTINCT(machine_tbl.id), machine_tbl.name, machine_tbl.number " +
            " FROM `machine_tbl` LEFT JOIN task_master_tbl ON machine_tbl.id=task_master_tbl.machine_id LEFT JOIN" +
            " attendance_tbl ON task_master_tbl.attendance_id=attendance_tbl.id WHERE task_master_tbl.task_date=?1" +
            " AND machine_tbl.status=?2 AND attendance_tbl.shift_id=?3 AND machine_tbl.institute_id=?4", nativeQuery = true)
    List<Object[]> findOnlyWorkingMachinesByShift(LocalDate todayDate, boolean b, String shiftId, long institute_id);

    @Query(value = "SELECT task_master_tbl.machine_id, machine_tbl.name, task_master_tbl.job_id, job_tbl.job_name," +
            " SUM(ok_qty), machine_tbl.number FROM `task_master_tbl` LEFT JOIN machine_tbl ON task_master_tbl.machine_id=machine_tbl.id" +
            " LEFT JOIN job_tbl ON task_master_tbl.job_id=job_tbl.id WHERE ok_qty>0 AND task_date BETWEEN ?1 AND ?2 " +
            " AND machine_id=?3 AND task_master_tbl.status=?4 GROUP BY machine_id, job_id",
            nativeQuery = true)
    List<Object[]> getMachineReportByMachine(String fromDate, String toDate, String machineId, boolean b);
}
