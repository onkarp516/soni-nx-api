package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.RoleMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleMasterRepository extends JpaRepository<RoleMaster, Long> {

    RoleMaster findByIdAndStatus(long parseLong, boolean b);
    List<RoleMaster> findByStatus(Boolean i);
    List<RoleMaster> findByInstituteIdAndStatus(long instituteId, Boolean i);
    RoleMaster findRoleMasterById(long userRole);
    RoleMaster findByRoleNameAndStatus(String roleName, boolean b);
}
