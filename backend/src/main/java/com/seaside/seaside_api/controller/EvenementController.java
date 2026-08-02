package com.seaside.seaside_api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seaside.seaside_api.dto.request.CategorieRequest;
import com.seaside.seaside_api.dto.request.EntreeRequest;
import com.seaside.seaside_api.dto.request.EvenementRequest;
import com.seaside.seaside_api.entity.Categorie;
import com.seaside.seaside_api.entity.Entree;
import com.seaside.seaside_api.entity.Evenement;
import com.seaside.seaside_api.service.EvenementService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/evenements")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class EvenementController {

    private final EvenementService evenementService;

    // ─── EVENEMENTS ─────────────────────────────────────────

    // GET /evenements — liste mes événements
    @GetMapping
    public ResponseEntity<List<Evenement>> mesEvenements() {
        return ResponseEntity.ok(evenementService.mesEvenements());
    }

    // GET /evenements/{id}/dashboard — dashboard d'un événement
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<Evenement> dashboard(@PathVariable UUID id) {
        return ResponseEntity.ok(evenementService.getDashboard(id));
    }

    // POST /evenements — créer un événement
    @PostMapping
    public ResponseEntity<Evenement> creer(@Valid @RequestBody EvenementRequest req) {

        log.info("=== POST /evenements appelé ===");
        log.info("Nom: {}, Lieu: {}", req.getNom(), req.getLieu());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(evenementService.creerEvenement(req));
    }

    // PUT /evenements/{id} — modifier
    @PutMapping("/{id}")
    public ResponseEntity<Evenement> modifier(
            @PathVariable UUID id,
            @Valid @RequestBody EvenementRequest req) {
        return ResponseEntity.ok(evenementService.modifierEvenement(id, req));
    }

    // DELETE /evenements/{id} — supprimer
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        evenementService.supprimerEvenement(id);
        return ResponseEntity.noContent().build();
    }

    // PATCH /evenements/{id}/activer
    @PatchMapping("/{id}/activer")
    public ResponseEntity<Evenement> activer(@PathVariable UUID id) {
        return ResponseEntity.ok(evenementService.activerEvenement(id));
    }

    // PATCH /evenements/{id}/desactiver
    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<Evenement> desactiver(@PathVariable UUID id) {
        return ResponseEntity.ok(evenementService.desactiverEvenement(id));
    }

    // ─── CATEGORIES ─────────────────────────────────────────

    // POST /evenements/{id}/categories — ajouter une catégorie
    @PostMapping("/{id}/categories")
    public ResponseEntity<Categorie> ajouterCategorie(
            @PathVariable UUID id,
            @Valid @RequestBody CategorieRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(evenementService.ajouterCategorie(id, req));
    }

    // DELETE /evenements/{evenementId}/categories/{categorieId}
    @DeleteMapping("/{evenementId}/categories/{categorieId}")
    public ResponseEntity<Void> supprimerCategorie(
            @PathVariable UUID evenementId,
            @PathVariable UUID categorieId) {
        evenementService.supprimerCategorie(evenementId, categorieId);
        return ResponseEntity.noContent().build();
    }

    // ─── ENTREES MANUELLES ───────────────────────────────────

    // POST /evenements/{id}/entrees — insérer une entrée manuellement
    @PostMapping("/{id}/entrees")
    public ResponseEntity<Entree> ajouterEntree(
            @PathVariable UUID id,
            @Valid @RequestBody EntreeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(evenementService.ajouterEntreeManuelle(id, req));
    }
}