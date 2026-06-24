package com.parking.modules.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDetailResponse {
    private Long sessionId;
    private String licensePlateIn;
    private String licensePlateOut;
    private String vehicleTypeName;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private String status;
    
    private String entryImageUrl;
    private String exitImageUrl;
    
    private String entryGateName;
    private String exitGateName;

    // Pricing info
    private BigDecimal totalFee;
    
    // Payment info
    private String paymentStatus;
    private String paymentMethod;
    private LocalDateTime paymentTime;
}
