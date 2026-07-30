package com.parking.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.getSubject());
    }

    /**
     * Token con dung KHI VA CHI KHI: dung chu so huu, chua het han, VA duoc phat hanh SAU moc thu
     * hoi phien cua tai khoan ({@link com.parking.entity.User#getSessionsValidFrom()}). Ve dieu kien
     * cuoi: JWT stateless nen doi mat khau / khoa tai khoan truoc day KHONG lam token cu chet —
     * token cu van dung duoc tren thiet bi khac cho toi khi het han. So sanh iat voi moc thu hoi
     * chinh la co che thu hoi do.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        if (username == null || !username.equals(userDetails.getUsername()) || isExpired(token)) {
            return false;
        }
        if (userDetails instanceof AppUserPrincipal principal && principal.getSessionsValidFrom() != null) {
            Date issuedAt = extractClaim(token, Claims::getIssuedAt);
            if (issuedAt == null) {
                return false; // token cu khong co 'iat' -> khong chung minh duoc la phat hanh sau moc
            }
            // 'iat' trong JWT chi co do phan giai GIAY, nen cat moc thu hoi ve giay truoc khi so
            // sanh; token phat hanh dung giay thu hoi duoc coi la hop le (tranh 401 oan cho phien
            // vua dang nhap lai ngay sau khi doi mat khau).
            Instant cutoff = principal.getSessionsValidFrom()
                    .atZone(ZoneId.systemDefault()).toInstant().truncatedTo(ChronoUnit.SECONDS);
            return !issuedAt.toInstant().isBefore(cutoff);
        }
        return true;
    }

    private boolean isExpired(String token) {
        return extractClaim(token, claims -> claims.getExpiration()).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
