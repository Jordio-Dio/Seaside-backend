package com.seaside.seaside_api.feature.auth.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String expediteur;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // ─── Envoyer OTP de vérification email ──────────────────
    // @Async : ne bloque pas la requête HTTP pendant l'envoi
    @Async
    public void envoyerOtpVerification(String email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(expediteur);
            helper.setTo(email);
            helper.setSubject("Seaside 2.0 — Code de vérification");
            helper.setText(construireEmailOtp(code), true); // true = HTML

            mailSender.send(message);
            log.info("OTP envoyé à : {}", email);
        } catch (Exception e) {
            log.error("Erreur envoi OTP à {} : {}", email, e.getMessage());
        }
    }

    // ─── Envoyer lien de réinitialisation mot de passe ──────
    @Async
    public void envoyerResetPassword(String email, String token, String nomUtilisateur) {
        try {
            String lien = frontendUrl + "/reset-password?token=" + token;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(expediteur);
            helper.setTo(email);
            helper.setSubject("Seaside 2.0 — Réinitialisation de votre mot de passe");
            helper.setText(construireEmailReset(nomUtilisateur, lien), true);

            mailSender.send(message);
            log.info("Email reset envoyé à : {}", email);
        } catch (Exception e) {
            log.error("Erreur envoi reset à {} : {}", email, e.getMessage());
        }
    }

    // ─── Notification : nouvelle demande d'organisateur (aux admins) ──
    @Async
    public void notifierNouvelleDemandeAdmin(String emailAdmin, String nomDemandeur) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(expediteur);
            helper.setTo(emailAdmin);
            helper.setSubject("Seaside 2.0 — Nouvelle demande d'organisateur");
            helper.setText(construireEmailNouvelleDemande(nomDemandeur), true);

            mailSender.send(message);
            log.info("Notification demande envoyée à l'admin : {}", emailAdmin);
        } catch (Exception e) {
            log.error("Erreur notification demande à {} : {}", emailAdmin, e.getMessage());
        }
    }

    // ─── Décision sur une demande (au demandeur) ──────────────
    @Async
    public void envoyerDecisionDemande(String email, String nom, boolean approuve, String motif) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(expediteur);
            helper.setTo(email);
            helper.setSubject(approuve
                    ? "Seaside 2.0 — Votre demande d'organisateur est approuvée"
                    : "Seaside 2.0 — Votre demande d'organisateur a été refusée");
            helper.setText(construireEmailDecision(nom, approuve, motif), true);

            mailSender.send(message);
            log.info("Email décision demande envoyé à : {} (approuve={})", email, approuve);
        } catch (Exception e) {
            log.error("Erreur email décision à {} : {}", email, e.getMessage());
        }
    }

    // ─── Templates HTML des emails ───────────────────────────
    private String construireEmailOtp(String code) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:24px">
              <h2 style="color:#1a1a2e">Vérification de votre email</h2>
              <p style="color:#555">Utilisez ce code pour confirmer votre adresse email :</p>
              <div style="background:#f0f4ff;border-radius:8px;padding:20px;text-align:center;margin:20px 0">
                <span style="font-size:36px;font-weight:700;letter-spacing:8px;color:#4F46E5">%s</span>
              </div>
              <p style="color:#888;font-size:13px">Ce code expire dans <strong>5 minutes</strong>.</p>
              <p style="color:#888;font-size:13px">Si vous n'avez pas demandé ce code, ignorez cet email.</p>
            </div>
            """.formatted(code);
    }

    private String construireEmailReset(String nom, String lien) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:24px">
              <h2 style="color:#1a1a2e">Réinitialisation du mot de passe</h2>
              <p style="color:#555">Bonjour <strong>%s</strong>,</p>
              <p style="color:#555">Cliquez sur le bouton ci-dessous pour choisir un nouveau mot de passe :</p>
              <div style="text-align:center;margin:28px 0">
                <a href="%s"
                   style="background:#4F46E5;color:#fff;padding:12px 28px;border-radius:8px;
                          text-decoration:none;font-weight:600;font-size:15px">
                  Réinitialiser mon mot de passe
                </a>
              </div>
              <p style="color:#888;font-size:13px">Ce lien expire dans <strong>15 minutes</strong>.</p>
              <p style="color:#888;font-size:13px">
                Si vous n'avez pas demandé à réinitialiser votre mot de passe,
                ignorez cet email — votre compte reste sécurisé.
              </p>
              <hr style="border:none;border-top:1px solid #eee;margin:20px 0"/>
              <p style="color:#bbb;font-size:11px">Seaside 2.0 — Ne partagez jamais ce lien.</p>
            </div>
            """.formatted(nom, lien);
    }

    private String construireEmailNouvelleDemande(String nomDemandeur) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:24px">
              <h2 style="color:#1a1a2e">Nouvelle demande d'organisateur</h2>
              <p style="color:#555"><strong>%s</strong> vient de déposer une demande
                 pour devenir organisateur.</p>
              <p style="color:#555">Connectez-vous à la console admin pour approuver,
                 refuser (motif obligatoire) ou ignorer cette demande.</p>
              <p style="color:#888;font-size:13px">Une demande reste valable 7 jours.</p>
              <hr style="border:none;border-top:1px solid #eee;margin:20px 0"/>
              <p style="color:#bbb;font-size:11px">Seaside 2.0</p>
            </div>
            """.formatted(nomDemandeur);
    }

    private String construireEmailDecision(String nom, boolean approuve, String motif) {
        if (approuve) {
            return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:24px">
                  <h2 style="color:#0f766e">Demande approuvée ✓</h2>
                  <p style="color:#555">Bonjour <strong>%s</strong>,</p>
                  <p style="color:#555">Votre demande d'organisateur a été approuvée&nbsp;!
                     Votre compte est désormais en rôle <strong>ORGANISATEUR</strong> —
                     reconnectez-vous pour activer vos fonctionnalités.</p>
                  <hr style="border:none;border-top:1px solid #eee;margin:20px 0"/>
                  <p style="color:#bbb;font-size:11px">Seaside 2.0</p>
                </div>
                """.formatted(nom);
        }
        return """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:24px">
              <h2 style="color:#b91c1c">Demande refusée</h2>
              <p style="color:#555">Bonjour <strong>%s</strong>,</p>
              <p style="color:#555">Votre demande d'organisateur a été refusée.</p>
              <div style="background:#fef2f2;border-radius:8px;padding:16px;margin:16px 0">
                <p style="color:#555;font-size:14px;margin:0"><strong>Motif&nbsp;:</strong> %s</p>
              </div>
              <p style="color:#888;font-size:13px">Vous pouvez soumettre une nouvelle demande
                 après correction.</p>
              <hr style="border:none;border-top:1px solid #eee;margin:20px 0"/>
              <p style="color:#bbb;font-size:11px">Seaside 2.0</p>
            </div>
            """.formatted(nom, motif != null ? motif : "non précisé");
    }
}