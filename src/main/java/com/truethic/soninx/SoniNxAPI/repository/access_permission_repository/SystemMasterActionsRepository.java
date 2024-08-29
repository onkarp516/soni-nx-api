package com.truethic.soninx.SoniNxAPI.repository.access_permission_repository;

import com.truethic.soninx.SoniNxAPI.model.access_permissions.SystemMasterActions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemMasterActionsRepository extends JpaRepository<SystemMasterActions,Long> {
    List<SystemMasterActions> findByStatus(boolean b);

    SystemMasterActions findByIdAndStatus(long parseLong, boolean b);
}
