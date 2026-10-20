package com.seaside.seaside_api.feature.esp32.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SlaveConfigDTO {
    private Integer slaveId;       // numéro physique du slave
    private String categorieId;    // UUID → utilisé dans le topic MQTT
    private String nomCategorie;   // "VIP", "Simple"...
}