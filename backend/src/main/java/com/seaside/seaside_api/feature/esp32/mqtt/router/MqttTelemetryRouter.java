package com.seaside.seaside_api.feature.esp32.mqtt.router;

import com.seaside.seaside_api.feature.esp32.mqtt.telemetry.MqttTelemetrySubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttTelemetryRouter {

    private final MqttTelemetrySubscriber telemetrySubscriber;

    public void router(String topic, String payload) {

        if (topic.startsWith("seaside/telemetry/")) {

            telemetrySubscriber.traiter(topic, payload);

        } else {

            log.warn("Topic télémétrie non géré : {}", topic);

        }
    }
}