package com.seaside.seaside_api.feature.esp32.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SlaveStatusDTO {
    private Integer slaveId;
    private String nom;
    private Integer batterie;            // 0-100%
    private Boolean estConnecte;
    private String categorieAssociee;    // nom de la catégorie
    private UUID categorieId;
    private LocalDateTime derniereActivite;
    private String niveauBatterie;       // "OK", "FAIBLE", "CRITIQUE"
}