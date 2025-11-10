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

    public void sendUpgradeOtp(UserAccount user) {
        String code = String.format("%06d", new Random().nextInt(999999));

        EmailOtp otp = EmailOtp.builder()
                .user(user)
                .code(code)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();
        emailOtpRepository.save(otp);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(user.getEmail());
            helper.setSubject("Mã OTP xác nhận đăng ký nhà cung cấp");
            helper.setText(
                    "<p>Xin chào " + user.getUsername() + ",</p>" +
                            "<p>Mã OTP xác nhận nâng cấp nhà cung cấp của bạn là: <b>" + code + "</b></p>" +
                            "<p>Mã có hiệu lực trong 5 phút.</p>",
                    true
            );
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Gửi email OTP thất bại", e);
        }
    }

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
