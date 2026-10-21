package com.seaside.seaside_api.feature.stats.service;

import org.springframework.stereotype.Service;

import com.seaside.seaside_api.feature.entree.repository.EntreeRepository;
import com.seaside.seaside_api.feature.evenement.repository.EvenementRepository;
import com.seaside.seaside_api.feature.esp32.repository.Esp32MasterRepository;
import com.seaside.seaside_api.feature.esp32.repository.Esp32SlaveRepository;
import com.seaside.seaside_api.feature.licence.model.LicenseStatus;
import com.seaside.seaside_api.feature.licence.repository.LicenseRepository;
import com.seaside.seaside_api.feature.organisateur.model.StatutDemande;
import com.seaside.seaside_api.feature.organisateur.repository.DemandeOrganisateurRepository;
import com.seaside.seaside_api.feature.stats.dto.StatsResponse;
import com.seaside.seaside_api.feature.utilisateur.model.RoleUsers;
import com.seaside.seaside_api.feature.utilisateur.repository.UtilisateurRepository;

import lombok.RequiredArgsConstructor;

/**
 * Dashboard global de la console admin (CEO).
 * Agrège les compteurs de chaque domaine — lecture seule, aucun effet de bord.
 * Les routes /admin/** exigent ROLE_ADMIN (SecurityConfig).
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final UtilisateurRepository utilisateurRepository;
    private final EvenementRepository evenementRepository;
    private final EntreeRepository entreeRepository;
    private final LicenseRepository licenseRepository;
    private final Esp32MasterRepository esp32MasterRepository;
    private final Esp32SlaveRepository esp32SlaveRepository;
    private final DemandeOrganisateurRepository demandeRepository;

    public StatsResponse dashboard() {
        Long personnes = entreeRepository.totalPersonnesToutes();

        return StatsResponse.builder()
                // Utilisateurs
                .totalUtilisateurs(utilisateurRepository.count())
                .utilisateursActifs(utilisateurRepository.countByEstActif(true))
                .totalAdmins(utilisateurRepository.countByRole(RoleUsers.ADMIN))
                .totalOrganisateurs(utilisateurRepository.countByRole(RoleUsers.ORGANISATEUR))
                .totalClients(utilisateurRepository.countByRole(RoleUsers.CLIENT))
                .totalComptoires(utilisateurRepository.countByRole(RoleUsers.COMPTOIRE))
                // Demandes en file d'attente
                .demandesEnAttente(demandeRepository.countByStatut(StatutDemande.EN_ATTENTE))
                // Événements
                .totalEvenements(evenementRepository.count())
                .evenementsActifs(evenementRepository.countByEstActif(true))
                // Activité
                .totalPersonnes(personnes != null ? personnes : 0L)
                .totalRevenus(entreeRepository.totalRevenusToutes())
                // Licences
                .totalLicences(licenseRepository.count())
                .licencesActives(licenseRepository.countByStatus(LicenseStatus.ACTIVE))
                // Parc ESP32
                .totalMasters(esp32MasterRepository.count())
                .mastersConnectes(esp32MasterRepository.countByConnected(true))
                .totalSlaves(esp32SlaveRepository.count())
                .build();
    }
}
