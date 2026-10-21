package com.seaside.seaside_api.feature.entree.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.seaside.seaside_api.feature.entree.model.Entree;

@Repository
public interface EntreeRepository extends JpaRepository<Entree, UUID>{
    
     List<Entree> findByCategorieIdOrderByEnregistreLeDesc(UUID categorieId);

     // Total personne pour une categorie
     @Query("SELECT COALESCE(SUM(e.comptage), 0) FROM Entree e WHERE e.categorie.id = :categorieId")
     Integer sumComptageByCategorie(@Param("categorieId") UUID categorieId);

     // Total personnes pour tout un evenement (via ses categories)
     @Query("SELECT COALESCE(SUM(e.comptage), 0) FROM Entree e WHERE e.categorie.evenement.id = :evenementId")
     Integer sumComptageByEvenement(@Param("evenementId") UUID evenementId);    // Historique  des 20 dernieres entrees d'un evenement
    @Query("SELECT e FROM Entree e WHERE e.categorie.evenement.id = :evenementId ORDER BY e.enregistreLe DESC")
    List<Entree> findTop20ByEvenementId(@Param("evenementId") UUID evenementId) ;

    // ─── Dashboard admin : totaux globaux ──────────────────
    @Query("SELECT COALESCE(SUM(e.comptage), 0) FROM Entree e")
    Long totalPersonnesToutes();

    @Query("SELECT COALESCE(SUM(e.comptage * e.categorie.prix), 0) FROM Entree e")
    BigDecimal totalRevenusToutes();
}
