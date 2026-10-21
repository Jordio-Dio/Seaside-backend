package com.seaside.seaside_api.feature.organisateur.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seaside.seaside_api.feature.organisateur.dto.DemandeOrganisateurRequest;
import com.seaside.seaside_api.feature.organisateur.dto.DemandeOrganisateurResponse;
import com.seaside.seaside_api.feature.organisateur.service.DemandeOrganisateurService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Côté demandeur : dépôt et suivi de sa demande d'organisateur.
 * Accessible à tout utilisateur connecté (les verrous métier sont dans le service).
 */
@RestController
@RequestMapping("/organisateur")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class OrganisateurController {

    private final DemandeOrganisateurService demandeService;

    // POST /organisateur/demande — déposer sa demande
    // (body facultatif : { "message": "..." })
    @PostMapping("/demande")
    public ResponseEntity<DemandeOrganisateurResponse> deposer(
            @Valid @RequestBody(required = false) DemandeOrganisateurRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(demandeService.deposer(req));
    }

    // GET /organisateur/demande — suivre ma demande (statut, motif)
    @GetMapping("/demande")
    public ResponseEntity<DemandeOrganisateurResponse> maDemande() {
        return ResponseEntity.ok(demandeService.maDemande());
    }
}
