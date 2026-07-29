package com.seaside.seaside_api.entity;

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
    @GeneratedValue
    @Column(name = "id_master")
    private UUID id;

    @Column(name = "mac_address", nullable = false, unique = true)
    private String macAddress;

    @Column(name = "firmware_version")
    private String firmwareVersion;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @OneToMany(mappedBy = "master", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Esp32Slave> slaves = new ArrayList<>();
}
