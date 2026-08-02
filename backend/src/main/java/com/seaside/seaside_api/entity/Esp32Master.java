package com.seaside.seaside_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "esp32_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Esp32Master {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_master")
    private UUID id;

    @Column(name = "mac_address", unique = true, nullable = false, length = 17)
    private String macAddress;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "nom", length = 100)
    private String nom;

    @Column(name = "licence", unique = true, length = 50)
    private String licence;

    @Column(name = "module_id", length = 50)
    private String moduleId;

    @Column(name = "seuil")
    private Integer seuil;

    @Column(name = "connected")
    private Boolean connected;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_category")
    @JsonIgnore
    private Categorie category;

    @OneToMany(mappedBy = "master", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Esp32Slave> slaves = new ArrayList<>();
}