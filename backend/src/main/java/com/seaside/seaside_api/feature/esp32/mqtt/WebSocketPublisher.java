package com.seaside.seaside_api.feature.esp32.mqtt;

import com.seaside.seaside_api.feature.esp32.dto.AlerteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    // ─── Comptage — nouvelle entrée détectée ────────────────
    // React s'abonne à /topic/comptage/{evenementId}
    public void publierComptage(String evenementId, Map<String, Object> donnees) {
        String dest = "/topic/comptage/" + evenementId;
        messagingTemplate.convertAndSend(dest, donnees);
        log.debug("WS comptage → {}", dest);
    }

    // ─── Télémétrie — batterie master + état slaves ─────────
    // React s'abonne à /topic/telemetrie/{evenementId}
    public void publierTelemetrie(String evenementId, Map<String, Object> donnees) {
        String dest = "/topic/telemetrie/" + evenementId;
        messagingTemplate.convertAndSend(dest, donnees);
        log.debug("WS télémétrie → {}", dest);
    }

    // ─── Alerte — batterie faible, slave déconnecté, etc. ───
    // React s'abonne à /topic/alerte/{evenementId}
    public void publierAlerte(String evenementId, AlerteDTO alerte) {
        String dest = "/topic/alerte/" + evenementId;
        messagingTemplate.convertAndSend(dest, alerte);
        log.info("WS alerte → {} | {}: {}", dest, alerte.getCode(), alerte.getMessage());
    }
}