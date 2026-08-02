package com.seaside.seaside_api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.seaside.seaside_api.entity.Categorie;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, UUID>{

    List<Categorie> findByEvenementId(UUID evenementId);

    Optional<Categorie> findByIdAndEvenementId(UUID id, UUID evenementId);

    List<Categorie> findByEvenementIdAndEstActif(UUID evenementId, Boolean estActif);   

    Optional<Categorie> findFirstByCouleur(String couleur);

    @Query("SELECT COALESCE(SUM(e.comptage), 0) FROM Entree e WHERE e.categorie.id = :categorieId")
    int getNombreEntreesByCategorieId(@Param("categorieId") UUID categorieId);  
} 
