package com.seaside.seaside_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_slave")
    private UUID id;

    @Column(name = "mac_address", unique = true, nullable = false)
    private String macAddress;

    @Column(name = "slave_number", nullable = false)
    private Integer slaveNumber;

    @Column(name = "nom", length = 100)
    private String nom;

    @Column(name = "slave_id", length = 50)
    private String slaveId;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "connecte")
    private Boolean connecte;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "seuil")
    private Integer seuil;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_master", nullable = false)
    @JsonIgnore
    private Esp32Master master;
}