package com.seaside.seaside_api.feature.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format email invalide")
    private String email;
}