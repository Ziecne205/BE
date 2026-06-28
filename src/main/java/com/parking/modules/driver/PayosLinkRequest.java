package com.parking.modules.driver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayosLinkRequest {
    private String type;
    private Long id;
}
