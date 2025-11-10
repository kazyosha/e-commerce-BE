package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.EmployeeAccountRequest;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.AccountStatus;
import com.c05.kaz.ecommercebackend.enums.SocialProvider;
import com.c05.kaz.ecommercebackend.enums.UserType;
import com.c05.kaz.ecommercebackend.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;


    @Transactional(readOnly = true)
    public Page<UserAccount> getUsers(int page, int size, String search) {
        PageRequest pageable = PageRequest.of(page, size);

        if (search == null || search.isBlank()) {
            return userAccountRepository.findAll(pageable);
        }

        return userAccountRepository
                .findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(
                        search.trim(), search.trim(), pageable
                );
    }


    @Transactional
    public void createEmployee(EmployeeAccountRequest req) {
        if (userAccountRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }
        if (userAccountRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        UserAccount user = UserAccount.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode("123456@Abc")) // default
                .email(req.getEmail())
                .userType(UserType.HR)       // hoặc ADMIN/STAFF tuỳ quy ước
                .status(AccountStatus.ACTIVE)
                .provider(SocialProvider.LOCAL)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userAccountRepository.save(user);
    }


    @Transactional
    public void resetDefaultPassword(Long id) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        user.setPassword(passwordEncoder.encode("123456@Abc"));
        user.setUpdatedAt(LocalDateTime.now());

        userAccountRepository.save(user);
    }


    @Transactional
    public void blockUser(Long id) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        user.setStatus(AccountStatus.BLOCKED);
        user.setUpdatedAt(LocalDateTime.now());

        userAccountRepository.save(user);
    }


    @Transactional
    public void activeUser(Long id) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        user.setStatus(AccountStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());

        userAccountRepository.save(user);
    }
}
