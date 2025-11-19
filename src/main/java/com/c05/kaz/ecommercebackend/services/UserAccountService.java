package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.UserAccountDTO;
import com.c05.kaz.ecommercebackend.entity.EmployeeProfile;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.AccountStatus;
import com.c05.kaz.ecommercebackend.enums.SocialProvider;
import com.c05.kaz.ecommercebackend.enums.UserType;
import com.c05.kaz.ecommercebackend.repository.EmployeeProfileRepository;
import com.c05.kaz.ecommercebackend.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final PasswordEncoder passwordEncoder;


    @Transactional(readOnly = true)
    public Page<UserAccount> getUsers(int page, int size, String search, String userType) {
        PageRequest pageable = PageRequest.of(page, size);

        // Chuẩn hóa search
        String keyword = (search == null || search.isBlank())
                ? null
                : search.trim();

        // Map String -> Enum UserType (HR, ADMIN, CUSTOMER, ...)
        UserType type = null;
        if (userType != null && !userType.isBlank()) {
            try {
                type = UserType.valueOf(userType); // "HR" -> UserType.HR
            } catch (IllegalArgumentException e) {
                // Nếu FE gửi bậy thì coi như không filter
                type = null;
            }
        }

        return userAccountRepository.searchUsers(type, keyword, pageable);
    }


    @Transactional
    public void createEmployee(UserAccountDTO req) {
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
//                .fullName(req.getFullName())
                .userType(UserType.HR)       // hoặc ADMIN/STAFF tuỳ quy ước
                .status(AccountStatus.ACTIVE)
                .provider(SocialProvider.LOCAL)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userAccountRepository.save(user);

        EmployeeProfile profile = EmployeeProfile.builder()
                .user(user)
                .fullName(req.getFullName() != null ? req.getFullName() : "Nhân viên HR mới")
                .salary(req.getSalary() != null ? req.getSalary() : 0L)
                .phone("")                  // bỏ phone → cho chuỗi rỗng để tránh null
                .address("")                // nếu không dùng address
                .age(null)                  // nếu age không dùng luôn
                .createdAt(LocalDateTime.now())
                .build();

        employeeProfileRepository.save(profile);

        System.out.println("✅ Đã tạo HR mới: " + req.getUsername() + " (có profile đi kèm)");
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

    public UserAccount getCurrentCustomer() {
        // 1. Lấy Authentication từ SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("Người dùng chưa đăng nhập.");
        }

        // 2. Lấy username (hoặc email) từ auth
        String username = auth.getName(); // chính là username khi bạn build UserDetails

        // 3. Tìm UserAccount trong DB
        UserAccount user = userAccountRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + username));

        // 4. Lấy Customer từ UserAccount
        UserAccount customer = userAccountRepository.findCustomerById(user.getId());
        if (customer == null) {
            throw new RuntimeException("Tài khoản hiện tại không phải khách hàng.");
        }

        return customer;
    }

    public Map<String, Long> getUserStats() {
        long hr = userAccountRepository.countByUserType(UserType.HR);
        long customer = userAccountRepository.countByUserType(UserType.CUSTOMER);
        long supplier = userAccountRepository.countByUserType(UserType.SUPPLIER);

        return Map.of(
                "HR", hr,
                "CUSTOMER", customer,
                "SUPPLIER", supplier
        );
    }

}