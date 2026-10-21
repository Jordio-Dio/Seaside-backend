package com.seaside.seaside_api.feature.organisateur.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.seaside.seaside_api.feature.organisateur.dto.DemandeOrganisateurResponse;
import com.seaside.seaside_api.feature.organisateur.dto.TraiterDemandeRequest;
import com.seaside.seaside_api.feature.organisateur.model.StatutDemande;
import com.seaside.seaside_api.feature.organisateur.service.DemandeOrganisateurService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Console d'approbation de l'admin (CEO).
 * Routes sous /admin/** → ROLE_ADMIN exigé (SecurityConfig).
 */
@RestController
@RequestMapping("/admin/demandes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminDemandeController {

    private final DemandeOrganisateurService demandeService;

    // GET /admin/demandes?statut=EN_ATTENTE — liste (toutes si pas de filtre)
    @GetMapping
    public ResponseEntity<List<DemandeOrganisateurResponse>> lister(
            @RequestParam(name = "statut", required = false) StatutDemande statut) {
        return ResponseEntity.ok(demandeService.lister(statut));
    }

    // POST /admin/demandes/{id}/approuver — → rôle ORGANISATEUR + email
    @PostMapping("/{id}/approuver")
    public ResponseEntity<DemandeOrganisateurResponse> approuver(@PathVariable UUID id) {
        return ResponseEntity.ok(demandeService.approuver(id));
    }

    // POST /admin/demandes/{id}/refuser — motif obligatoire + email motivé
    @PostMapping("/{id}/refuser")
    public ResponseEntity<DemandeOrganisateurResponse> refuser(
            @PathVariable UUID id,
            @Valid @RequestBody TraiterDemandeRequest req) {
        return ResponseEntity.ok(demandeService.refuser(id, req));
    }

    // POST /admin/demandes/{id}/ignorer — mise de côté (re-traitable ensuite)
    @PostMapping("/{id}/ignorer")
    public ResponseEntity<DemandeOrganisateurResponse> ignorer(@PathVariable UUID id) {
        return ResponseEntity.ok(demandeService.ignorer(id));
    }
}
