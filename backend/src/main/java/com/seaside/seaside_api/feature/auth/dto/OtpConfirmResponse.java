package com.seaside.seaside_api.feature.auth.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ─── Réponse après confirmation OTP ─────────────────────
// emailVerifie est renvoyé pour que le frontend pré-remplisse
// le champ email du formulaire d'inscription sans que l'utilisateur
// puisse le modifier
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OtpConfirmResponse {
    private String message;
    private String emailVerifie;  // à pré-remplir dans le formulaire d'inscription
    private Boolean verified;     // toujours true si on arrive ici
}