package com.seaside.seaside_api.repository;

import com.seaside.seaside_api.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenAndEstUtiliseFalse(String token);

    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.utilisateur.id = :utilisateurId")
    void deleteByUtilisateurId(UUID utilisateurId);
}