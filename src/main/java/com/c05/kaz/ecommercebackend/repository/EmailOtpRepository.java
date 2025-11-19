package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.EmailOtp;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.EmailOtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    Optional<EmailOtp> findTopByUserAndCodeAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            UserAccount user,
            String code,
            EmailOtpPurpose purpose
    );

    @Modifying
    @Transactional
    @Query("update EmailOtp o set o.used = true where o.user = :user and o.purpose = :purpose and o.used = false")
    void invalidateAllUnusedByUserAndPurpose(@Param("user") UserAccount user, @Param("purpose") EmailOtpPurpose purpose);
}
