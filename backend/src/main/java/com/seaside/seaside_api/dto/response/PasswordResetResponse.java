package com.seaside.seaside_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ─── Réponse générique pour forgot-password et reset-password
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PasswordResetResponse {
    private String message;
}
