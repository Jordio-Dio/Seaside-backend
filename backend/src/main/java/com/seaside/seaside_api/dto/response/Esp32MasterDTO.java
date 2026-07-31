package com.seaside.seaside_api.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Esp32MasterDTO {

    private UUID id;

    private String macAddress;

    private Integer batteryLevel;

    private LocalDateTime lastSeen;

    private Integer nombreSlaves;

    private Integer nombreSlavesConnectes;

    private UUID categorieId;

    private String categorieNom;

    private List<Esp32SlaveDTO> slaves;
}