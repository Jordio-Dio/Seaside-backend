package com.seaside.seaside_api.feature.esp32.mqtt.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seaside.seaside_api.feature.esp32.service.SeasideMqttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeasideMqttSubscriber {

    private final SeasideMqttService mqttService;
    private final ObjectMapper objectMapper;

    public void traiterEntree(String topic, String payload) {
        try {
            mqttService.traiterEntree(topic, lire(payload));
        } catch (Exception e) {
            log.error("Erreur traitement entrée — topic: {} | {}", topic, e.getMessage());
        }
    }

    public void traiterTelemetrieMaster(String payload) {
        try {
            mqttService.traiterTelemetrieMaster(lire(payload));
        } catch (Exception e) {
            log.error("Erreur traitement télémétrie master | {}", e.getMessage());
        }
    }

    public void traiterTelemetrieSlave(String topic, String payload) {
        try {
            mqttService.traiterTelemetrieSlave(lire(payload));
        } catch (Exception e) {
            log.error("Erreur traitement télémétrie slave — topic: {} | {}", topic, e.getMessage());
        }
    }

    public void traiterAlerte(String payload) {
        try {
            mqttService.traiterAlerte(lire(payload));
        } catch (Exception e) {
            log.error("Erreur traitement alerte | {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> lire(String payload) throws Exception {
        return objectMapper.readValue(payload, Map.class);
    }
}
