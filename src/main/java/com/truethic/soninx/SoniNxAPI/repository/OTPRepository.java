package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.OTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;

public interface OTPRepository extends JpaRepository<OTP, Long> {
    OTP findTop1ByUsernameAndStatusOrderByIdDesc(String username, boolean b);

    @Modifying
    @Transactional
    @Query(value = "UPDATE otp_tbl as a SET a.status=0 WHERE a.username=?1", nativeQuery = true)
    void updateOtpStatus(String username);
}
