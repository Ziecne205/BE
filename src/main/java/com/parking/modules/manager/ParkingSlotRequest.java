package com.parking.modules.manager;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParkingSlotRequest {

    @NotNull(message = "Tầng không được để trống")
    private Integer floorId;

    @Size(max = 5, message = "Khu vực tối đa 5 ký tự (A, B, C...)")
    private String zone;

    @NotBlank(message = "Mã ô đỗ không được để trống")
    @Size(max = 10, message = "Mã ô tối đa 10 ký tự (VD: B1-A01)")
    private String slotCode;

    @NotNull(message = "Loại xe không được để trống")
    private Integer vehicleTypeId;

    @Pattern(regexp = "Available|Occupied|Maintenance",
             message = "Trạng thái phải là Available, Occupied hoặc Maintenance")
    private String status = "Available";
}
