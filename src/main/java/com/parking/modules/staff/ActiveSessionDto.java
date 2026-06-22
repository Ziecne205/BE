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
}
