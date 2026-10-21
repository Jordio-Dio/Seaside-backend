package com.seaside.seaside_api.feature.organisateur.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** POST /organisateur/demande — message facultatif du demandeur. */
@Data
public class DemandeOrganisateurRequest {

    @Size(max = 2000, message = "Le message ne peut pas dépasser 2000 caractères")
    private String message;
}
