package com.seaside.seaside_api.feature.evenement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seaside.seaside_api.feature.evenement.dto.CategorieRequest;
import com.seaside.seaside_api.feature.entree.dto.EntreeRequest;
import com.seaside.seaside_api.feature.evenement.dto.EvenementRequest;
import com.seaside.seaside_api.feature.evenement.model.Categorie;
import com.seaside.seaside_api.feature.entree.model.Entree;
import com.seaside.seaside_api.feature.evenement.model.Evenement;
import com.seaside.seaside_api.feature.utilisateur.model.Utilisateur;
import com.seaside.seaside_api.feature.evenement.repository.CategorieRepository;
import com.seaside.seaside_api.feature.entree.repository.EntreeRepository;
import com.seaside.seaside_api.feature.evenement.repository.EvenementRepository;
import com.seaside.seaside_api.feature.utilisateur.repository.UtilisateurRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EvenementService {

    private final EvenementRepository evenementRepository;
    private final CategorieRepository categorieRepository;
    private final EntreeRepository entreeRepository;

    private final UtilisateurRepository utilisateurRepository;

    private Utilisateur utilisateurConnecte() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    // ─── Récupérer l'utilisateur connecté ───────────────────

    /*
     * private Utilisateur utilisateurConnecte() {
     * var auth = SecurityContextHolder.getContext().getAuthentication();
     * 
     * System.out.println("AUTH = " + auth);
     * System.out.println("PRINCIPAL = " + auth.getPrincipal());
     * 
     * return (Utilisateur) auth.getPrincipal();
     * }
     * 
     * /*
     * Utilisateur utilisateurConnecte() {
     * var auth = SecurityContextHolder.getContext().getAuthentication();
     * 
     * if (auth == null || !auth.isAuthenticated()) {
     * throw new RuntimeException("Utilisateur non authentifié");
     * }
     * 
     * Object principal = auth.getPrincipal();
     * 
     * if (!(principal instanceof Utilisateur utilisateur)) {
     * throw new RuntimeException("Principal invalide : " + principal);
     * }
     * 
     * return utilisateur;
     * }
     */

    // ─── EVENEMENTS ─────────────────────────────────────────

    public List<Evenement> mesEvenements() {
        return evenementRepository.findByUtilisateurId(utilisateurConnecte().getId());
    }

    public Evenement creerEvenement(EvenementRequest req) {

        log.info("=== Entrée dans creerEvenement ===");

        Evenement ev = Evenement.builder()
                .utilisateur(utilisateurConnecte())
                .nom(req.getNom())
                .dateEvenement(req.getDateEvenement())
                .lieu(req.getLieu())
                .description(req.getDescription())
                .capaciteMaximale(req.getCapaciteMaximale())
                .estActif(true)
                .build();

        Evenement saved = evenementRepository.save(ev);

        log.info("=== Événement enregistré : {} ===", saved.getId());

        return saved;
    }

    public Evenement modifierEvenement(UUID id, EvenementRequest req) {
        Evenement ev = trouverMonEvenement(id);
        ev.setNom(req.getNom());
        ev.setDateEvenement(req.getDateEvenement());
        ev.setLieu(req.getLieu());
        ev.setDescription(req.getDescription());
        ev.setCapaciteMaximale(req.getCapaciteMaximale());
        return evenementRepository.save(ev);
    }

    public void supprimerEvenement(UUID id) {
        Evenement ev = trouverMonEvenement(id);
        evenementRepository.delete(ev);
    }

    public Evenement activerEvenement(UUID id) {
        Evenement ev = trouverMonEvenement(id);
        ev.activer();
        return evenementRepository.save(ev);
    }

    public Evenement desactiverEvenement(UUID id) {
        Evenement ev = trouverMonEvenement(id);
        ev.desactiver();
        return evenementRepository.save(ev);
    }

    // ─── CATEGORIES ─────────────────────────────────────────

    public Categorie ajouterCategorie(UUID evenementId, CategorieRequest req) {
        Evenement ev = trouverMonEvenement(evenementId);
        Categorie cat = Categorie.builder()
                .evenement(ev)
                .nom(req.getNom())
                .prix(req.getPrix())
                .capacite(req.getCapacite())
                .couleur(req.getCouleur())
                .estActif(true)
                .build();
        return categorieRepository.save(cat);
    }

    public void supprimerCategorie(UUID evenementId, UUID categorieId) {
        trouverMonEvenement(evenementId); // vérif appartenance
        Categorie cat = categorieRepository.findByIdAndEvenementId(categorieId, evenementId)
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
        categorieRepository.delete(cat);
    }

    // ─── ENTREES MANUELLES ───────────────────────────────────

    public Entree ajouterEntreeManuelle(UUID evenementId, EntreeRequest req) {
        trouverMonEvenement(evenementId); // vérif appartenance

        Categorie cat = categorieRepository.findByIdAndEvenementId(
                req.getCategorieId(), evenementId)
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));

        if (cat.estComplete()) {
            throw new RuntimeException("Capacité maximale atteinte pour la catégorie : " + cat.getNom());
        }

        Entree entree = Entree.builder()
                .categorie(cat)
                .comptage(req.getComptage())
                .source(req.getSource() != null ? req.getSource() : "manuel")
                .build();

        return entreeRepository.save(entree);
    }

    // ─── DASHBOARD ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public Evenement getDashboard(UUID evenementId) {
        return trouverMonEvenement(evenementId);
    }

    // ─── Utilitaire privé ────────────────────────────────────
    private Evenement trouverMonEvenement(UUID id) {
        return evenementRepository.findByIdAndUtilisateurId(id, utilisateurConnecte().getId())
                .orElseThrow(() -> new RuntimeException("Événement introuvable ou accès refusé"));
    }
}