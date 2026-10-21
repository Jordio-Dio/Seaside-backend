package com.seaside.seaside_api.feature.auth.service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * CORRECTION P0 — Verrouillage du compte après échecs répétés.
 *
 * 5 échecs consécutifs sur un même email → blocage 15 minutes,
 * indépendamment du reste (limite le brute-force sur /auth/login).
 *
 * Note : stockage en mémoire (un seul instance du backend). Si l'équipe
 * passe à plusieurs instances, migrer vers une table ou Redis.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ECHECS = 5;
    private static final long DUREE_BLOCAGE_MINUTES = 15;

    private static final class Tentative {
        int echecs;
        Instant verrouilleJusquA;
    }

    private final Map<String, Tentative> tentatives = new ConcurrentHashMap<>();

    private static String clef(String email) {
        return email == null ? "" : email.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Appelé AVANT l'authentification. Lance une RuntimeException
     * (→ 400 via GlobalExceptionHandler) si le compte est temporairement bloqué.
     * Le message reste volontairement générique (pas d'énumération de comptes).
     */
    public void verifierAvantTentative(String email) {
        Tentative t = tentatives.get(clef(email));
        if (t != null && t.verrouilleJusquA != null) {
            if (Instant.now().isBefore(t.verrouilleJusquA)) {
                long secondesRestantes = java.time.Duration
                        .between(Instant.now(), t.verrouilleJusquA)
                        .getSeconds();
                throw new RuntimeException(
                        "Trop de tentatives de connexion — réessayez dans "
                                + Math.max(1, (secondesRestantes + 59) / 60)
                                + " minute(s)");
            }
            // Blocage expiré → on repart à zéro
            tentatives.remove(clef(email));
        }
    }

    /** Mauvais mot de passe → incrémente et verrouille si seuil atteint. */
    public void enregistrerEchec(String email) {
        Tentative t = tentatives.computeIfAbsent(clef(email), k -> new Tentative());
        t.echecs++;
        if (t.echecs >= MAX_ECHECS) {
            t.verrouilleJusquA = Instant.now()
                    .plusSeconds(DUREE_BLOCAGE_MINUTES * 60);
            t.echecs = 0;
        }
    }

    /** Connexion réussie → remise à zéro. */
    public void reinitialiser(String email) {
        tentatives.remove(clef(email));
    }
}
