package com.seaside.seaside_api.feature.utilisateur.service;
import com.seaside.seaside_api.feature.auth.service.RefreshTokenService;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seaside.seaside_api.feature.utilisateur.dto.ChangeMotDePasseRequest;
import com.seaside.seaside_api.feature.utilisateur.dto.ChangeRoleRequest;
import com.seaside.seaside_api.feature.utilisateur.dto.ModifierProfilRequest;
import com.seaside.seaside_api.feature.utilisateur.dto.ModifierUtilisateurAdminRequest;
import com.seaside.seaside_api.feature.utilisateur.dto.ResetMotDePasseAdminRequest;
import com.seaside.seaside_api.feature.utilisateur.dto.UtilisateurResponse;
import com.seaside.seaside_api.feature.utilisateur.model.Utilisateur;
import com.seaside.seaside_api.feature.utilisateur.repository.UtilisateurRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    // Utilitaire : recuperer l'utilisateur connecte
    private Utilisateur moi() {
        Utilisateur user = (Utilisateur) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();

        // Recharge depuis la bdd pour eviter lazyloadingexception
        return utilisateurRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));
    }

    // Convertir entite en -> DTO (sans mot de passe)
    private UtilisateurResponse toResponse(Utilisateur u) {
        return UtilisateurResponse.builder()
                .id(u.getId())
                .nomUtilisateur(u.getNomUtilisateur())
                .email(u.getEmail())
                .role(u.getRole())
                .estActif(u.isEstActif())
                .dateCreation(u.getDateCreation())
                .nombreEvenments(u.getEvenements().size())
                .build();
    }

    // ==========================================
    // PARTIE CLIENT - Operations sur son propre compte
    // ==========================================
    // voir son profil
    @Transactional(readOnly = true)
    public UtilisateurResponse monProfil() {
        Utilisateur u = moi();

        // on recharge l'utilisateur depuis la bdd de session active
        u = utilisateurRepository.findById(u.getId()).orElseThrow();

        return toResponse(u);
    }

    // Modifier son nom et / ou email
    public UtilisateurResponse modifierMonProfil(ModifierProfilRequest req) {
        Utilisateur utilisateur = moi();

        // Verifier si le vouvel email n'est pas deja pris par quelqu'un d'autre
        if (req.getEmail() != null
                && !req.getEmail().equals(utilisateur.getEmail())
                && utilisateurRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Cet email est deja utilise par un autre compte");
        }

        // Verifier si le nouveau nom n'est pas deja pris
        if (req.getNomUtilisateur() != null
                && !req.getNomUtilisateur().equals(utilisateur.getNomUtilisateur())
                && utilisateurRepository.existsByNomUtilisateur(req.getNomUtilisateur())) {
            throw new RuntimeException("Ce nom d'utilisateur est deja pris");
        }

        if (req.getNomUtilisateur() != null)
            utilisateur.setNomUtilisateur(req.getNomUtilisateur());
        if (req.getEmail() != null)
            utilisateur.setEmail(req.getEmail());

        return toResponse(utilisateurRepository.save(utilisateur));
    }

    // Changer son mot de passe
    public void changerMotDePasse(ChangeMotDePasseRequest req) {
        Utilisateur utilisateur = moi();

        // 1 Verifier que l'ancien mot de passe est correct
        if (!passwordEncoder.matches(req.getAncienMotDePasse(), utilisateur.getMotsDePasseHash())) {
            throw new RuntimeException("Ancien mot de passe incorrect");
        }

        // 2 Verifier que la confirmation correspond
        if (!req.getNouveauMotDePasse().equals(req.getConfirmation())) {
            throw new RuntimeException("La confirmation ne correspond pas au nouveau mot de passe");
        }

        // CORRECTION P0 : politique NIST (12+ caractères, dictionnaire)
        com.seaside.seaside_api.feature.auth.service.PasswordPolicy
                .valider(req.getNouveauMotDePasse(), utilisateur.getEmail());

        // 3. Vérifier que le nouveau est différent de l'ancien
        if (passwordEncoder.matches(req.getNouveauMotDePasse(), utilisateur.getMotsDePasseHash())) {
            throw new RuntimeException("Le noueveau mots de passe doit different que  l'ancien");
        }

        // 4. Hasher et garder le nouveau mots de passe
        utilisateur.setMotsDePasseHash(passwordEncoder.encode(req.getNouveauMotDePasse()));
        utilisateurRepository.save(utilisateur);

        // 5. Invalider tous les refresh tokens (forcer reconnexions sur autres
        // appareils)
        refreshTokenService.supprimerParUtilisateur(utilisateur);
    }

    // Supprimer son propre compte
    public void supprimerMonCompte() {
        Utilisateur utilisateur = moi();
        refreshTokenService.supprimerParUtilisateur(utilisateur);
        utilisateurRepository.delete(utilisateur);
    }

    /// ========================================
    /// PARTIE ADMIN - gestion des utilisateurs
    /// =======================================

    // Liste tous les utilisateurs
    @Transactional(readOnly = true)
    public List<UtilisateurResponse> tousLesUtilisateurs() {
        return utilisateurRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Voir un utilisateur par id
    @Transactional(readOnly = true)
    public UtilisateurResponse trouverParId(UUID id) {
        return toResponse(trouverUtilisateur(id));
    }

    // Activer un compte
    public UtilisateurResponse activer(UUID id) {
        Utilisateur u = trouverUtilisateur(id);
        u.setEstActif(true);
        return toResponse(utilisateurRepository.save(null));
    }

    // Desactiver un compte
    public UtilisateurResponse desactiver(UUID id) {
        Utilisateur u = trouverUtilisateur(id);

        // Un admin ne peut pas se desactiver lui-meme
        if (u.getId().equals(moi().getId())) {
            throw new RuntimeException("Vous ne pouvez pas desactiver votre propre compte admin");
        }

        u.setEstActif(false);
        // Invalider ses refresh tokens -> forcer connexions
        refreshTokenService.supprimerParUtilisateur(u);
        return toResponse(utilisateurRepository.save(u));
    }

    // Changer le role d'un utilisateur
    public UtilisateurResponse changerRole(UUID id, ChangeRoleRequest req) {
        Utilisateur u = trouverUtilisateur(id);

        // Un admin ne peut pas se retirer son role admin
        if (u.getId().equals(moi().getId())) {
            throw new RuntimeException("Vous ne pouvez pas modifier votre propre role");
        }

        if (u.getId().equals(moi().getId())) {
            throw new RuntimeException("Vous ne pouvez pas modifier votre propre role");
        }

        u.setRole(req.getRole());
        return toResponse(utilisateurRepository.save(u));
    }

    // Supprimer un utilisateur (admin)
    public void supprimerUtilisateur(UUID id) {
        Utilisateur u = trouverUtilisateur(id);

        if (u.getId().equals(moi().getId())) {
            throw new RuntimeException("Utilisez /utilisateur/moi pour supprimer votre compte");
        }

        refreshTokenService.supprimerParUtilisateur(u);
        utilisateurRepository.delete(u);
    }

    // Utilitaire prive je ne mais pas public cette methode
    private Utilisateur trouverUtilisateur(UUID id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }


    // ─── Reset mot de passe par l'admin ─────────────────────────
    // L'admin n'a pas besoin de connaître l'ancien mot de passe
    public void resetMotDePasseAdmin(UUID id, ResetMotDePasseAdminRequest req) {
 
        // Vérifier que confirmation = nouveau mot de passe
        if (!req.getNouveauMotDePasse().equals(req.getConfirmation())) {
            throw new RuntimeException("La confirmation ne correspond pas au nouveau mot de passe");
        }
 
        Utilisateur u = trouverUtilisateur(id);
 
        // CORRECTION P0 : politique NIST (12+ caractères, dictionnaire)
        com.seaside.seaside_api.feature.auth.service.PasswordPolicy
                .valider(req.getNouveauMotDePasse(), u.getEmail());
 
        // Hasher et sauvegarder le nouveau mot de passe
        u.setMotsDePasseHash(passwordEncoder.encode(req.getNouveauMotDePasse()));
        utilisateurRepository.save(u);
 
        // Invalider tous les refresh tokens de cet utilisateur
        refreshTokenService.supprimerParUtilisateur(u);
    }

    // Modification complete par admin (nom + email + role + statut)
    public UtilisateurResponse modifierUtilisateurAdmin(UUID id, ModifierUtilisateurAdminRequest req) {
        Utilisateur u = trouverUtilisateur(id);

        // Verifier doublon email
        if (req.getEmail() != null
                && !req.getEmail().equals(u.getEmail())
                && utilisateurRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Cet email est deja utilise par un autre");
        }

        // Verifier doublon nom
        if (req.getNomUtilisateur() != null
                && !req.getNomUtilisateur().equalsIgnoreCase(u.getNomUtilisateur())
                && utilisateurRepository.existsByEmail(req.getNomUtilisateur())) {
            throw new RuntimeException("Ce nom d'utilisateur est deja pris");
        }

        //Protection : l'admin ne peut pas se désactiver lui-même via cette route
        if (req.getEstActif() != null
                && !req.getEstActif()
                && u.getId().equals(moi().getId())) {
            throw new RuntimeException("Vous ne pouvez pas désactiver votre propre compte admin");
        }

        // Protection : l'admin ne peut pas se changer  son propre role
        if (req.getRole() != null && u.getId().equals(moi().getId())) {
            throw new RuntimeException("Vous ne pouvez pas modifier votre propre rôle");
        }

        // Appliquer seulement les champs fournis (non null)
        if (req.getNomUtilisateur() != null) u.setNomUtilisateur(req.getNomUtilisateur());
        if (req.getEmail() != null)          u.setEmail(req.getEmail());
        if (req.getRole() != null)           u.setRole(req.getRole());
        if (req.getEstActif() != null) {
            u.setEstActif(req.getEstActif());
            // Si désactivé → invalider les refresh tokens
            if (!req.getEstActif()) {
                refreshTokenService.supprimerParUtilisateur(u);
            }
        }
 
        return toResponse(utilisateurRepository.save(u));

    }

}
