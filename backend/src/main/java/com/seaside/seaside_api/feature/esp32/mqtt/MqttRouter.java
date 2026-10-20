package com.seaside.seaside_api.feature.esp32.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

// ─────────────────────────────────────────────────────────────
// MqttRouter — composant intermédiaire SANS dépendances circulaires
// Il reçoit les messages MQTT et délègue à MqttSubscriber
// en utilisant ApplicationContext pour éviter tout cycle
// ─────────────────────────────────────────────────────────────
@Slf4j
@Component
public class MqttRouter {

    private final ApplicationContext applicationContext;

    // ApplicationContext n'a pas de dépendances circulaires
    // Spring l'injecte toujours sans problème
    public MqttRouter(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void router(String topic, String payload) {
        // Récupérer MqttSubscriber seulement au moment de l'appel
        // pas au démarrage — casse le cycle complètement
        MqttSubscriber subscriber = applicationContext.getBean(MqttSubscriber.class);
        subscriber.traiter(topic, payload);
    }
}