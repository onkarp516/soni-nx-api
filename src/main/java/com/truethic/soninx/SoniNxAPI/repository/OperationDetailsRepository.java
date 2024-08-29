package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.OperationDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface OperationDetailsRepository extends JpaRepository<OperationDetails, Long> {


    List<OperationDetails> findByJobOperationIdAndStatus(Long id, boolean b);

    OperationDetails findByIdAndStatus(long id, boolean b);

    @Query(value = "SELECT operation_details_tbl.id, operation_details_tbl.cycle_time, operation_details_tbl.average_per_shift," +
            " operation_details_tbl.point_per_job, operation_details_tbl.pcs_rate, operation_details_tbl.operation_break_in_min," +
            " operation_details_tbl.effective_date, operation_details_tbl.operation_diameter_type  FROM `operation_details_tbl`" +
            " LEFT JOIN job_operation_tbl ON operation_details_tbl.job_operation_id=job_operation_tbl.id WHERE" +
            " job_operation_id=?1 AND effective_date<=?2 ORDER BY effective_date DESC LIMIT 1", nativeQuery = true)
    String getOperationDetailsByOperationId(Long id, LocalDate now);
}
