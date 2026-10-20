package com.seaside.seaside_api.feature.auth.model;
import com.seaside.seaside_api.feature.utilisateur.model.Utilisateur;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refresh_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // le refresh token lui-meme (Chaine aleatoire unique)
    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    // A quel utilisateur il appqrtient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur.id", nullable = false)
    private Utilisateur utilisateur;

    // Date expiration (7 jours)
    @Column(name = "expire_le", nullable = false)
    private LocalDateTime expireLe;

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
