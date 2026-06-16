package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SystemConfigs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig {

    @Id
    @Column(name = "ConfigKey")
    private String configKey;

    @Column(name = "ConfigValue", nullable = false)
    private String configValue;

    @Column(name = "Description")
    private String description;
}
