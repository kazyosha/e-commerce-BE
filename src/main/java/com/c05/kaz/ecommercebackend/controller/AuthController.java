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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private final JavaMailSender mailSender; // ✅ Inject JavaMailSender

    // Lưu OTP tạm thời vào memory, không vào DB
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

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

        String token = jwtService.generateToken(springUser);

        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getUserType().name()));
    }

    // ================== QUÊN MẬT KHẨU ==================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        var userOpt = userAccountRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Email không tồn tại trong hệ thống");
        }

        try {
            // Tạo + lưu + gửi OTP (dùng hàm dùng chung)
            generateAndSendOtp(
                    request.getEmail(),
                    "Mã OTP đặt lại mật khẩu",
                    "Mã OTP của bạn là: "
            );

            return ResponseEntity.ok("Đã gửi mã OTP đến email " + request.getEmail());
        } catch (Exception e) {
            e.printStackTrace();
            // Dev mode: vẫn cho dùng OTP in trong log
            return ResponseEntity.ok("Đã tạo OTP (DEV MODE), kiểm tra server log để lấy mã.");
        }
    }

    // Tạo + lưu + gửi OTP dùng chung
    private void generateAndSendOtp(String email, String subject, String messagePrefix) {
        String otp = generateOtpCode();

        // Lưu OTP với hạn 5 phút
        otpStore.put(email, new OtpEntry(otp, LocalDateTime.now().plusMinutes(5)));

        // Log để dev test
        System.out.println("OTP for " + email + " = " + otp);

        // Gửi email
        sendOtpEmail(email, subject, messagePrefix, otp);

    }

    // Chỉ sinh OTP 6 số
    private String generateOtpCode() {
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }

    // Gửi email OTP (có thể tái dùng cho nhiều loại OTP khác nhau)
    private void sendOtpEmail(String email, String subject, String messagePrefix, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(messagePrefix + otp + "\nHết hạn sau 5 phút.");
        message.setFrom("phamhaianhpc10@gmail.com"); // hoặc @Value từ cấu hình
        mailSender.send(message);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        OtpEntry entry = otpStore.get(request.getEmail());
        if (entry == null) return ResponseEntity.badRequest().body("Không có mã OTP hợp lệ");
        if (entry.expiry.isBefore(LocalDateTime.now())) {
            otpStore.remove(request.getEmail());
            return ResponseEntity.badRequest().body("Mã OTP đã hết hạn");
        }
        if (!entry.code.equals(request.getOtp())) {
            return ResponseEntity.badRequest().body("Mã OTP không chính xác");
        }

        UserAccount user = userAccountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userAccountRepository.save(user);

        otpStore.remove(request.getEmail());

        return ResponseEntity.ok("Đặt lại mật khẩu thành công");
    }

    // ================== HÀM DÙNG CHUNG ==================
    private ResponseEntity<?> registerUser(RegisterRequest request, UserType userType, String roleCode) {
        if (userAccountRepository.existsByEmail(request.getEmail()))
            return ResponseEntity.badRequest().body("Email đã tồn tại");
        if (userAccountRepository.existsByUsername(request.getUsername()))
            return ResponseEntity.badRequest().body("Username đã tồn tại");

        var role = roleRepository.findByCode(roleCode);
        if (role == null) return ResponseEntity.badRequest().body("Role " + roleCode + " chưa tồn tại");

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
                        .build()
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

    private record OtpEntry(String code, LocalDateTime expiry) {}
}
