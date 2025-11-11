package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.EmailOtp;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.EmailOtpPurpose;
import com.c05.kaz.ecommercebackend.repository.EmailOtpRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final JavaMailSender mailSender;
    private final EmailOtpRepository emailOtpRepository;

    private String randomCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    private void saveAndSendHtml(
            UserAccount user,
            String code,
            EmailOtpPurpose purpose,
            String subject,
            String htmlBody
    ) {
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
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
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
}
