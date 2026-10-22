package com.seaside.seaside_api.feature.evenement.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seaside.seaside_api.feature.evenement.dto.AdminEvenementResponse;
import com.seaside.seaside_api.feature.evenement.model.Evenement;
import com.seaside.seaside_api.feature.evenement.repository.EvenementRepository;
import com.seaside.seaside_api.feature.utilisateur.model.Utilisateur;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

/**
 * Console admin — supervision de TOUS les événements de la plateforme
 * (contrairement à GET /evenements qui ne liste que les siens).
 * Routes sous /admin/** → ROLE_ADMIN exigé (SecurityConfig).
 */
@RestController
@RequestMapping("/admin/evenements")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminEvenementController {

    private final EvenementRepository evenementRepository;

    // GET /admin/evenements — tous les événements + propriétaire + totaux
    @GetMapping
    public ResponseEntity<List<AdminEvenementResponse>> tousLesEvenements() {
        return ResponseEntity.ok(
                evenementRepository.findAll().stream().map(this::toResponse).toList());
    }

    // GET /admin/evenements/{id} — détail d'un événement
    @GetMapping("/{id}")
    public ResponseEntity<AdminEvenementResponse> evenement(@PathVariable UUID id) {
        Evenement evenement = evenementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Événement introuvable"));
        return ResponseEntity.ok(toResponse(evenement));
    }

    // PATCH /admin/evenements/{id}/activer — l'admin peut agir sur
    // n'importe quel événement (l'endpoint /evenements/{id}/activer du
    // propriétaire reste réservé à ce dernier).
    @PatchMapping("/{id}/activer")
    public ResponseEntity<AdminEvenementResponse> activer(@PathVariable UUID id) {
        return ResponseEntity.ok(adminToggle(id, true));
    }

    // PATCH /admin/evenements/{id}/desactiver — suspension (fraude, litige…)
    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<AdminEvenementResponse> desactiver(@PathVariable UUID id) {
        return ResponseEntity.ok(adminToggle(id, false));
    }

    private AdminEvenementResponse adminToggle(UUID id, boolean actif) {
        Evenement evenement = evenementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Événement introuvable"));
        if (actif) {
            evenement.activer();
        } else {
            evenement.desactiver();
        }
        return toResponse(evenementRepository.save(evenement));
    }

    private AdminEvenementResponse toResponse(Evenement e) {
        Utilisateur proprietaire = e.getUtilisateur();
        return AdminEvenementResponse.builder()
                .id(e.getId())
                .nom(e.getNom())
                .dateEvenement(e.getDateEvenement())
                .lieu(e.getLieu())
                .capaciteMaximale(e.getCapaciteMaximale())
                .estActif(e.isEstActif())
                .dateCreation(e.getDateCreation())
                .totalPersonnes(e.getTotalPersonnes())
                .totalRevenus(e.getTotalRevenus())
                .nbCategories(e.getCategories() != null ? e.getCategories().size() : 0)
                .proprietaireId(proprietaire != null ? proprietaire.getId() : null)
                .proprietaireNomUtilisateur(proprietaire != null ? proprietaire.getNomUtilisateur() : null)
                .proprietaireEmail(proprietaire != null ? proprietaire.getEmail() : null)
                .build();
    }
}
