package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.AccountStatus;
import com.c05.kaz.ecommercebackend.enums.SocialProvider;
import com.c05.kaz.ecommercebackend.enums.UserType;
import com.c05.kaz.ecommercebackend.repository.RoleRepository;
import com.c05.kaz.ecommercebackend.repository.UserAccountRepository;
import com.c05.kaz.ecommercebackend.security.JwtService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    // ================== ĐĂNG KÝ KHÁCH HÀNG ==================

    @PostMapping("/register/customer")
    public ResponseEntity<?> registerCustomer(@RequestBody RegisterRequest request) {
        return registerUser(request, UserType.CUSTOMER, "CUSTOMER");
    }

    // ================== ĐĂNG KÝ NHÀ PHÂN PHỐI ==================

    @PostMapping("/register/supplier")
    public ResponseEntity<?> registerDistributor(@RequestBody RegisterRequest request) {
        return registerUser(request, UserType.SUPPLIER, "SUPPLIER");
    }

    // ================== LOGIN DÙNG CHUNG ==================
    // identifier = username hoặc email
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            request.getIdentifier(),
                            request.getPassword()
                    );

            authenticationManager.authenticate(authToken);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Sai tài khoản hoặc mật khẩu");
        } catch (LockedException e) {
            return ResponseEntity.status(403).body("Tài khoản đã bị khóa");
        } catch (DisabledException e) {
            return ResponseEntity.status(403).body("Tài khoản đang bị vô hiệu hóa");
        }

        // Lấy user theo username hoặc email
        UserAccount user = userAccountRepository
                .findByUsernameOrEmail(request.getIdentifier(), request.getIdentifier())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        // Có thể check trạng thái nếu cần
        if (user.getStatus() != AccountStatus.ACTIVE) {
            return ResponseEntity.status(403).body("Tài khoản không ở trạng thái hoạt động");
        }

        var springUser = User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(
                        user.getRoles().stream()
                                .map(r -> "ROLE_" + r.getCode())
                                .toArray(String[]::new)
                )
                .build();

        String token = jwtService.generateToken(springUser);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUsername(user.getUsername());
        response.setUserType(user.getUserType().name());

        return ResponseEntity.ok(response);
    }

    // ================== HÀM DÙNG CHUNG ==================

    private ResponseEntity<?> registerUser(RegisterRequest request, UserType userType, String roleCode) {
        if (userAccountRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email đã tồn tại");
        }
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body("Username đã tồn tại");
        }

        var role = roleRepository.findByCode(roleCode);
        if (role == null) {
            return ResponseEntity.badRequest().body("Role " + roleCode + " không tồn tại, hãy seed dữ liệu ROLE trước");
        }

        UserAccount user = UserAccount.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(userType)
                .status(AccountStatus.ACTIVE)
                .provider(SocialProvider.LOCAL)
                .roles(Set.of(role))
                .createdAt(LocalDateTime.now())
                .build();

        userAccountRepository.save(user);

        var springUser = User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + roleCode)
                .build();

        String token = jwtService.generateToken(springUser);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUsername(user.getUsername());
        response.setUserType(user.getUserType().name());

        return ResponseEntity.ok(response);
    }

    // ================== DTOs ==================

    @Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
    }

    @Data
    public static class LoginRequest {
        private String identifier; // username hoặc email
        private String password;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String username;
        private String userType;
    }
}
