package com.seaside.seaside_api.feature.evenement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vue admin d'un événement (tous les événements de la plateforme,
 * avec propriétaire et totaux) — pour la console du CEO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminEvenementResponse {

    private UUID id;
    private String nom;
    private LocalDate dateEvenement;
    private String lieu;
    private Integer capaciteMaximale;
    private boolean estActif;
    private LocalDateTime dateCreation;

    // Totaux calculés (somme des catégories)
    private Integer totalPersonnes;
    private BigDecimal totalRevenus;
    private int nbCategories;

    // Propriétaire
    private UUID proprietaireId;
    private String proprietaireNomUtilisateur;
    private String proprietaireEmail;
}
