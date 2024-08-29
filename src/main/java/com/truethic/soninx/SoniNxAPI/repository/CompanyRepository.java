package com.truethic.soninx.SoniNxAPI.repository;


import com.truethic.soninx.SoniNxAPI.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findAllByStatus(boolean b);
    List<Company> findAllByInstituteIdAndStatus(long instituteId, boolean b);

    Company findByIdAndStatus(long id, boolean b);
}
