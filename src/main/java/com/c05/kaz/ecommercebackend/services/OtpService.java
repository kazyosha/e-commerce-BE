package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.EmailOtp;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
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

    /** Tạo & lưu OTP, trả về code */
    private String generateOtpCode(UserAccount user) {
        String code = String.format("%06d", new Random().nextInt(999999));

        EmailOtp otp = EmailOtp.builder()
                .user(user)
                .code(code)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();

        emailOtpRepository.save(otp);
        return code;
    }

    /** Gửi email HTML đơn giản */
    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Gửi email OTP thất bại", e);
        }
    }

    /** OTP dùng cho nâng cấp nhà cung cấp */
    public void sendUpgradeOtp(UserAccount user) {
        String code = generateOtpCode(user);

        String html = "<p>Xin chào <b>" + user.getUsername() + "</b>,</p>"
                + "<p>Mã OTP xác nhận nâng cấp tài khoản nhà cung cấp của bạn là: "
                + "<b style='font-size:18px;'>" + code + "</b></p>"
                + "<p>Mã có hiệu lực trong 5 phút.</p>";

        sendEmail(user.getEmail(), "Mã OTP xác nhận đăng ký nhà cung cấp", html);
    }

    /** OTP dùng cho xác thực email tài khoản khách hàng */
    public void sendVerifyOtp(UserAccount user) {
        String code = generateOtpCode(user);

        String html = "<p>Xin chào <b>" + user.getUsername() + "</b>,</p>"
                + "<p>Mã OTP xác thực email tài khoản của bạn là: "
                + "<b style='font-size:18px;'>" + code + "</b></p>"
                + "<p>Mã có hiệu lực trong 5 phút.</p>";

        sendEmail(user.getEmail(), "Xác thực email tài khoản Kaz E-Commerce", html);
    }

    /** Xác thực OTP (dùng chung cho cả 2 luồng) */
    public boolean verifyOtp(UserAccount user, String code) {
        EmailOtp otp = emailOtpRepository
                .findTopByUserAndCodeAndUsedFalseOrderByCreatedAtDesc(user, code)
                .orElse(null);

        if (otp == null) return false;
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) return false;

        otp.setUsed(true);
        emailOtpRepository.save(otp);
        return true;
    }
}
