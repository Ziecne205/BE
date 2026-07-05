package com.parking.common.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPasswordResetOtp(String toEmail, String otp, int ttlMinutes) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Ma OTP dat lai mat khau - Parking System");

            String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px;'>" +
                    "<h2>Dat lai mat khau</h2>" +
                    "<p>Ban da yeu cau dat lai mat khau tren he thong Parking System.</p>" +
                    "<p>Ma OTP cua ban la:</p>" +
                    "<p style='font-size:28px;font-weight:bold;letter-spacing:6px;" +
                    "background:#f1f3f6;padding:12px 20px;border-radius:8px;display:inline-block;'>" +
                    otp + "</p>" +
                    "<p>Ma nay se het han trong vong " + ttlMinutes + " phut va chi dung duoc mot lan.</p>" +
                    "<p>Neu ban khong yeu cau dat lai mat khau, vui long bo qua email nay.</p>" +
                    "</div>";

            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("Da gui email OTP dat lai mat khau thanh cong den: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Loi khi gui email OTP dat lai mat khau den {}: {}", toEmail, e.getMessage());
        }
    }
}
