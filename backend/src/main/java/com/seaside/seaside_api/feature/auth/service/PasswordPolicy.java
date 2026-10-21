package com.seaside.seaside_api.feature.auth.service;

import java.util.Locale;
import java.util.Set;

/**
 * CORRECTION P0 — Politique de mots de passe (NIST SP 800-63B) :
 *  - minimum 12 caractères (côté DTO aussi : @Size(min = 12)) ;
 *  - refuse les mots de passe les plus courants (dictionnaire réduit) ;
 *  - refuse le mot de passe contenant l'adresse email locale.
 *
 * Classe utilitaire statique → utilisable par AuthService,
 * PasswordResetService et UtilisateurService (changements de mot de passe).
 */
public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    /**
     * Dictionnaire réduit des mots de passe les plus utilisés (et leurs
     * variantes triviales sont couvertes par la comparaison sans casse).
     * À enrichir avec la liste complète depuis le référentiel de l'équipe.
     */
    private static final Set<String> MOTS_DE_PASSE_INTERDITS = Set.of(
            "password", "passw0rd", "password123", "motdepasse",
            "123456", "123456789", "1234567890", "12345678", "12345",
            "qwerty", "qwertyuiop", "azerty", "azertyuiop",
            "admin", "administrator", "welcome", "bienvenue",
            "letmein", "ilovyou", "football", "monkey", "dragon",
            "sunshine", "princess", "master", "shadow", "superman",
            "abc123", "abcd1234", "aaaaaa", "aaaaaaaaaa",
            "seaside", "smartcount", "seaside123", "smartcount123");

    public static final int LONGUEUR_MINIMALE = 12;

    /**
     * Valide un mot de passe ou lève une RuntimeException (400) avec un
     * message explicite et sûr (ne révèle jamais si le compte existe).
     */
    public static void valider(String motDePasse, String email) {
        if (motDePasse == null || motDePasse.length() < LONGUEUR_MINIMALE) {
            throw new RuntimeException(
                    "Le mot de passe doit faire au moins "
                            + LONGUEUR_MINIMALE + " caractères");
        }

        String normalise = motDePasse.toLowerCase(Locale.ROOT);

        if (MOTS_DE_PASSE_INTERDITS.contains(normalise)) {
            throw new RuntimeException(
                    "Ce mot de passe est trop courant — choisissez-en un autre");
        }

        // Refuser si le mot de passe contient l'adresse email (ou sa partie
        // locale) : usage abusif dans les dictionnaires d'attaque.
        if (email != null && !email.isBlank()) {
            String local = email.substring(0, email.indexOf('@') > 0
                    ? email.indexOf('@')
                    : email.length()).toLowerCase(Locale.ROOT);
            if (local.length() >= 4 && normalise.contains(local)) {
                throw new RuntimeException(
                        "Le mot de passe ne doit pas contenir votre adresse email");
            }
        }
    }
}
