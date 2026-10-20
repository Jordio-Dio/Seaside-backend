package com.seaside.seaside_api.feature.esp32.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CategorieStatDTO {
    private UUID categorieId;
    private String nom;
    private String couleur;
    private BigDecimal prix;
    private Integer nombreEntrees;
    private BigDecimal revenu;
    private Integer capacite;
    private Boolean estComplete;
    private Integer slaveId; // slave associé à cette catégorie
}