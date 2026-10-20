package com.seaside.seaside_api.feature.esp32.mqtt.router;

import com.seaside.seaside_api.feature.esp32.mqtt.telemetry.SeasideMqttSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeasideMqttRouter {

    private final SeasideMqttSubscriber subscriber;

    public void router(String topic, String payload) {
        if (topic.startsWith("seaside/entrees/")) {
            subscriber.traiterEntree(topic, payload);

        } else if (topic.equals("seaside/telemetry/master")) {
            subscriber.traiterTelemetrieMaster(payload);

        } else if (topic.startsWith("seaside/telemetry/slave/")) {
            subscriber.traiterTelemetrieSlave(topic, payload);

        } else if (topic.equals("seaside/alerte")) {
            subscriber.traiterAlerte(payload);

        } else {
            log.warn("Topic MQTT non géré : {}", topic);
        }
    }
}
