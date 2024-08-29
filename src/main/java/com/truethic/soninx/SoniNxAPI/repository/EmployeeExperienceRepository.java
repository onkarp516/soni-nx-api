package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.EmployeeExperienceDetails;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;

public interface EmployeeExperienceRepository extends JpaRepository<EmployeeExperienceDetails, Long> {
    EmployeeExperienceDetails findByIdAndStatus(long id, boolean b);

    @Modifying
    @Transactional
    @Cascade(CascadeType.DELETE)
    @Query(value = "DELETE FROM employee_experience_details_tbl WHERE id=?1", nativeQuery = true)
    void deleteExperienceFromEmployee(long id);
}
