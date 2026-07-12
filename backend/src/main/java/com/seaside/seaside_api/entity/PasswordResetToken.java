package com.seaside.seaside_api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// ─── Stocke les tokens de réinitialisation mot de passe ─────
@Entity
@Table(name = "password_reset_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Token aléatoire unique envoyé par email dans le lien
    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    // À quel utilisateur il appartient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    // Expire dans 15 minutes
    @Column(name = "expire_le", nullable = false)
    private LocalDateTime expireLe;

    // True après utilisation — empêche la réutilisation
    @Column(name = "est_utilise", nullable = false)
    @Builder.Default
    private Boolean estUtilise = false;

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