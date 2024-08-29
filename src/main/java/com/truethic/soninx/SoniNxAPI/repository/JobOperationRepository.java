package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.JobOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobOperationRepository extends JpaRepository<JobOperation, Long> {
    List<JobOperation> findAllByStatus(boolean b);

    Page<JobOperation> findByStatusOrderByIdDesc(Pageable pageable, boolean b);

    JobOperation findByIdAndStatus(long id, boolean b);

    List<JobOperation> findByJobIdAndStatus(Long jobId, boolean b);

    @Query(value = "SELECT DISTINCT(operation_name) FROM `job_operation_tbl` WHERE status=?1", nativeQuery = true)
    List<Object[]> getDistinctOperationNamesData(boolean b);
}
