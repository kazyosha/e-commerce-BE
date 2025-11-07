package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.PasswordResetToken;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUser(UserAccount user);
}
