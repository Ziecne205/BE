package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FeedbackID")
    private Long feedbackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SessionID", nullable = false)
    private ParkingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;

    @Column(name = "Rating", nullable = false)
    private Integer rating; // 1-5

    @Column(name = "Comment")
    private String comment;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;
}
