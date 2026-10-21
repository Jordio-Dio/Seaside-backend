package com.seaside.seaside_api.feature.stats.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dashboard global de la console admin (GET /admin/dashboard).
 * Chaque bloc alimente une carte du dashboard frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {

    // ─── Utilisateurs ─────────────────────────────────────
    private long totalUtilisateurs;
    private long utilisateursActifs;
    private long totalAdmins;
    private long totalOrganisateurs;
    private long totalClients;
    private long totalComptoires;

    // ─── Demandes organisateur (file d'attente de validation) ─
    private long demandesEnAttente;

    // ─── Événements ───────────────────────────────────────
    private long totalEvenements;
    private long evenementsActifs;

    // ─── Activité / revenus (toutes entrées confondues) ───
    private long totalPersonnes;
    private BigDecimal totalRevenus;

    // ─── Licences ─────────────────────────────────────────
    private long totalLicences;
    private long licencesActives;

    // ─── Parc ESP32 ───────────────────────────────────────
    private long totalMasters;
    private long mastersConnectes;
    private long totalSlaves;
}
