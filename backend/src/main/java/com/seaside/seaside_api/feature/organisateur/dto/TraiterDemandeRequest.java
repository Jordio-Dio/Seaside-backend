package com.seaside.seaside_api.feature.organisateur.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** POST /admin/demandes/{id}/refuser — motif obligatoire (communiqué au demandeur). */
@Data
public class TraiterDemandeRequest {

    @NotBlank(message = "Le motif de refus est obligatoire")
    @Size(max = 1000, message = "Le motif ne peut pas dépasser 1000 caractères")
    private String motif;
}
