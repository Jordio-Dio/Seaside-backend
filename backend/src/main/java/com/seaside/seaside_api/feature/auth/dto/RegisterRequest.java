package com.seaside.seaside_api.feature.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

    // CORRECTION P0 : le champ `role` a été SUPPRIMÉ.
    // Il était accepté depuis le client → n'importe qui pouvait s'inscrire ADMIN.
    // Le serveur force systématiquement CLIENT (voir AuthService.inscrire).
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 12, message = "Le mot de passe doit faire au moins 12 caracteres (NIST SP 800-63B)")
    private String motDePasse;
}
