package com.seaside.seaside_api.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "esp32_slave")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Esp32Slave {

    @Id
    @GeneratedValue
    @Column(name = "id_slave")
    private UUID id;

    @Column(name = "slave_number", nullable = false)
    private Integer slaveNumber;

    @Column(name = "mac_address", nullable = false, unique = true)
    private String macAddress;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "connecte")
    private Boolean connecte;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_master", nullable = false)
    private Esp32Master master;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_category", nullable = false)
    private Categorie categorie;

    @OneToOne(mappedBy = "slave", cascade = CascadeType.ALL, orphanRemoval = true)
    private License license;

    @OneToMany(mappedBy = "slave", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BatteryTelemetry> telemetries = new ArrayList<>();

    @OneToMany(mappedBy = "slave", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EntryCount> entryCounts = new ArrayList<>();
}