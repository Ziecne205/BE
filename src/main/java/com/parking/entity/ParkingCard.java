package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ParkingCards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CardID")
    private Long cardId;

    @Column(name = "CardCode", nullable = false, unique = true)
    private String cardCode;

    @Column(name = "Status")
    private String status; // Active, Lost, InUse
}
