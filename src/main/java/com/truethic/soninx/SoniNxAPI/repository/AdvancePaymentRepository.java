package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.AdvancePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AdvancePaymentRepository extends JpaRepository<AdvancePayment, Long> {
    List<AdvancePayment> findByStatus(boolean b);

    List<AdvancePayment> findByEmployeeIdAndStatus(Long id, boolean b);

    AdvancePayment findByIdAndStatus(Long paymentId, boolean b);

    List<AdvancePayment> findByEmployeeIdAndStatusOrderByIdDesc(Long id, boolean b);

    @Query(value = "SELECT IFNULL(SUM(paid_amount),0) FROM `advance_payment_tbl` WHERE employee_id='?1' AND YEAR" +
            "(date_of_request)='?2' AND MONTH(date_of_request)='?3' AND payment_status='Approved' AND status=1", nativeQuery = true)
    double getEmployeeAdvanceOfMonth(Long employeeId, int year, int monthValue);
}
