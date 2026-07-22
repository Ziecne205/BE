package com.parking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Document(collection = "Feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    private Long feedbackId;

    @DBRef
    @JsonIgnore
    private ParkingSession session;

    @DBRef
    @JsonIgnore
    private User user;

    private Integer rating; // 1-5

    private String comment;

    private LocalDateTime createdAt;
}
