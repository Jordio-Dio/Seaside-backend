package com.seaside.seaside_api.service;


import com.seaside.seaside_api.entity.VerificationOtp;
import com.seaside.seaside_api.repository.UtilisateurRepository;
import com.seaside.seaside_api.repository.VerificationOtpRepository;
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

        // Supprimer les anciens OTP de cet email
        otpRepository.deleteByEmail(email);

        // Générer un code à 6 chiffres sécurisé
        String code = genererCode6Chiffres();

        // Sauvegarder en BDD avec expiration 10 min
        VerificationOtp otp = VerificationOtp.builder()
                .email(email)
                .code(code)
                .expireLe(LocalDateTime.now().plusMinutes(10))
                .estVerifie(false)
                .build();
        otpRepository.save(otp);

        // Envoyer l'email (asynchrone — ne bloque pas)
        emailService.envoyerOtpVerification(email, code);

        log.info("OTP généré pour : {}", email);
    }

    // ─── Étape 2 : valider OTP ──────────────────────────────
    // Retourne true si le code est correct et non expiré
    public void validerOtp(String email, String code) {
        VerificationOtp otp = otpRepository
                .findByEmailAndCodeAndEstVerifieFalse(email, code)
                .orElseThrow(() -> new RuntimeException("Code invalide ou déjà utilisé"));

        if (otp.estExpire()) {
            throw new RuntimeException("Code expiré — demandez un nouveau code");
        }

        // Marquer comme vérifié — ne peut plus être réutilisé
        otp.setEstVerifie(true);
        otpRepository.save(otp);

        log.info("Email vérifié avec succès : {}", email);
    }

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
