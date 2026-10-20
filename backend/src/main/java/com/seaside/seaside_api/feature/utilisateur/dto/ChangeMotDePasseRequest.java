package com.seaside.seaside_api.feature.utilisateur.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeMotDePasseRequest {

    @NotBlank(message = "L'ancien mot de passe est obligatoire")
    private String ancienMotDePasse;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 6, message = "Le nouveau mot de passe doir faire au moin")
    private String nouveauMotDePasse;

    @NotBlank(message = "La confirmation est obligatoire")
    private String confirmation;
}