package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.Deduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DeductionRepository extends JpaRepository<Deduction, Long> {
    Deduction findByIdAndStatus(Long id, boolean b);

    @Query(value = "SELECT IFNULL(SUM(amount),0) FROM `deduction_tbl` WHERE status=1", nativeQuery = true)
    double getSumOfDeduction();
    @Query(value = "SELECT * FROM `deduction_tbl` WHERE status=1 and deduction_status=1", nativeQuery = true)
    List<Deduction> findAllByStatus();
}
