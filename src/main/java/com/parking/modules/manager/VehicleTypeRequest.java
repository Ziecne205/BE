package com.parking.modules.manager;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleTypeRequest {

    @NotBlank(message = "Tên loại xe không được để trống")
    @Size(max = 50, message = "Tên loại xe tối đa 50 ký tự")
    private String typeName;

    @Size(max = 50, message = "Kích thước tối đa 50 ký tự")
    private String dimensions;
}
