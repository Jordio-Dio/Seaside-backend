package com.seaside.seaside_api.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Le token est obligatoire")
    private String token;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 12, message = "Le mot de passe doit faire au moins 12 caractères (NIST SP 800-63B)")
    private String nouveauMotDePasse;

    @NotBlank(message = "La confirmation est obligatoire")
    private String confirmation;
}