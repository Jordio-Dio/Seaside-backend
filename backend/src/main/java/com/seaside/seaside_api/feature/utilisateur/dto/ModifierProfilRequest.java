package com.seaside.seaside_api.feature.utilisateur.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

// modifier un profil (nom)
@Data
public class ModifierProfilRequest {
    @Size(min = 3, max = 50, message = "Le nom doit faire entre 2 et 50 caracteres")
    private String nomUtilisateur;

    @Email(message = "Format invalide")
    private String email;
}
