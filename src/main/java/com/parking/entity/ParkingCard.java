package com.parking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ParkingCards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingCard {

    @Id
    private Long cardId;

    @Indexed(unique = true)
    private String cardCode;

    private String status; // Active, Lost, InUse
}
