package com.seaside.seaside_api.feature.organisateur.model;

/**
 * Machine à états de la demande d'organisateur (workflow recommandé §3.1) :
 * EN_ATTENTE → APPROUVEE | REFUSEE ; IGNOREE = mise de côté (remise en file) ;
 * EXPIREE = dépassée de 7 jours (job planifié à venir).
 */
public enum StatutDemande {
    EN_ATTENTE,
    APPROUVEE,
    REFUSEE,
    IGNOREE,
    EXPIREE
}
