package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.EmailOtp;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.EmailOtpPurpose;
import com.c05.kaz.ecommercebackend.repository.EmailOtpRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final JavaMailSender mailSender;
    private final EmailOtpRepository emailOtpRepository;

    @Value("${app.mail.from:}")       // From tuỳ biến của app (ưu tiên dùng cái này)
    private String appFrom;

    @Value("${spring.mail.username:}")// Fall back: username của SMTP (ví dụ Gmail)
    private String smtpUser;

    @Value("${app.mail.fromName:}")   // Tên hiển thị (tuỳ chọn)
    private String appFromName;

    private String randomCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    private String resolveFrom() {
        if (appFrom != null && !appFrom.isBlank()) return appFrom;
        if (smtpUser != null && !smtpUser.isBlank()) return smtpUser;
        throw new IllegalStateException("Thiếu cấu hình địa chỉ FROM cho email (app.mail.from hoặc spring.mail.username).");
    }

    @Transactional
    void saveAndSendHtml(
            UserAccount user,
            String code,
            EmailOtpPurpose purpose,
            String subject,
            String htmlBody
    ) {
        emailOtpRepository.invalidateAllUnusedByUserAndPurpose(user, purpose);

        EmailOtp otp = EmailOtp.builder()
                .user(user)
                .code(code)
                .purpose(purpose)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();
        emailOtpRepository.save(otp);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            // ✅ PHẢI có From
            String from = resolveFrom();
            if (appFromName != null && !appFromName.isBlank()) {
                helper.setFrom(from, appFromName);   // có tên hiển thị
            } else {
                helper.setFrom(from);                // không có tên hiển thị
            }
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Gửi email OTP thất bại", e);
        }
    }

    /* ================== SUPPLIER UPGRADE ================== */

    public void sendUpgradeOtp(UserAccount user) {
        String code = randomCode();
        String subject = "Mã OTP xác nhận đăng ký nhà cung cấp";
        String body = "<p>Xin chào " + user.getUsername() + ",</p>"
                + "<p>Mã OTP xác nhận nâng cấp nhà cung cấp của bạn là: <b>" + code + "</b></p>"
                + "<p>Mã có hiệu lực trong 5 phút.</p>";
        saveAndSendHtml(user, code, EmailOtpPurpose.SUPPLIER_UPGRADE, subject, body);
    }

    public boolean verifyUpgradeOtp(UserAccount user, String code) {
        return verify(user, code, EmailOtpPurpose.SUPPLIER_UPGRADE);
    }

    /* ================== EMAIL VERIFY (KHÁCH HÀNG) ================== */

    public void sendVerifyEmailOtp(UserAccount user) {
        String code = randomCode();
        String subject = "Xác thực email tài khoản Kaz E-Commerce";
        String body = "<p>Xin chào " + user.getUsername() + ",</p>"
                + "<p>Mã OTP xác thực email của bạn là: <b>" + code + "</b></p>"
                + "<p>Mã có hiệu lực trong 5 phút.</p>";
        saveAndSendHtml(user, code, EmailOtpPurpose.EMAIL_VERIFY, subject, body);
    }

    public boolean verifyEmailOtp(UserAccount user, String code) {
        return verify(user, code, EmailOtpPurpose.EMAIL_VERIFY);
    }

    /* ================== COMMON VERIFY ================== */

    private boolean verify(UserAccount user, String code, EmailOtpPurpose purpose) {
        EmailOtp otp = emailOtpRepository
                .findTopByUserAndCodeAndPurposeAndUsedFalseOrderByCreatedAtDesc(user, code, purpose)
                .orElse(null);

        if (otp == null) return false;
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) return false;

        otp.setUsed(true);
        emailOtpRepository.save(otp);
        return true;
    }

    @Transactional
    public void sendForgotPasswordOtp(UserAccount user) {
        String code = randomCode();
        saveAndSendHtml(
                user, code, EmailOtpPurpose.FORGOT_PASSWORD,
                "Mã OTP đặt lại mật khẩu",
                """
                <p>Xin chào %s,</p>
                <p>Mã OTP đặt lại mật khẩu của bạn là: <b>%s</b></p>
                <p>Mã có hiệu lực trong 5 phút.</p>
                """.formatted(user.getUsername(), code)
        );
    }

    @Transactional
    public boolean verifyForgotPasswordOtp(UserAccount user, String code) {
        return verify(user, code, EmailOtpPurpose.FORGOT_PASSWORD);
    }
}
