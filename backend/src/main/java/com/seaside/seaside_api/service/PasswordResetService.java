package com.seaside.seaside_api.service;

import com.seaside.seaside_api.entity.PasswordResetToken;
import com.seaside.seaside_api.entity.Utilisateur;
import com.seaside.seaside_api.repository.PasswordResetTokenRepository;
import com.seaside.seaside_api.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

    // ─── Étape 1 : demande de reset ─────────────────────────
    // On ne dit JAMAIS si l'email existe ou non pour éviter
    // l'énumération d'emails (attaque de sécurité)
    public void demanderReset(String email) {
        utilisateurRepository.findByEmail(email).ifPresent(utilisateur -> {
            // Supprimer les anciens tokens de cet utilisateur
            tokenRepository.deleteByUtilisateurId(utilisateur.getId());

            // Générer un token aléatoire unique
            String token = UUID.randomUUID().toString();

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .utilisateur(utilisateur)
                    .expireLe(LocalDateTime.now().plusMinutes(15))
                    .estUtilise(false)
                    .build();

            tokenRepository.save(resetToken);

            // Envoyer l'email avec le lien
            emailService.envoyerResetPassword(
                    email,
                    token,
                    utilisateur.getNomUtilisateur()
            );

            log.info("Reset password demandé pour : {}", email);
        });
        // Si email inconnu → on ne fait rien ET on ne dit rien
        // L'utilisateur reçoit toujours la même réponse
    }

    // ─── Étape 2 : réinitialiser le mot de passe ────────────
    public void resetPassword(String token, String nouveauMotDePasse, String confirmation) {

        // Vérifier que confirmation = nouveau mot de passe
        if (!nouveauMotDePasse.equals(confirmation)) {
            throw new RuntimeException("La confirmation ne correspond pas au nouveau mot de passe");
        }

        // Trouver le token en BDD
        PasswordResetToken resetToken = tokenRepository
                .findByTokenAndEstUtiliseFalse(token)
                .orElseThrow(() -> new RuntimeException("Lien invalide ou déjà utilisé"));

        // Vérifier l'expiration (15 minutes)
        if (resetToken.estExpire()) {
            throw new RuntimeException("Ce lien a expiré — demandez un nouveau lien");
        }

        Utilisateur utilisateur = resetToken.getUtilisateur();

        // Vérifier que le nouveau mot de passe est différent de l'ancien
        if (passwordEncoder.matches(nouveauMotDePasse, utilisateur.getMotsDePasseHash())) {
            throw new RuntimeException("Le nouveau mot de passe doit être différent de l'ancien");
        }

        // Hasher et sauvegarder le nouveau mot de passe
        utilisateur.setMotsDePasseHash(passwordEncoder.encode(nouveauMotDePasse));
        utilisateurRepository.save(utilisateur);

        // Invalider le token — ne peut plus être réutilisé
        resetToken.setEstUtilise(true);
        tokenRepository.save(resetToken);

        // Invalider tous les refresh tokens — force reconnexion
        refreshTokenService.supprimerParUtilisateur(utilisateur);

        log.info("Mot de passe réinitialisé pour : {}", utilisateur.getEmail());
    }
}