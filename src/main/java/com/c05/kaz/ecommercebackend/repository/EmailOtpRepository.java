package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.EmailOtp;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.EmailOtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    Optional<EmailOtp> findTopByUserAndCodeAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            UserAccount user,
            String code,
            EmailOtpPurpose purpose
    );
}
