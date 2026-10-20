package com.seaside.seaside_api.feature.auth.dto;

import com.seaside.seaside_api.feature.utilisateur.model.RoleUsers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 50, message = "Le nom doit faire entre 3 et 50 caracteres")
    private String nomUtilisateur;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit faire au moins 6 caracteres")
    private String motDePasse;

    @NotNull(message = "Le role est obligatoire")
    private RoleUsers role;
}
