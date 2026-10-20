package com.seaside.seaside_api.shared.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter{
    
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain 
    ) throws ServletException, IOException {
        
        // Lire le header Authorisations
        final String authHeader = request.getHeader("Authorization");

        // Si pas de token -> on laisse passer les routes publiques
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3 Extraire le token (enlever "Bearer")
        final String token = authHeader.substring(7);
        final String email;

        try {
            email = jwtUtil.extraireEmail(token);
        } catch (Exception e) {
            // Token malforme ou signature invalide
            filterChain.doFilter(request, response);
            return;
        }

        // 4 Si email extrait mais pas encore authentifie
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // 5 Valider le token
            if (jwtUtil.estValide(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6 Enregistrer l'authentication ds le contexte spring Security
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
    
        filterChain.doFilter(request, response);
    }
}
