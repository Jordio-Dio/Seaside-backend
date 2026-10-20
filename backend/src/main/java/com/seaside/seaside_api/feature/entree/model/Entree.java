package com.seaside.seaside_api.feature.entree.model;
import com.seaside.seaside_api.feature.evenement.model.Categorie;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "entrees")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entree {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id", nullable = false)
    @JsonIgnore
    private Categorie categorie;

    @Min(1)
    @Column(name = "comptage", nullable = false)
    @Builder.Default
    private Integer comptage = 1;

    @NotBlank
    @Column(name = "source", nullable = false, length = 50)
    private String source = "esp32"; // Esp32 ou Manuel

    @Column(name = "enregistre_le", nullable = false, updatable = false)
    private LocalDateTime enregistreLe;

    // --------------Hook JPA--------------
    @PrePersist
    public void prePersist() {
        this.enregistreLe = LocalDateTime.now();
    }

    public void enregistrer() {
        this.enregistreLe = LocalDateTime.now();
    }
}
