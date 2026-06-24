package com.parking.modules.admin;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class RolePermissionRequest {

    @NotEmpty(message = "Danh sach permission khong duoc trong")
    private Set<Integer> permissionIds;
}
