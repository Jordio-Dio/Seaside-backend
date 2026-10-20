package com.seaside.seaside_api.feature.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfirmOtpRequest {
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format email invalide")
    private String email;

    @NotBlank(message = "Le code est obligatoire")
    @Size(min = 6, max = 6, message = "Le code doit faire exactement 6 chiffres")
    private String code;
}