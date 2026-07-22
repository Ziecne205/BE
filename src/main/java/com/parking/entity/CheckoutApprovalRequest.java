package com.parking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Yeu cau Manager duyet khi so tien mat thu tai cong (collectedAmount) lech qua muc cho phep
 * (CASH_TOLERANCE_VND) so voi so phai thu tinh duoc (computedAmount) — Phase 6 item 2. Khong
 * dung @DBRef sang ParkingSession vi checkout that su chi duoc hoan tat (finalize) khi Manager
 * approve — luu lai toan bo ngu canh can thiet (session/gate/lostTicket/paymentMethod) de
 * SessionService co the hoan tat checkout dung nhu luc Staff yeu cau ban dau, khong tinh lai
 * phi theo thoi diem approve (co the la nhieu gio sau).
 */
@Document(collection = "CheckoutApprovalRequests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutApprovalRequest {

    @Id
    private Long approvalId;

    private Long sessionId;

    private String licensePlate;

    private Integer exitGateId;

    private String exitImageUrl;

    private boolean lostTicket;

    private String paymentMethod;

    /** So tien Staff thuc thu tai cong (khac voi computedAmount qua muc tolerance). */
    private BigDecimal requestedAmount;

    /** So tien he thong tinh la phai thu (final_payment sau khi tru coc/da thanh toan online). */
    private BigDecimal computedAmount;

    private String reason;

    private String requestedBy;

    private String status; // Open, Approved, Rejected

    private String decidedBy;

    private LocalDateTime decidedAt;

    private LocalDateTime createdAt;
}
