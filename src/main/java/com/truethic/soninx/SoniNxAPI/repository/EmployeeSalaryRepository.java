package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.EmployeeSalary;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.time.LocalDate;

public interface EmployeeSalaryRepository extends JpaRepository<EmployeeSalary, Long> {
    EmployeeSalary findByIdAndStatus(long empSalId, boolean b);

    @Modifying
    @Transactional
    @Cascade(CascadeType.DELETE)
    @Query(value = "DELETE FROM employee_salary_tbl WHERE id=?1", nativeQuery = true)
    void deleteSalaryFromEmployee(Long id);

    @Query(value = "SELECT IFNULL(salary, 0) FROM employee_salary_tbl  WHERE employee_id=?1 AND effective_date<=?2" +
            " ORDER BY effective_date DESC LIMIT 1", nativeQuery = true)
    Double getEmployeeSalary(Long employeeId, LocalDate now);
}
