package com.seaside.seaside_api.feature.esp32.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "battery_telemetry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatteryTelemetry {

    @Id
    @GeneratedValue
    @Column(name = "id_telemetry")
    private UUID id;

    @Column(name = "battery_level", nullable = false)
    private Integer batteryLevel;

    @Column(name = "signal_strength")
    private Integer signalStrength;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_slave", nullable = false)
    private Esp32Slave slave;

    @PrePersist
    public void prePersist() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }
}