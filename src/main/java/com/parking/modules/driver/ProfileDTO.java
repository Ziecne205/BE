package com.parking.modules.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ho so tai khoan tra ve cho app tai xe. Khong bao gom passwordHash.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String roleName;
    private String status;
}
