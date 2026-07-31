package com.parking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "ManualRefundRequests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualRefundRequest {

    @Id
    private UUID id;

    @DBRef
    private Reservation reservation;

    @DBRef
    private User user;

    private String reason;

    private String bankInfo;

    private String status; // Pending, Processed

    private LocalDateTime requestedAt;

    private LocalDateTime processedAt;
}
