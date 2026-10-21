package com.seaside.seaside_api.shared.security;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * CORRECTION P0 — Limitation de débit sur les endpoints sensibles.
 *
 * Fenêtre glissante en mémoire, clé = IP + chemin. Sans dépendance externe.
 *  - /auth/register, /auth/verify-email, /auth/confirm-otp : 5 req/min
 *  - /auth/forgot-password, /auth/reset-password          : 5 req/min
 *  - /auth/login                                           : 10 req/min
 *  - /public/reservations                                  : 30 req/min
 *
 * Réponse 429 au dépassement (format ErrorResponse du GlobalExceptionHandler).
 * Note : stockage local à l'instance — si plusieurs instances, migrer vers
 * un store partagé (Redis) ou un reverse-proxy (nginx limit_req).
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final class Limite {
        final int max;
        final long fenetreSecondes;
        final Deque<Long> hits = new ArrayDeque<>();

        Limite(int max, long fenetreSecondes) {
            this.max = max;
            this.fenetreSecondes = fenetreSecondes;
        }
    }

    /** Règle associée à un préfixe de chemin. */
    private static final class Regle {
        final String prefixe;
        final int max;
        final long fenetreSecondes;

        Regle(String prefixe, int max, long fenetreSecondes) {
            this.prefixe = prefixe;
            this.max = max;
            this.fenetreSecondes = fenetreSecondes;
        }
    }

    private static final Regle[] REGLES = {
            new Regle("/auth/register", 5, 60),
            new Regle("/auth/verify-email", 5, 60),
            new Regle("/auth/confirm-otp", 5, 60),
            new Regle("/auth/forgot-password", 5, 60),
            new Regle("/auth/reset-password", 5, 60),
            new Regle("/auth/login", 10, 60),
            new Regle("/public/reservations", 30, 60),
    };

    private final Map<String, Limite> compteurs = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return reglePour(request) == null;
    }

    private Regle reglePour(HttpServletRequest request) {
        String chemin = request.getRequestURI();
        // Le context-path est /api : on le retire pour matcher les règles
        String contexte = request.getContextPath();
        if (contexte != null && !contexte.isEmpty() && chemin.startsWith(contexte)) {
            chemin = chemin.substring(contexte.length());
        }
        for (Regle r : REGLES) {
            if (chemin.startsWith(r.prefixe)) {
                return r;
            }
        }
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Regle regle = reglePour(request);
        if (regle == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String cle = clientIp(request) + "|" + regle.prefixe;
        long maintenant = Instant.now().getEpochSecond();

        Limite limite = compteurs.computeIfAbsent(cle,
                k -> new Limite(regle.max, regle.fenetreSecondes));

        synchronized (limite) {
            // Purge de la fenêtre glissante
            while (!limite.hits.isEmpty()
                    && limite.hits.peekFirst() < maintenant - limite.fenetreSecondes) {
                limite.hits.pollFirst();
            }

            if (limite.hits.size() >= limite.max) {
                // 429 — format identique à ErrorResponse (code, message, timestamp)
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(
                        "{\"code\":429,"
                                + "\"message\":\"Trop de requêtes — réessayez dans une minute\","
                                + "\"timestamp\":\"" + java.time.LocalDateTime.now() + "\"}");
                return;
            }

            limite.hits.addLast(maintenant);
        }

        filterChain.doFilter(request, response);
    }

    /** IP réelle derrière un proxy (X-Forwarded-For) sinon IP de connexion. */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // Prend la première IP (client d'origine)
            int virgule = xff.indexOf(',');
            return (virgule > 0 ? xff.substring(0, virgule) : xff).trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "inconnu";
    }
}
