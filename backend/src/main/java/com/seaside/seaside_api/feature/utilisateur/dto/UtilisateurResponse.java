package com.seaside.seaside_api.feature.utilisateur.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.seaside.seaside_api.feature.utilisateur.model.RoleUsers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurResponse {
    
    private UUID id;
    private String nomUtilisateur;
    private String email;
    private RoleUsers role;
    private boolean estActif;
    private LocalDateTime dateCreation;
    private Integer nombreEvenments; // Info utile pour admin

    // pas de motDePasse  ici
}
