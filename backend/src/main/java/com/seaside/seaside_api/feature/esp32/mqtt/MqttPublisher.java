package com.seaside.seaside_api.feature.esp32.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class MqttPublisher {

    private final MqttPahoMessageHandler mqttOutbound;
    private final ObjectMapper objectMapper;

    public MqttPublisher(MqttPahoMessageHandler mqttOutbound,
                         ObjectMapper objectMapper) {
        this.mqttOutbound = mqttOutbound;
        this.objectMapper = objectMapper;
    }

    // ─── Publier un message JSON vers un topic MQTT ──────────
    // Utilisé pour envoyer la config au firmware
    public void publier(String topic, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            mqttOutbound.handleMessage(
                MessageBuilder.withPayload(json)
                    .setHeader("mqtt_topic", topic)
                    .setHeader("mqtt_qos", 1)
                    .build()
            );
            log.info("MQTT → topic: {} | payload: {}", topic, json);
        } catch (Exception e) {
            log.error("Erreur publication MQTT — topic: {} | erreur: {}", topic, e.getMessage());
        }
    }
}