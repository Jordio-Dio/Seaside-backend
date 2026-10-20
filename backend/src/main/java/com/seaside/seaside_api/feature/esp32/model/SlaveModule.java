package com.seaside.seaside_api.feature.esp32.model;
import com.seaside.seaside_api.feature.evenement.model.Categorie;


import jakarta.persistence.*;
import lombok.*;
 
import java.time.LocalDateTime;
import java.util.UUID;
 
@Entity
@Table(name = "slaves_modules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SlaveModule {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    // Numéro du slave (1, 2, 3...) — connu du firmware
    @Column(name = "slave_id", nullable = false)
    private Integer slaveId;
 
    @Column(name = "nom", length = 100)
    private String nom; // ex: "Slave Porte VIP"
 
    // Slave rattaché au master
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_esp32_id", nullable = false)
    private ModuleEsp32 master;
 
    // ─── CLEF DU SYSTÈME ────────────────────────────────────
    // Un slave = une porte physique = une catégorie d'entrée
    // C'est ici qu'on fait l'association dynamique
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id")
    private Categorie categorie; // peut être null avant configuration
 
    // Télémétrie
    @Column(name = "batterie")
    @Builder.Default
    private Integer batterie = 100;
 
    @Column(name = "est_connecte")
    @Builder.Default
    private Boolean estConnecte = false;
 
    @Column(name = "derniere_activite")
    private LocalDateTime derniereActivite;
}