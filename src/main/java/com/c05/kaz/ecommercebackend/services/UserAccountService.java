package com.c05.kaz.ecommercebackend.services;
import com.c05.kaz.ecommercebackend.dto.EmployeeAccountRequest;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.AccountStatus;
import com.c05.kaz.ecommercebackend.enums.SocialProvider;
import com.c05.kaz.ecommercebackend.enums.UserType;
import com.c05.kaz.ecommercebackend.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public void createEmployee(EmployeeAccountRequest req) {
        if (userAccountRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }
        if (userAccountRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        UserAccount user = UserAccount.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode("123456@Abc")) // luôn set default ở backend
                .email(req.getEmail())
                .userType(UserType.HR)
                .status(AccountStatus.ACTIVE)
                .provider(SocialProvider.LOCAL)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .build();

        userAccountRepository.save(user);
    }

    public void resetDefaultPassword(Long id) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        user.setPassword(passwordEncoder.encode("123456@Abc"));
        user.setUpdatedAt(LocalDateTime.now());

        userAccountRepository.save(user);
    }
}