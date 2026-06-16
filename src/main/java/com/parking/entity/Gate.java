package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Gates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GateID")
    private Integer gateId;

    @Column(name = "GateName", nullable = false)
    private String gateName;

    @Column(name = "GateType", nullable = false)
    private String gateType; // Entry, Exit

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FloorID", nullable = false)
    private Floor floor;
}
