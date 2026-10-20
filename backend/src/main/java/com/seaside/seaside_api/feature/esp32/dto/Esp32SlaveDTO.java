package com.seaside.seaside_api.feature.esp32.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Esp32SlaveDTO {

    private UUID id;

    private Integer slaveNumber;

    private String slaveId;

    private String nom;

    private String macAddress;

    private Integer batteryLevel;

    private Boolean connecte;

    private Boolean active;

    private Integer seuil;

    private LocalDateTime lastSeen;

    private UUID masterId;
}