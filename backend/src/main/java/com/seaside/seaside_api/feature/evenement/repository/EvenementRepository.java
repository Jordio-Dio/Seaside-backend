package com.seaside.seaside_api.feature.evenement.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seaside.seaside_api.feature.evenement.model.Evenement;

@Repository
public interface EvenementRepository extends JpaRepository<Evenement, UUID> {

    // Tous les evenements d'un utilisateur
    List<Evenement> findByUtilisateurId(UUID utilisateurId);

    // Un evenement precis appartenant a un utilisateur (Securite et isolation)
    Optional<Evenement> findByIdAndUtilisateurId(UUID id, UUID utilisateurId);

    // Seulement les evenements les evenements actifs d'un utilisateur
    List<Evenement> findByUtilisateurIdAndEstActif(UUID utilisateurId, Boolean estActif);

    // ─── Dashboard admin ───────────────────────────────────
    long countByEstActif(boolean estActif);

}