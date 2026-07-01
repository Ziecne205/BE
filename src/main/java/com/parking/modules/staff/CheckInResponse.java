package com.parking.modules.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInResponse {
    private Long sessionId;
    private String licensePlateIn;
    private LocalDateTime entryTime;
    private String suggestedSlotCode;
    private boolean isReserved;
    private boolean isForceCheckIn;
}
