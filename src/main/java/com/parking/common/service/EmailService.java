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

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Yeu cau khoi phuc mat khau - Parking System");

            String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px;'>" +
                    "<h2>Khoi phuc mat khau</h2>" +
                    "<p>Ban da yeu cau khoi phuc mat khau cho tai khoan tren he thong Parking System.</p>" +
                    "<p>Nhan vao nut ben duoi de dat lai mat khau:</p>" +
                    "<p><a href='" + resetLink + "' " +
                    "style='display:inline-block;padding:10px 18px;background:#007bff;color:#fff;" +
                    "text-decoration:none;border-radius:4px;'>Dat lai mat khau</a></p>" +
                    "<p>Hoac sao chep lien ket sau vao trinh duyet:</p>" +
                    "<p style='word-break:break-all;'>" + resetLink + "</p>" +
                    "<p>Lien ket nay se het han trong vong 15 phut va chi dung duoc mot lan.</p>" +
                    "<p>Neu ban khong yeu cau khoi phuc mat khau, vui long bo qua email nay.</p>" +
                    "</div>";

            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("Da gui email khoi phuc mat khau thanh cong den: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Loi khi gui email khoi phuc mat khau den {}: {}", toEmail, e.getMessage());
        }
    }
}
