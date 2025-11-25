package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.auth.AuthResponseAll;
import com.c05.kaz.ecommercebackend.dto.auth.UserDTO;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.AccountStatus;
import com.c05.kaz.ecommercebackend.enums.SocialProvider;
import com.c05.kaz.ecommercebackend.enums.UserType;
import com.c05.kaz.ecommercebackend.repository.RoleRepository;
import com.c05.kaz.ecommercebackend.repository.UserAccountRepository;
import com.c05.kaz.ecommercebackend.security.JwtService;
import com.c05.kaz.ecommercebackend.services.OtpService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;
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
    private final OtpService otpService; // <-- dùng service mới

    // ================== ĐĂNG KÝ ==================
    @PostMapping("/register/customer")
    public ResponseEntity<?> registerCustomer(@RequestBody RegisterRequest request) {
        return registerUser(request, UserType.CUSTOMER, "CUSTOMER");
    }

    @PostMapping("/register/supplier")
    public ResponseEntity<?> registerSupplier(@RequestBody RegisterRequest request) {
        return registerUser(request, UserType.SUPPLIER, "SUPPLIER");
    }

    // ================== LOGIN ==================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getIdentifier(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Sai tài khoản hoặc mật khẩu");
        } catch (LockedException e) {
            return ResponseEntity.status(403).body("Tài khoản đã bị khóa");
        } catch (DisabledException e) {
            return ResponseEntity.status(403).body("Tài khoản đang bị vô hiệu hóa");
        }

        UserAccount user = userAccountRepository
                .findByUsernameOrEmail(request.getIdentifier(), request.getIdentifier())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            return ResponseEntity.status(403).body("Tài khoản không ở trạng thái hoạt động");
        }

        var springUser = User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRoles().stream().map(r -> "ROLE_" + r.getCode()).toArray(String[]::new))
                .build();

        String token = jwtService.generateToken(springUser, user.getId());

        return ResponseEntity.ok(
                new AuthResponseAll(
                        token,
                        new UserDTO(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail(),
                                user.getUserType().name(),
                                user.getRoles()
                        )
                )
        );
    }

    // ================== QUÊN MẬT KHẨU ==================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        var userOpt = userAccountRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email không tồn tại trong hệ thống"));
        }
        var user = userOpt.get();

        // Tạo + lưu + gửi OTP qua OtpService
        otpService.sendForgotPasswordOtp(user);
        return ResponseEntity.ok(Map.of("message", "Đã gửi mã OTP đến email " + request.getEmail()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        var user = userAccountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        boolean ok = otpService.verifyForgotPasswordOtp(user, request.getOtp());
        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã OTP không hợp lệ hoặc đã hết hạn"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userAccountRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công"));
    }

    // ================== HÀM DÙNG CHUNG ==================
    private ResponseEntity<?> registerUser(RegisterRequest request, UserType userType, String roleCode) {
        if (userAccountRepository.existsByEmail(request.getEmail()))
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email đã tồn tại", "code", "EMAIL_EXISTS"));

        if (userAccountRepository.existsByUsername(request.getUsername()))
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username đã tồn tại", "code", "USERNAME_EXISTS"));

        var role = roleRepository.findByCode(roleCode);
        if (role == null)
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Role " + roleCode + " chưa tồn tại", "code", "ROLE_NOT_FOUND"));

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

        String token = jwtService.generateToken(
                User.withUsername(user.getUsername())
                        .password(user.getPassword())
                        .authorities("ROLE_" + roleCode)
                        .build(),
                user.getId()
        );

        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getUserType().name()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request,
                                            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Bạn chưa đăng nhập");
        }

        String username = authentication.getName();

        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Mật khẩu hiện tại không đúng");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Mật khẩu mới không được trùng mật khẩu hiện tại");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userAccountRepository.save(user);

        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }


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
    public static class ForgotPasswordRequest {
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        private String email;
        private String otp;
        private String newPassword;
    }

    @Data
    public static class AuthResponse {
        private final String token;
        private final String username;
        private final String userType;
    }

    @Data
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
    }

}
