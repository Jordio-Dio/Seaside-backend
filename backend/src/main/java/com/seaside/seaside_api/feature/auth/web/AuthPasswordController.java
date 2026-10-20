package com.seaside.seaside_api.feature.auth.web;

import com.seaside.seaside_api.feature.auth.dto.ConfirmOtpRequest;
import com.seaside.seaside_api.feature.auth.dto.ForgotPasswordRequest;
import com.seaside.seaside_api.feature.auth.dto.ResetPasswordRequest;
import com.seaside.seaside_api.feature.auth.dto.VerifyEmailRequest;
import com.seaside.seaside_api.feature.auth.dto.OtpConfirmResponse;
import com.seaside.seaside_api.feature.auth.dto.OtpResponse;
import com.seaside.seaside_api.feature.auth.dto.PasswordResetResponse;
import com.seaside.seaside_api.feature.auth.service.PasswordResetService;
import com.seaside.seaside_api.feature.auth.service.VerificationEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthPasswordController {

    private final VerificationEmailService verificationEmailService;
    private final PasswordResetService passwordResetService;

    // ─────────────────────────────────────────────────────────
    // VÉRIFICATION EMAIL AVANT INSCRIPTION
    // ─────────────────────────────────────────────────────────

    // ÉTAPE 1 — Envoyer le code OTP
    // POST /auth/verify-email
    @PostMapping("/verify-email")
    public ResponseEntity<OtpResponse> envoyerOtp(
            @Valid @RequestBody VerifyEmailRequest req) {
        verificationEmailService.envoyerOtp(req.getEmail());
        return ResponseEntity.ok(OtpResponse.builder()
                .message("Code envoyé à " + req.getEmail() + " — valable 10 minutes")
                .email(req.getEmail())
                .build());
    }

    // ÉTAPE 2 — Confirmer le code OTP
    // POST /auth/confirm-otp
    @PostMapping("/confirm-otp")
    public ResponseEntity<OtpConfirmResponse> confirmerOtp(
            @Valid @RequestBody ConfirmOtpRequest req) {
        verificationEmailService.validerOtp(req.getEmail(), req.getCode());
        return ResponseEntity.ok(OtpConfirmResponse.builder()
                .message("Email vérifié avec succès")
                .emailVerifie(req.getEmail())
                .verified(true)
                .build());
    }

    // ─────────────────────────────────────────────────────────
    // RÉCUPÉRATION MOT DE PASSE OUBLIÉ
    // ─────────────────────────────────────────────────────────

    // ÉTAPE 1 — Demander le lien de reset
    // POST /auth/forgot-password
    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req) {
        passwordResetService.demanderReset(req.getEmail());
        return ResponseEntity.ok(PasswordResetResponse.builder()
                .message("Si cet email existe, un lien de réinitialisation a été envoyé")
                .build());
    }


    // ÉTAPE 2 — Réinitialiser le mot de passe avec le token
    // POST /auth/reset-password
    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResetResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(
                req.getToken(),
                req.getNouveauMotDePasse(),
                req.getConfirmation()
        );
        return ResponseEntity.ok(PasswordResetResponse.builder()
                .message("Mot de passe réinitialisé avec succès — vous pouvez vous connecter")
                .build());
    }
}