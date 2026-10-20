package com.seaside.seaside_api.feature.auth.dto;

import com.seaside.seaside_api.feature.utilisateur.model.RoleUsers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse { // les retours pour l'authentification

    private String token; // access token 60 minutes
    private String refreshToken; // refresh token - 7 jours
    private String type;       // Toujours bearer
    private String email;
    private String nomUtilisateur;
    private RoleUsers role;
}
