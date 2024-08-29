package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<Users, Long> {
    Users findByUsername(String userName);

    List<Users> findByUserRoleIgnoreCaseAndStatus(String cadmin, boolean b);
    List<Users> findByStatus(boolean b);
    Users findByIdAndStatus(long id, boolean b);
    Users findByUsernameAndStatus(String username, boolean b);
}
