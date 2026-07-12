package com.parking.modules.staff;

import com.parking.entity.Gate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GateResponse {

    private Integer gateId;
    private String gateName;
    private String gateType;
    private Integer floorId;
    private String floorName;

    public static GateResponse from(Gate gate) {
        return GateResponse.builder()
                .gateId(gate.getGateId())
                .gateName(gate.getGateName())
                .gateType(gate.getGateType())
                .floorId(gate.getFloor() != null ? gate.getFloor().getFloorId() : null)
                .floorName(gate.getFloor() != null ? gate.getFloor().getFloorName() : null)
                .build();
    }
}
