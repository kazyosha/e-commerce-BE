package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationCustomerService {

    private final UserAccountRepository userRepo;
    private final OtpService otpService;

    private UserAccount getCurrentUser(String principalName) {
        return userRepo.findByUsernameOrEmail(principalName, principalName)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /** Gửi OTP xác thực email cho user đang đăng nhập */
    public void sendEmailVerifyOtp(String principalName) {
        UserAccount user = getCurrentUser(principalName);

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email đã được xác thực");
        }

        otpService.sendVerifyOtp(user);
    }

    /** Xác thực OTP & set emailVerified = true */
    @Transactional
    public void verifyEmail(String principalName, String code) {
        UserAccount user = getCurrentUser(principalName);

        if (user.isEmailVerified()) {
            return; // đã verify rồi thì thôi
        }

        boolean ok = otpService.verifyOtp(user, code);
        if (!ok) {
            throw new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }

        user.setEmailVerified(true);
        userRepo.save(user);
    }
}
