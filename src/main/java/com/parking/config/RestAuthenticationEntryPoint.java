package com.parking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Tra ve 401 (khong phai 403) khi request CHUA xac thuc — thieu token, token het han
 * hoac chu ky khong hop le. Mac dinh cua Spring Security 6 la Http403ForbiddenEntryPoint
 * (tra 403), khien FE khong phan biet duoc "phien het han" (can dang nhap lai) voi
 * "khong du quyen" (dung role nhung bi cam). Dat 401 o day de FE tu dong dieu huong ve
 * trang dang nhap; con 403 (AccessDeniedHandler mac dinh) van danh cho loi thieu quyen role.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> body = ApiResponse.fail(
                "Phien dang nhap da het han hoac khong hop le, vui long dang nhap lai",
                "SESSION_EXPIRED");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
