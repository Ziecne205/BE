package com.parking.modules.staff;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckOutResponse {
    private Long sessionId;
    private String licensePlateIn;
    private String licensePlateOut;
    private boolean plateMismatch;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private long parkedMinutes;
    private String vehicleTypeName;
    private BigDecimal amount;
    private BigDecimal lostTicketFee;
    private String paymentMethod;
    private String paymentStatus;
    private String exitGateName;
    private String slotFreed;
    private boolean cardReturned;
    private boolean isOverstay;

    /** true khi collectedAmount lech qua tolerance — checkout CHUA hoan tat, cho Manager duyet. */
    private boolean pendingApproval;
    /** ID cua CheckoutApprovalRequest da tao, chi co gia tri khi pendingApproval=true. */
    private Long approvalRequestId;
}
