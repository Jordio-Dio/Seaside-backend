package com.seaside.seaside_api.feature.organisateur.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.seaside.seaside_api.feature.utilisateur.model.Utilisateur;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Demande de passage CLIENT → ORGANISATEUR soumise par un utilisateur
 * et traitée par l'admin (console d'approbation du CEO).
 * Table créée automatiquement (ddl-auto=update).
 */
@Entity
@Table(name = "demandes_organisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeOrganisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    @JsonIgnore
    private Utilisateur utilisateur;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private StatutDemande statut = StatutDemande.EN_ATTENTE;

    // Message facultatif du demandeur (activité, type d'événements…)
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    // Motif de refus communiqué au demandeur (obligatoire si REFUSEE)
    @Column(name = "motif", columnDefinition = "TEXT")
    private String motif;

    @Column(name = "date_demande", nullable = false, updatable = false)
    private LocalDateTime dateDemande;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

    // Id de l'admin qui a trité la demande (journal de décision)
    @Column(name = "traite_par")
    private UUID traiteParId;

    @PrePersist
    public void prePersist() {
        if (dateDemande == null) {
            dateDemande = LocalDateTime.now();
        }
    }
}
