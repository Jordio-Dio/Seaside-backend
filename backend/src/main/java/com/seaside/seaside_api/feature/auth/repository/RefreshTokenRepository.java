package com.seaside.seaside_api.feature.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.seaside.seaside_api.feature.auth.model.RefreshToken;
import com.seaside.seaside_api.feature.utilisateur.model.Utilisateur;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    // Supprimer tous les refreshTokens d'un utilisateur (lors du deconnexion)
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.utilisateur = :utilisateur")
    void deledeleteByUtilisateur(Utilisateur utilisateur);

}