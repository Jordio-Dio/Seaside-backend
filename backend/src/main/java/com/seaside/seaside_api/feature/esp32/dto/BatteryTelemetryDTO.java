package com.seaside.seaside_api.feature.esp32.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatteryTelemetryDTO {

    private UUID id;

    private UUID slaveId;

    private Integer batteryLevel;

    private Integer signalStrength;

    private Double temperature;

    private LocalDateTime recordedAt;
}