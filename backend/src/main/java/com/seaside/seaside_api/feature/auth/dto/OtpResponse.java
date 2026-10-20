package com.seaside.seaside_api.feature.auth.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ─── Réponse après envoi OTP ────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OtpResponse {
    private String message;
    private String email;         // email auquel le code a été envoyé
}
