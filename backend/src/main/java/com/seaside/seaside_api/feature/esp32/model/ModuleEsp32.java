package com.seaside.seaside_api.feature.esp32.model;
import com.seaside.seaside_api.feature.evenement.model.Evenement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "module_esp32")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor @Builder
public class ModuleEsp32 {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Adresse MAC unique de l'ESP32 — identifiant physique
    @Column(name = "adresse_mac", nullable = false, unique = true, length = 50)
    private String adresseMac; // ex: "AA:BB:CC:DD:EE:FF"

    @Column(name = "nom", length = 100)
    private String nom; // ex: "Master entree principale

    // Eveneement auquel ce module est connecté (relation ManyToOne)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evenement_id")
    private Evenement evenement;

    // batterie du master en pourcerntage
    @Column(name = "batterie_master")
    @Builder.Default
    private Integer batterieMaster = 100; // Niveau de batterie du module maître (0-100%)

    @Column(name = "est_connecte")
    @Builder.Default
    private Boolean estConnecte = false;

    @Column(name = "derniere_activite")
    private LocalDateTime derniereActivite;

    @Column(name = "cree_le", updatable = false)
    private LocalDateTime creeLe;

    // Slaves connectes à ce module master (relation OneToMany)
    @OneToMany(mappedBy = "master", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SlaveModule> slaves = new ArrayList<>();

    @Deprecated
    protected void onCreate() {
        this.creeLe = LocalDateTime.now();
    }

}