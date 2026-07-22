package com.parking.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Gui email OTP qua SendGrid Mail Send API (HTTPS) thay vi SMTP.
 * Ly do: Render chan ket noi SMTP ra ngoai (465/587) -> SMTP luon timeout.
 * SendGrid goi qua HTTPS (443) nen khong bi chan.
 *
 * Cau hinh:
 *  - sendgrid.api-key   (env SENDGRID_API_KEY) — API key dang SG....
 *  - mail.sender-email  (env MAIL_SENDER)      — email nguoi gui DA XAC MINH (Single Sender) tren SendGrid
 *  - mail.sender-name   (env MAIL_SENDER_NAME) — ten hien thi nguoi gui
 *
 * Nuot loi (chi log) de luong dang ky/quen mat khau khong bi vo (tra 500) khi mail tam loi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final String SENDGRID_URL = "https://api.sendgrid.com/v3/mail/send";

    @Value("${sendgrid.api-key:}")
    private String sendgridApiKey;

    @Value("${mail.sender-email:}")
    private String senderEmail;

    @Value("${mail.sender-name:Parking System}")
    private String senderName;

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout((int) Duration.ofSeconds(8).toMillis());
        f.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        return new RestTemplate(f);
    }

    public void sendPasswordResetOtp(String toEmail, String otp, int ttlMinutes) {
        sendOtpEmail(toEmail,
                "Ma OTP dat lai mat khau - Parking System",
                "Dat lai mat khau",
                "Ban da yeu cau dat lai mat khau tren he thong Parking System.",
                otp, ttlMinutes);
    }

    public void sendRegistrationOtp(String toEmail, String otp, int ttlMinutes) {
        sendOtpEmail(toEmail,
                "Ma OTP xac nhan dang ky - Parking System",
                "Xac nhan dang ky tai khoan",
                "Ban dang dang ky tai khoan tren he thong Parking System. Hay nhap ma OTP de hoan tat.",
                otp, ttlMinutes);
    }

    private void sendOtpEmail(String toEmail, String subject, String heading, String intro,
                              String otp, int ttlMinutes) {
        if (sendgridApiKey == null || sendgridApiKey.isBlank()) {
            log.warn("SENDGRID_API_KEY chua cau hinh -> BO QUA gui email OTP toi {}. (Dat env SENDGRID_API_KEY de bat)", toEmail);
            return;
        }

        String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px;'>" +
                "<h2>" + heading + "</h2>" +
                "<p>" + intro + "</p>" +
                "<p>Ma OTP cua ban la:</p>" +
                "<p style='font-size:28px;font-weight:bold;letter-spacing:6px;" +
                "background:#f1f3f6;padding:12px 20px;border-radius:8px;display:inline-block;'>" +
                otp + "</p>" +
                "<p>Ma nay se het han trong vong " + ttlMinutes + " phut va chi dung duoc mot lan.</p>" +
                "<p>Neu ban khong yeu cau thao tac nay, vui long bo qua email nay.</p>" +
                "</div>";

        Map<String, Object> body = Map.of(
                "personalizations", List.of(Map.of("to", List.of(Map.of("email", toEmail)))),
                "from", Map.of("email", senderEmail, "name", senderName),
                "subject", subject,
                "content", List.of(Map.of("type", "text/html", "value", htmlContent)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(sendgridApiKey.trim());

        try {
            restTemplate.postForEntity(SENDGRID_URL, new HttpEntity<>(body, headers), String.class);
            log.info("Da gui email OTP (SendGrid) thanh cong den: {} ({})", toEmail, subject);
        } catch (Exception e) {
            // Nuot loi de khong tra 500 cho FE; log chi tiet de chan doan (vd sender chua xac minh, key sai).
            log.error("Loi khi gui email OTP (SendGrid) den {}: {}", toEmail, e.getMessage());
        }
    }
}
