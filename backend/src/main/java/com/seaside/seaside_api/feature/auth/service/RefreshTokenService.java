package com.seaside.seaside_api.feature.auth.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.seaside.seaside_api.feature.auth.model.RefreshToken;
import com.seaside.seaside_api.feature.utilisateur.model.Utilisateur;
import com.seaside.seaside_api.feature.auth.repository.RefreshTokenRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;

    // Duree de vie du refresh token : 7 jours
    private static long REFRESH_TOKEN_JOURS = 7;

    //  Creer un nouveau refresh token
    @Transactional
    public RefreshToken creer(Utilisateur utilisateur) {

        // supprimer l'ancien refresh token s'il existe
        refreshTokenRepository.deledeleteByUtilisateur(utilisateur);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString()) // chaine aleatoire unique
                .utilisateur(utilisateur)
                .expireLe(LocalDateTime.now().plusDays(REFRESH_TOKEN_JOURS))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }


    /// Valider  un refresh token
    @Transactional
    public RefreshToken valider(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh Token invalide"));

        if (refreshToken.estExpire()) {
            // Supprimer le token de la BDD
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expire - veuillez vous reconnecter");
        }

        return refreshToken;
    }

    // Supprimer lors du logout
    @Transactional
    public void supprimerParUtilisateur(Utilisateur utilisateur) {
        refreshTokenRepository.deledeleteByUtilisateur(utilisateur);
    }

}
