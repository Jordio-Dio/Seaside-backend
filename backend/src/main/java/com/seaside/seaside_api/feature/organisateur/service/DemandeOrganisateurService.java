package com.seaside.seaside_api.feature.organisateur.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seaside.seaside_api.feature.auth.service.EmailService;
import com.seaside.seaside_api.feature.organisateur.dto.DemandeOrganisateurRequest;
import com.seaside.seaside_api.feature.organisateur.dto.DemandeOrganisateurResponse;
import com.seaside.seaside_api.feature.organisateur.dto.TraiterDemandeRequest;
import com.seaside.seaside_api.feature.organisateur.model.DemandeOrganisateur;
import com.seaside.seaside_api.feature.organisateur.model.StatutDemande;
import com.seaside.seaside_api.feature.organisateur.repository.DemandeOrganisateurRepository;
import com.seaside.seaside_api.feature.utilisateur.model.RoleUsers;
import com.seaside.seaside_api.feature.utilisateur.model.Utilisateur;
import com.seaside.seaside_api.feature.utilisateur.repository.UtilisateurRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Workflow de validation des organisateurs (scénario 3 du doc
 * INSCRIPTION_ORGANISATEUR_RECOMMANDATIONS.md §3) :
 *  dépôt → file d'attente admin → approuver (rôle ORGANISATEUR) /
 *  refuser (motif obligatoire) / ignorer (mise de côté, re-traitable).
 * Chaque décision envoie un email au demandeur et est horodatée.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DemandeOrganisateurService {

    private final DemandeOrganisateurRepository demandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmailService emailService;

    private Utilisateur moi() {
        return (Utilisateur) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
    }

    // ─── Côté demandeur ────────────────────────────────────

    @Transactional
    public DemandeOrganisateurResponse deposer(DemandeOrganisateurRequest req) {
        Utilisateur utilisateur = moi();

        if (demandeRepository
                .findByUtilisateurIdAndStatut(utilisateur.getId(), StatutDemande.EN_ATTENTE)
                .isPresent()) {
            throw new RuntimeException("Une demande est déjà en attente de traitement");
        }
        boolean dejaApprouvee = demandeRepository
                .findFirstByUtilisateurIdOrderByDateDemandeDesc(utilisateur.getId())
                .map(d -> d.getStatut() == StatutDemande.APPROUVEE)
                .orElse(false);
        if (dejaApprouvee) {
            throw new RuntimeException("Votre demande a déjà été approuvée");
        }
        if (utilisateur.getRole() == RoleUsers.ORGANISATEUR
                || utilisateur.getRole() == RoleUsers.ADMIN) {
            throw new RuntimeException("Vous êtes déjà organisateur");
        }

        DemandeOrganisateur demande = DemandeOrganisateur.builder()
                .utilisateur(utilisateur)
                .statut(StatutDemande.EN_ATTENTE)
                .message(req != null ? req.getMessage() : null)
                .build();
        demande = demandeRepository.save(demande);
        log.info("Nouvelle demande d'organisateur {} de {}", demande.getId(), utilisateur.getEmail());

        // Notification à tous les admins (best-effort : EmailService avale les erreurs)
        for (Utilisateur admin : utilisateurRepository.findByRole(RoleUsers.ADMIN)) {
            emailService.notifierNouvelleDemandeAdmin(admin.getEmail(), utilisateur.getNomUtilisateur());
        }
        return toResponse(demande);
    }

    public DemandeOrganisateurResponse maDemande() {
        return demandeRepository
                .findFirstByUtilisateurIdOrderByDateDemandeDesc(moi().getId())
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Aucune demande d'organisateur trouvée"));
    }

    // ─── Côté admin (console d'approbation) ────────────────

    public List<DemandeOrganisateurResponse> lister(StatutDemande statut) {
        List<DemandeOrganisateur> liste = (statut == null)
                ? demandeRepository.findAllByOrderByDateDemandeDesc()
                : demandeRepository.findByStatutOrderByDateDemandeDesc(statut);
        return liste.stream().map(this::toResponse).toList();
    }

    @Transactional
    public DemandeOrganisateurResponse approuver(UUID id) {
        DemandeOrganisateur demande = traitable(id);
        Utilisateur utilisateur = demande.getUtilisateur();

        demande.setStatut(StatutDemande.APPROUVEE);
        finaliserTraitement(demande);

        // L'approbation confère le rôle ORGANISATEUR (le JWT suivant portera le nouveau rôle)
        utilisateur.setRole(RoleUsers.ORGANISATEUR);
        utilisateurRepository.save(utilisateur);

        log.info("Demande {} approuvée par {}", id, moi().getEmail());
        emailService.envoyerDecisionDemande(
                utilisateur.getEmail(), utilisateur.getNomUtilisateur(), true, null);
        return toResponse(demande);
    }

    @Transactional
    public DemandeOrganisateurResponse refuser(UUID id, TraiterDemandeRequest req) {
        DemandeOrganisateur demande = traitable(id);
        Utilisateur utilisateur = demande.getUtilisateur();

        demande.setStatut(StatutDemande.REFUSEE);
        demande.setMotif(req.getMotif());
        finaliserTraitement(demande);

        log.info("Demande {} refusée par {}", id, moi().getEmail());
        emailService.envoyerDecisionDemande(
                utilisateur.getEmail(), utilisateur.getNomUtilisateur(), false, req.getMotif());
        return toResponse(demande);
    }

    @Transactional
    public DemandeOrganisateurResponse ignorer(UUID id) {
        DemandeOrganisateur demande = traitable(id);
        demande.setStatut(StatutDemande.IGNOREE);
        finaliserTraitement(demande);
        log.info("Demande {} mise de côté par {}", id, moi().getEmail());
        return toResponse(demande);
    }

    // ─── Utilitaires ───────────────────────────────────────

    private DemandeOrganisateur traitable(UUID id) {
        DemandeOrganisateur demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande introuvable"));
        if (demande.getStatut() != StatutDemande.EN_ATTENTE
                && demande.getStatut() != StatutDemande.IGNOREE) {
            throw new RuntimeException(
                    "Cette demande a déjà été traitée (" + demande.getStatut() + ")");
        }
        return demande;
    }

    private void finaliserTraitement(DemandeOrganisateur demande) {
        demande.setDateTraitement(LocalDateTime.now());
        demande.setTraiteParId(moi().getId());
        demandeRepository.save(demande);
    }

    private DemandeOrganisateurResponse toResponse(DemandeOrganisateur d) {
        Utilisateur u = d.getUtilisateur();
        return DemandeOrganisateurResponse.builder()
                .id(d.getId())
                .utilisateurId(u.getId())
                .nomUtilisateur(u.getNomUtilisateur())
                .email(u.getEmail())
                .statut(d.getStatut())
                .message(d.getMessage())
                .motif(d.getMotif())
                .dateDemande(d.getDateDemande())
                .dateTraitement(d.getDateTraitement())
                .traiteParId(d.getTraiteParId())
                .build();
    }
}
