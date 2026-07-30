package com.parking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "Users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private Long userId;

    @Indexed(unique = true)
    private String username;

    @JsonIgnore
    private String passwordHash;

    private String fullName;

    @Indexed(unique = true, sparse = true)
    private String phoneNumber;

    @Indexed(unique = true, sparse = true)
    private String email;

    @DBRef
    private Role role;

    private String status; // Active, Inactive, Banned

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** So lan no-show lien tiep (chua den nhan xe du da dat/xac nhan). Reset ve 0 khi check-in
     * thanh cong; dat Blacklisted=true khi cham/vuot BLACKLIST_THRESHOLD (xem FeeConfig). */
    @Builder.Default
    private Integer consecutiveNoShows = 0;

    @Builder.Default
    private Boolean blacklisted = false;

    /**
     * Moc thu hoi phien: MOI JWT phat hanh TRUOC thoi diem nay deu bi tu choi
     * (xem {@link com.parking.config.JwtService#isTokenValid}). JWT la stateless nen khong the
     * "xoa" token da phat hanh — day moc nay len la cach duy nhat de dang xuat tuc thoi cac
     * thiet bi khac. Duoc day len khi: doi/reset mat khau (tu phuc vu hoac do Admin), va khi
     * tai khoan bi khoa/ban.
     */
    @JsonIgnore
    private LocalDateTime sessionsValidFrom;
}
