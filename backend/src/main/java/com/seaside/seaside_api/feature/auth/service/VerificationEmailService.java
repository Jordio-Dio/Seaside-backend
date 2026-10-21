package com.seaside.seaside_api.feature.auth.service;


import com.seaside.seaside_api.feature.auth.model.VerificationOtp;
import com.seaside.seaside_api.feature.utilisateur.repository.UtilisateurRepository;
import com.seaside.seaside_api.feature.auth.repository.VerificationOtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Hashtable;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VerificationEmailService {

    private final VerificationOtpRepository otpRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmailService emailService;

    // ─── Étape 1 : envoyer OTP ──────────────────────────────
    public void envoyerOtp(String email) {

        // Vérifier que l'email n'est pas déjà utilisé
        if (utilisateurRepository.existsByEmail(email)) {
            throw new RuntimeException("Cet email est déjà associé à un compte");
        }

        // Vérifier que le domaine email a des enregistrements MX
        // (vérifie que le domaine peut recevoir des emails)
        String domaine = email.substring(email.indexOf('@') + 1);
        if (!domaineMxExiste(domaine)) {
            throw new RuntimeException("Adresse email invalide — ce domaine n'existe pas");
        }

        // CORRECTION P0 — cooldown : un nouvel OTP ne peut être demandé
        // qu'une fois par minute (limite l'envoi d'emails et le spam)
        Optional<VerificationOtp> existant = otpRepository.findFirstByEmailOrderByCreeLeDesc(email);
        if (existant.isPresent()
                && existant.get().getCreeLe() != null
                && existant.get().getCreeLe().isAfter(LocalDateTime.now().minusSeconds(60))) {
            throw new RuntimeException("Code déjà envoyé — attendez 60 secondes avant de redemander");
        }

        // Supprimer les anciens OTP de cet email
        otpRepository.deleteByEmail(email);

        // Générer un code à 6 chiffres sécurisé
        String code = genererCode6Chiffres();

        // Sauvegarder en BDD avec expiration 5 min (CORRECTION P0)
        VerificationOtp otp = VerificationOtp.builder()
                .email(email)
                .code(code)
                .expireLe(LocalDateTime.now().plusMinutes(5))
                .estVerifie(false)
                .tentatives(0)
                .build();
        otpRepository.save(otp);

        // Envoyer l'email (asynchrone — ne bloque pas)
        emailService.envoyerOtpVerification(email, code);

        log.info("OTP généré pour : {}", email);
    }

    // ─── Étape 2 : valider OTP ──────────────────────────────
    // Retourne true si le code est correct et non expiré
    public void validerOtp(String email, String code) {

        // CORRECTION P0 — anti brute-force : on charge le dernier OTP de
        // l'email quel que soit le code fourni, pour compter les tentatives.
        VerificationOtp otp = otpRepository
                .findFirstByEmailOrderByCreeLeDesc(email)
                .orElseThrow(() -> new RuntimeException("Code invalide ou déjà utilisé"));

        if (Boolean.TRUE.equals(otp.getEstVerifie())) {
            throw new RuntimeException("Code invalide ou déjà utilisé");
        }

        if (otp.estExpire()) {
            throw new RuntimeException("Code expiré — demandez un nouveau code");
        }

        // CORRECTION P0 — 5 tentatives maximum par code
        if (otp.getTentatives() == null) {
            otp.setTentatives(0);
        }
        if (otp.getTentatives() >= MAX_TENTATIVES) {
            otpRepository.delete(otp);
            throw new RuntimeException("Trop de tentatives — demandez un nouveau code");
        }

        if (!otp.getCode().equals(code)) {
            otp.setTentatives(otp.getTentatives() + 1);
            otpRepository.save(otp);
            int restantes = MAX_TENTATIVES - otp.getTentatives();
            throw new RuntimeException(restantes > 0
                    ? "Code invalide — il vous reste " + restantes + " tentative(s)"
                    : "Trop de tentatives — demandez un nouveau code");
        }

        // Marquer comme vérifié — ne peut plus être réutilisé
        otp.setEstVerifie(true);
        otpRepository.save(otp);

        log.info("Email vérifié avec succès : {}", email);
    }

    // Nombre maximal de saisies d'un même code OTP (CORRECTION P0)
    private static final int MAX_TENTATIVES = 5;

    // ─── Vérifier qu'un email a été validé avant inscription ─
    public boolean emailEstVerifie(String email) {
        return otpRepository.existsByEmailAndEstVerifieTrue(email);
    }

    // ─── Génération du code 6 chiffres sécurisé ─────────────
    private String genererCode6Chiffres() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // 100000 à 999999
        return String.valueOf(code);
    }

    // ─── Vérification DNS MX du domaine ─────────────────────
    // Vérifie que le domaine de l'email peut recevoir des emails
    // ex: gmail.com → a des enregistrements MX → valide
    //     fakedomainxyz123.com → pas de MX → rejeté
    private boolean domaineMxExiste(String domaine) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial",
                    "com.sun.jndi.dns.DnsContextFactory");
            env.put("java.naming.provider.url", "dns:");

            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domaine, new String[]{"MX"});
            ctx.close();

            // Si l'attribut MX existe et a des valeurs → domaine valide
            return attrs.get("MX") != null && attrs.get("MX").size() > 0;
        } catch (Exception e) {
            log.warn("Domaine MX invalide pour : {} — {}", domaine, e.getMessage());
            return false; // domaine inexistant ou erreur DNS
        }
    }
}
