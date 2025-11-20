package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.user.UserReportDTO;
import com.c05.kaz.ecommercebackend.dto.user.UserReportRequest;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.entity.UserReport;
import com.c05.kaz.ecommercebackend.enums.AccountStatus;
import com.c05.kaz.ecommercebackend.enums.Severity;
import com.c05.kaz.ecommercebackend.exception.NotFoundException;
import com.c05.kaz.ecommercebackend.repository.UserAccountRepository;
import com.c05.kaz.ecommercebackend.repository.UserReportRepository;
import com.c05.kaz.ecommercebackend.security.SecurityUtils;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import com.c05.kaz.ecommercebackend.exception.BadRequestException;

import org.springframework.mail.javamail.JavaMailSender;

import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserReportService {

    private final UserReportRepository reportRepository;
    private final UserAccountRepository userAccountRepository;
    private final SecurityUtils securityUtils;

    // 💥 Inject trực tiếp mail sender (KHÔNG cần EmailService)
    private final JavaMailSender mailSender;

    public void createReport(Long reportedUserId, UserReportRequest request) {

        // Validate severity
        if (request.getSeverity() == null || request.getSeverity().isBlank()) {
            throw new BadRequestException("Severity không được để trống");
        }

        Severity severity;
        try {
            severity = Severity.valueOf(request.getSeverity().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Severity không hợp lệ: chỉ nhận HIGH, MEDIUM, LOW");
        }

        // user bị báo cáo
        UserAccount reported = userAccountRepository.findById(reportedUserId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        // user đang đăng nhập
        Long reporterId = securityUtils.getCurrentUserId();
        UserAccount reporter = userAccountRepository.findById(reporterId)
                .orElseThrow(() -> new NotFoundException("Người gửi không tồn tại"));

        UserReport report = UserReport.builder()
                .reportedUser(reported)
                .reporter(reporter)
                .reason(request.getReason())
                .detail(request.getDetail())
                .severity(severity)
                .build();

        reportRepository.save(report);
    }

    public List<UserReportDTO> getAllReports() {
        return reportRepository.findAll().stream()
                .map(r -> new UserReportDTO(
                        r.getId(),
                        r.getReportedUser().getUsername(),
                        r.getReporter().getUsername(),
                        r.getReason(),
                        r.getDetail(),
                        r.getSeverity().name(),
                        r.getCreatedAt(),
                        r.getEmailSent()
                ))
                .toList();
    }

    public void approveReport(Long reportId) {
        UserReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report không tồn tại"));

        UserAccount user = report.getReportedUser();
        user.setStatus(AccountStatus.BLOCKED);

        userAccountRepository.save(user);
    }

    // 💌 Gửi email cảnh cáo
    public void sendWarningEmail(Long reportId) {
        UserReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report không tồn tại"));

        UserAccount violatedUser = report.getReportedUser();

        String toEmail = violatedUser.getEmail();
        String subject = "Cảnh báo vi phạm tài khoản";

        String content = String.format("""
                Xin chào %s,

                Hệ thống ghi nhận tài khoản của bạn đã vi phạm quy định.

                🔥 Mức độ vi phạm: %s
                📌 Lý do: %s
                📝 Chi tiết: %s
                🕒 Thời gian: %s

                Vui lòng kiểm tra và tránh lặp lại để không bị khóa tài khoản.

                Trân trọng,
                Hệ thống hỗ trợ khách hàng
                """,
                violatedUser.getUsername(),
                report.getSeverity().name(),
                report.getReason(),
                report.getDetail(),
                report.getCreatedAt()
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            //  TÊN NGƯỜI GỬI
            helper.setFrom("phamhaianhpc10@gmail.com", "E-Commerce Team");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, false); // false = text, true = HTML

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi gửi email: " + e.getMessage());
        }
        report.setEmailSent(true);
        reportRepository.save(report);
    }
}
