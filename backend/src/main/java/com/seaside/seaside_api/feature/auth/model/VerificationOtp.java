package com.seaside.seaside_api.feature.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// ─── Stocke les OTP de vérification email avant inscription ─
@Entity
@Table(name = "verification_otp")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VerificationOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    // Code à 6 chiffres 
    @Column(name = "code", nullable = false, length = 6)
    private String code;

    // Expire dans 5 minutes (CORRECTION P0 — réduit de 10 à 5)
    @Column(name = "expire_le", nullable = false)
    private LocalDateTime expireLe;

    // CORRECTION P0 : nombre de tentatives de saisie du code.
    // Atteint 5 → le code est refusé définitivement (anti brute-force).
    @Column(name = "tentatives", nullable = false)
    @Builder.Default
    private Integer tentatives = 0;

    // True une fois que l'utilisateur a validé le code
    // Empêche la réutilisation du même OTP
    @Column(name = "est_verifie", nullable = false)
    @Builder.Default
    private Boolean estVerifie = false;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;

    @PrePersist
    protected void onCreate() {
        this.creeLe = LocalDateTime.now();
    }

    public boolean estExpire() {
        return LocalDateTime.now().isAfter(expireLe);
    }
}

