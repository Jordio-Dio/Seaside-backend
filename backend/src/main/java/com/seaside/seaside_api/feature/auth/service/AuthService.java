package com.seaside.seaside_api.feature.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.seaside.seaside_api.feature.auth.dto.LoginRequest;
import com.seaside.seaside_api.feature.auth.dto.RegisterRequest;
import com.seaside.seaside_api.feature.auth.dto.AuthResponse;
import com.seaside.seaside_api.feature.auth.model.RefreshToken;
import com.seaside.seaside_api.feature.utilisateur.model.Utilisateur;
import com.seaside.seaside_api.feature.utilisateur.model.RoleUsers;
import com.seaside.seaside_api.feature.utilisateur.repository.UtilisateurRepository;
import com.seaside.seaside_api.shared.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    // ─── AJOUT : injecter VerificationEmailService ───────────
    // @RequiredArgsConstructor l'injecte automatiquement via constructeur
    private final VerificationEmailService verificationEmailService;
    private final LoginAttemptService loginAttemptService;


    // ---------- Inscription --------------------
    public AuthResponse inscrire(RegisterRequest request) {

         // ─── NOUVEAU : vérifier que l'email a été validé ─────
        if (!verificationEmailService.emailEstVerifie(request.getEmail())) {
            throw new RuntimeException(
                    "Email non vérifié — validez votre email avant de vous inscrire");
        }

        // Verifier si email deja utilise
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet email est deja utilise");
        }

        // Verifier si nom d'utilisateur deja utilise
        if (utilisateurRepository.existsByNomUtilisateur(request.getNomUtilisateur())) {
            throw new RuntimeException("Ce nom d'utilisateur est deja pris");
        }

        // CORRECTION P0 : politique de mot de passe NIST (longueur + liste commune)
        PasswordPolicy.valider(request.getMotDePasse(), request.getEmail());

        // Creer l'utilisateur avec mot de passe hashe par BCrypt
        Utilisateur utilisateur = Utilisateur.builder()
                .nomUtilisateur(request.getNomUtilisateur())
                .email(request.getEmail())
                .motsDePasseHash(passwordEncoder.encode(request.getMotDePasse()))// ← hash BCrypt
                // CORRECTION P0 : le rôle ne vient JAMAIS de la requête client.
                // Toute inscription libre est CLIENT ; ADMIN est provisionné par un
                // admin existant, ORGANISATEUR est accordé à la validation d'une demande.
                .role(RoleUsers.CLIENT)
                .estActif(true)
                .build();

        utilisateurRepository.save(utilisateur);

        // Generer le token jwt
        String accessToken = jwtUtil.genererToken(utilisateur);
        RefreshToken refresh = refreshTokenService.creer(utilisateur);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refresh.getToken())
                .type("Bearer")
                .email(utilisateur.getEmail())
                .nomUtilisateur(utilisateur.getNomUtilisateur())
                .role(utilisateur.getRole())
                .build();
    }

    // ---------------- Connexion ---------------------
    public AuthResponse seConnecter(LoginRequest request) {

        // CORRECTION P0 : verrouillage après 5 échecs consécutifs (15 min)
        loginAttemptService.verifierAvantTentative(request.getEmail());

        try {
            // Spring security verifie email + mot de passe automatiquement
            // lance une exception si les credentiels sont incorrectes
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getMotDePasse()));
        } catch (BadCredentialsException e) {
            // Échec → compteur de tentatives (verrouillage à partir de 5)
            loginAttemptService.enregistrerEchec(request.getEmail());
            throw e;
        }

        // Succès → remise à zéro du compteur
        loginAttemptService.reinitialiser(request.getEmail());

        // Si on arrive ici , les credentials sont corrects
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        String accesstoken = jwtUtil.genererToken(utilisateur);
        RefreshToken refresh = refreshTokenService.creer(utilisateur);

        return AuthResponse.builder()
                .token(accesstoken)
                .refreshToken(refresh.getToken())
                .type("Bearer")
                .email(utilisateur.getEmail())
                .nomUtilisateur(utilisateur.getNomUtilisateur())
                .role(utilisateur.getRole())
                .build();
    }

    // -----------Renouveler l'access token
    public AuthResponse rafraichir(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenService.valider(refreshTokenStr);
        Utilisateur utilisateur = refreshToken.getUtilisateur();
        String nouvelAccessToken = jwtUtil.genererToken(utilisateur);

        return AuthResponse.builder()
                .token(nouvelAccessToken)
                .refreshToken(refreshTokenStr) // refresh token reste le même
                .type("Bearer")
                .email(utilisateur.getEmail())
                .nomUtilisateur(utilisateur.getNomUtilisateur())
                .role(utilisateur.getRole())
                .build();
    }

    // Deconnexion
    public void seDeconnecter() {
        Utilisateur utilisateur = (Utilisateur) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        refreshTokenService.supprimerParUtilisateur(utilisateur);
    }
}
