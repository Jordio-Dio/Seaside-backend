package com.seaside.seaside_api.feature.organisateur.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seaside.seaside_api.feature.organisateur.model.DemandeOrganisateur;
import com.seaside.seaside_api.feature.organisateur.model.StatutDemande;

@Repository
public interface DemandeOrganisateurRepository extends JpaRepository<DemandeOrganisateur, UUID> {

    List<DemandeOrganisateur> findAllByOrderByDateDemandeDesc();

    List<DemandeOrganisateur> findByStatutOrderByDateDemandeDesc(StatutDemande statut);

    // Dernière demande de l'utilisateur (pour le suivi « ma demande »)
    Optional<DemandeOrganisateur> findFirstByUtilisateurIdOrderByDateDemandeDesc(UUID utilisateurId);

    Optional<DemandeOrganisateur> findByUtilisateurIdAndStatut(UUID utilisateurId, StatutDemande statut);

    long countByStatut(StatutDemande statut);
}
