package com.seaside.seaside_api.feature.organisateur.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.seaside.seaside_api.feature.organisateur.model.StatutDemande;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Réponse unique pour « ma demande » et la liste admin. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeOrganisateurResponse {

    private UUID id;
    private UUID utilisateurId;
    private String nomUtilisateur;
    private String email;
    private StatutDemande statut;
    private String message;
    private String motif;
    private LocalDateTime dateDemande;
    private LocalDateTime dateTraitement;
    private UUID traiteParId;
}
