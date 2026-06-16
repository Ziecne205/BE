package com.parking.modules.manager;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FloorRequest {

    @NotBlank
    private String floorName;

    /** Co the null = tang phuc vu tat ca loai xe. */
    private Integer dedicatedVehicleTypeId;

    @NotNull
    private Integer totalCapacity;
}
