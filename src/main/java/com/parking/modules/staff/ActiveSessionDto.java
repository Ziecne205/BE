package com.parking.modules.staff;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveSessionDto {
    private Long sessionId;
    private String licensePlateIn;
    private String vehicleTypeName;
    private LocalDateTime entryTime;
    private String entryGateName;
    private String status;
    private String suggestedSlotCode;
    private String actualSlotCode;
    private boolean hasReservation;
    private boolean hasCard;
    private long parkedMinutes;
    /** Phi tam tinh theo bang gia hien hanh cho thoi luong da do den hien tai (null neu chua co bang gia). */
    private java.math.BigDecimal estimatedFee;
    /** Tien coc da thanh toan (0 neu khong co dat cho hoac coc chua "Paid") — de FE tru vao khi
     * hoi thu tien mat, tranh bat khach tra du ca phan da coc. */
    private java.math.BigDecimal depositAlreadyPaid;
    /** So tien THUC con phai thu = estimatedFee - depositAlreadyPaid - da thanh toan online khac,
     * toi thieu 0. Day la con so nen hien thi/dien san cho Staff, KHONG phai estimatedFee. */
    private java.math.BigDecimal amountDue;
    /** Da qua expectedExitTime+30' ma van chua check-out (SessionExpiryScheduler.flagOverstaySessions) —
     * tach rieng de dashboard bao "N xe qua han" ma KHONG loai xe nay khoi suc chua/headroom
     * (xe van dang chiem cho vat ly). */
    private boolean isOverstayFlagged;
}
