package com.seaside.seaside_api.feature.esp32.mqtt.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seaside.seaside_api.feature.esp32.service.Esp32TelemetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttTelemetrySubscriber {

private final Esp32TelemetryService telemetryService;
private final ObjectMapper objectMapper;

public void traiter(String topic, String payload) {
    log.info("MQTT TELEMETRY ← topic: {} | payload: {}", topic, payload);

    try {
        if (topic.equals("seaside/telemetry/master")) {
            traiterBatterieMaster(payload);

        } else if (topic.startsWith("seaside/telemetry/slave/")) {
            traiterEtatSlave(topic, payload);

        } else {
            log.warn("Topic télémétrie non géré : {}", topic);
        }

    } catch (Exception e) {
        log.error("Erreur traitement télémétrie MQTT : {}", e.getMessage(), e);
    }
}

private void traiterBatterieMaster(String payload) throws Exception {
    Map<?, ?> data = objectMapper.readValue(payload, Map.class);

    String masterMac = data.get("masterMac").toString();
    int batterie = Integer.parseInt(data.get("batterie").toString());

    telemetryService.traiterBatterieMaster(masterMac, batterie);

    log.info("Batterie master {} : {}%", masterMac, batterie);
}

private void traiterEtatSlave(String topic, String payload) throws Exception {

    String[] parts = topic.split("/");

    if (parts.length < 4) {
        log.warn("Topic slave invalide : {}", topic);
        return;
    }

    int slaveNumber = Integer.parseInt(parts[3]);

    Map<?, ?> data = objectMapper.readValue(payload, Map.class);

    String masterMac = data.get("masterMac").toString();
    int batterie = Integer.parseInt(data.get("batterie").toString());
    boolean connecte = Boolean.parseBoolean(data.get("connecte").toString());

    telemetryService.traiterEtatSlave(
            masterMac,
            slaveNumber,
            batterie,
            connecte
    );

    log.info(
            "Slave {} | master: {} | batterie: {}% | connecté: {}",
            slaveNumber,
            masterMac,
            batterie,
            connecte
    );
}

}
