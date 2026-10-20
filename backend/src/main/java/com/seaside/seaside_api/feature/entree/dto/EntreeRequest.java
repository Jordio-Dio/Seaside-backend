package com.seaside.seaside_api.feature.entree.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EntreeRequest {
    
    @NotNull(message = "La categorie est obligatoire")
    private UUID categorieId;

    @Min(value = 1, message = "Le comptage doit etre au moins 1")
    private Integer comptage = 1;

    // Manuel pour saisie manuelle , "esp32" raha efa vita le module
    private String source = "manuel";
}
