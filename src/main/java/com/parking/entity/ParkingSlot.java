package com.parking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ParkingSlots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSlot {

    @Id
    private Long slotId;

    @DBRef
    private Floor floor;

    private String zone;

    @Indexed(unique = true)
    private String slotCode;

    @DBRef
    private VehicleType vehicleType;

    private String status; // Available, Occupied, Maintenance — chỉ camera CV hoặc Manager đổi
}
