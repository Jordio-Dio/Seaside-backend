package com.seaside.seaside_api.feature.esp32.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AlerteDTO {
    private String code;        // "BATTERIE_FAIBLE", "SLAVE_DECONNECTE", "CAPACITE_MAX"
    private String message;
    private String niveau;      // "INFO", "WARN", "CRITIQUE"
    private Integer slaveId;    // null si alerte globale master
    private LocalDateTime declencheLe;
}