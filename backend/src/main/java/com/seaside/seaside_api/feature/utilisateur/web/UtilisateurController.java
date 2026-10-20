package com.seaside.seaside_api.feature.utilisateur.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.seaside.seaside_api.feature.utilisateur.dto.ChangeMotDePasseRequest;
import com.seaside.seaside_api.feature.utilisateur.dto.ChangeRoleRequest;
import com.seaside.seaside_api.feature.utilisateur.dto.ModifierProfilRequest;
import com.seaside.seaside_api.feature.utilisateur.dto.ModifierUtilisateurAdminRequest;
import com.seaside.seaside_api.feature.utilisateur.dto.ResetMotDePasseAdminRequest;
import com.seaside.seaside_api.feature.utilisateur.dto.UtilisateurResponse;
import com.seaside.seaside_api.feature.utilisateur.model.Utilisateur;
import com.seaside.seaside_api.feature.utilisateur.service.UtilisateurService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    // =====================================================
    // ROUTES CLIENT - /Utilisateurs/moi
    // =====================================================

    @GetMapping("/utilisateurs/moi")
    public ResponseEntity<UtilisateurResponse> monProfil() {
        return ResponseEntity.ok(utilisateurService.monProfil());
    }

    @PutMapping("/utilisateurs/moi")
    public ResponseEntity<UtilisateurResponse> modifierProfil(@Valid @RequestBody ModifierProfilRequest req) {
        return ResponseEntity.ok(utilisateurService.modifierMonProfil(req));
    }

    @PatchMapping("/utilisateurs/moi/mot-de-passe")
    public ResponseEntity<Void> changerMotDePasse(
            @Valid @RequestBody ChangeMotDePasseRequest req) {
        utilisateurService.changerMotDePasse(req);
        return ResponseEntity.noContent().build();
    }

    //DELETE /utilisateurs/moi
    @DeleteMapping("/utilisateurs/moi")
    public ResponseEntity<Void> supprimerMonCompte() {
        utilisateurService.supprimerMonCompte();
        return ResponseEntity.noContent().build();
    }


    // ROUTES ADMIN - /admin/utilisateurs
    // =====================================================

    @GetMapping("/admin/utilisateurs")
    public ResponseEntity<List<UtilisateurResponse>> tousLesUtilisateurs() {
        return ResponseEntity.ok(utilisateurService.tousLesUtilisateurs());
    }

    @GetMapping("/admin/utilisateurs/{id}")
    public ResponseEntity<UtilisateurResponse> trouverParId(@PathVariable UUID id) {
        return ResponseEntity.ok(utilisateurService.trouverParId(id));
    }

    @PatchMapping("/admin/utilisateurs/{id}/activer")
    public ResponseEntity<UtilisateurResponse> activer(@PathVariable UUID id) {
        return ResponseEntity.ok(utilisateurService.activer(id));
    }

    @PatchMapping("/admin/utilisateurs/{id}/desactiver")
    public ResponseEntity<UtilisateurResponse> desactiver(@PathVariable UUID id) {
        return ResponseEntity.ok(utilisateurService.desactiver(id));
    }

    @PatchMapping("/admin/utilisateurs/{id}/role")
    public ResponseEntity<UtilisateurResponse> changerRole(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest req) {
        return ResponseEntity.ok(utilisateurService.changerRole(id, req));
    }


    @PutMapping("/admin/utilisateurs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UtilisateurResponse> modifierUtilisateur(
            @PathVariable UUID id,
            @Valid @RequestBody ModifierUtilisateurAdminRequest req) {
        return ResponseEntity.ok(utilisateurService.modifierUtilisateurAdmin(id, req));
    }

    @DeleteMapping("/admin/utilisateurs/{id}")
    public ResponseEntity<Void> supprimerUtilisateur(@PathVariable UUID id) {
        utilisateurService.supprimerUtilisateur(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/utilisateurs/{id}/mot-de-passe")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetMotDePasse(
            @PathVariable UUID id,
            @Valid @RequestBody ResetMotDePasseAdminRequest req) {
        utilisateurService.resetMotDePasseAdmin(id, req);
        return ResponseEntity.noContent().build();
    }
}
