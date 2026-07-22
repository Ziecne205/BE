package com.parking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Document(collection = "Reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    private UUID reservationId;

    @JsonIgnore
    @DBRef
    private User user;

    @JsonIgnore
    @DBRef
    private VehicleType vehicleType;

    private String licensePlate;

    private LocalDateTime expectedEntryTime;

    private LocalDateTime expectedExitTime;

    private BigDecimal depositAmount;

    private BigDecimal priceAtBookingTime;

    // Snapshot cua toan bo cac thanh phan gia/PricingPolicy/FeeConfig con lai tai thoi diem dat
    // cho (ngoai priceAtBookingTime da co san) — de checkout tinh phi luon dung gia da khoa, khong
    // bi anh huong boi Manager doi PricingPolicy/FeeConfig sau khi da dat cho. Xem Phase 2.
    private Integer baseHoursAtBooking;

    private BigDecimal extraHourPriceAtBooking;

    private BigDecimal nightSurchargeAtBooking;

    private BigDecimal lostTicketFeeAtBooking;

    private BigDecimal depositPercentAtBooking;

    private BigDecimal overstayRatePerHourAtBooking;

    private BigDecimal estimatedFeeAtBooking;

    // Han check-in va so phut gia han truoc khi bi coi la no-show (cong thuc theo Phase 3),
    // luu san de scheduler query truc tiep thay vi tinh lai moi lan chay.
    private LocalDateTime checkinDeadline;

    private Integer graceMinutes;

    // Gio ra du kien GOC tai thoi diem dat cho, khong bao gio bi extendReservation() sua doi —
    // de checkout van gioi han duoc base_fee theo gia khoa trong dung khung gio da dat ban dau,
    // ke ca sau khi expectedExitTime da bi day ra boi gia han (xem Phase 4/Phase 5).
    private LocalDateTime originalExpectedExitTime;

    private String depositStatus; // Pending, Paid, Forfeited, Refunded

    private String status; // Pending, Confirmed, CheckedIn, Fulfilled, Cancelled, Expired

    private LocalDateTime createdAt;

    // Phơi ID/tên phẳng ra JSON (user & vehicleType bị @JsonIgnore để khỏi lộ
    // passwordHash). FE đọc trực tiếp các field này.
    public Long getUserId() {
        return user != null ? user.getUserId() : null;
    }

    public Integer getVehicleTypeId() {
        return vehicleType != null ? vehicleType.getVehicleTypeId() : null;
    }

    public String getVehicleTypeName() {
        return vehicleType != null ? vehicleType.getTypeName() : null;
    }
}
