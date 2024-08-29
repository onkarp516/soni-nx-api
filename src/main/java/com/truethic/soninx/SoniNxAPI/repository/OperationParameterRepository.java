package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.OperationParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OperationParameterRepository extends JpaRepository<OperationParameter, Long> {
    OperationParameter findByIdAndStatus(long parameterId, boolean b);

    Page<OperationParameter> findByStatusOrderByIdDesc(Pageable pageable, boolean b);

    List<OperationParameter> findByJobOperationIdAndStatus(Long jobOperationId, boolean b);

//    @Query(value = "SELECT id,specification, first_parameter, second_parameter FROM `operation_parameter_tbl` WHERE" +
//            " job_id=?1 AND job_operation_id=?2 AND status=?3", nativeQuery = true)
//    List<Object[]> getDrawingListByOperation(Long id, Long id1, boolean b);

    @Query(value = "SELECT DISTINCT(specification),inspection_id, drawing_size, first_parameter, second_parameter FROM `employee_inspection_tbl`" +
            " WHERE task_id=?1", nativeQuery = true)
    List<Object[]> getDrawingListByOperation(Long id);

    @Query(value = "SELECT DISTINCT(specification),inspection_id, drawing_size, first_parameter, second_parameter FROM `employee_inspection_tbl`" +
            " WHERE task_id=?1 AND supervisor_task_master_id=?2", nativeQuery = true)
    List<Object[]> getDrawingListByOperationForSupervisor(Long id, Long supervisorTaskId);
}
