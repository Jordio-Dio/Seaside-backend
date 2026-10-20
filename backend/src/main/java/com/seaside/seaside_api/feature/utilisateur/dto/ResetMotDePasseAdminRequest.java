package com.seaside.seaside_api.feature.utilisateur.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// ═══════════════════════════════════════════════════════════
// 3. Reset mot de passe — ADMIN seulement
//    L'admin n'a pas besoin de connaître l'ancien mot de passe
//    Il définit directement le nouveau
// ═══════════════════════════════════════════════════════════
@Data
public class ResetMotDePasseAdminRequest {
    
    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit faire au moins 6 caracteres")
    private String nouveauMotDePasse;

    @NotBlank(message = "La confirmation est obligatoire")
    private String confirmation;
}
