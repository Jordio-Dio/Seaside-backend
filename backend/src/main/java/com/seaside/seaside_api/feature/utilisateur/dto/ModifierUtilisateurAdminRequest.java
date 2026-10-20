package com.seaside.seaside_api.feature.utilisateur.dto;

import com.seaside.seaside_api.feature.utilisateur.model.RoleUsers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

// 
//    Modification complète par ADMIN
//    Permet de modifier nom + email + rôle + statut en une seule requête
//    Tous les champs sont optionnels — seuls les champs fournis sont modifiés
// 

@Data
public class ModifierUtilisateurAdminRequest {
    
    @Size(min = 3, max = 50, message = "Le nom doit faire entre 3 et 50 caractères")
    private String nomUtilisateur;// Optionnel

    @Email(message = "Format email invalide")
    private String email;

    
    private RoleUsers role;

    private Boolean estActif;
}
