package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.Allowance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AllowanceRepository extends JpaRepository<Allowance, Long> {
    Allowance findByIdAndStatus(Long id, boolean b);

    @Query(value = "SELECT IFNULL(SUM(amount),0) FROM `allowance_tbl` WHERE status=1", nativeQuery = true)
    double getSumOfAllowance();
}
