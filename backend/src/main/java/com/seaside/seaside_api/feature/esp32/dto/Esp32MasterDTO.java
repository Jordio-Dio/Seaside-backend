package com.seaside.seaside_api.feature.esp32.dto;

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

    private String adresseMac;

    private String nom;

    private Integer batteryLevel;

    private LocalDateTime lastSeen;

    private String licence;

    private String moduleId;

    private Integer seuil;

    private Boolean connected;

    private Integer nombreSlaves;

    private Integer nombreSlavesConnectes;

    private UUID categorieId;

    private String categorieNom;

    private List<Esp32SlaveDTO> slaves;
}