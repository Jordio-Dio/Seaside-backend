package com.seaside.seaside_api.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seaside.seaside_api.dto.response.AlerteDTO;
import com.seaside.seaside_api.entity.Categorie;
import com.seaside.seaside_api.entity.Entree;
import com.seaside.seaside_api.repository.CategorieRepository;
import com.seaside.seaside_api.repository.EntreeRepository;
import com.seaside.seaside_api.service.Esp32Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.Hibernate;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttSubscriber {

    private final CategorieRepository categorieRepository;
    private final EntreeRepository entreeRepository;
    private final WebSocketPublisher webSocketPublisher;
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    // ─── @Lazy sur Esp32Service pour casser le cycle ────────
    // Cycle : Esp32Service → MqttPublisher → MqttConfig → MqttSubscriber → Esp32Service
    // @Lazy = Spring crée un proxy — Esp32Service est instancié seulement
    // au premier appel réel, pas au démarrage
    @Autowired
    public MqttSubscriber(
            CategorieRepository categorieRepository,
            EntreeRepository entreeRepository,
            WebSocketPublisher webSocketPublisher,
            ObjectMapper objectMapper,
            ApplicationContext applicationContext) {
        this.categorieRepository = categorieRepository;
        this.entreeRepository    = entreeRepository;
        this.webSocketPublisher  = webSocketPublisher;
        this.objectMapper        = objectMapper;
        this.applicationContext  = applicationContext;
    }

    // Recuperer Esp32 au moment de l'appel
    private Esp32Service esp32Service() {
        return applicationContext.getBean(Esp32Service.class);
    }

    // ─── Point d'entrée : appelé par MqttConfig ─────────────
    @Transactional
    // ─── Point d'entrée unique — dispatche selon le topic ───
    public void traiter(String topic, String payload) {
        log.info("MQTT ← topic: {} | payload: {}", topic, payload);
        try {
            if (topic.startsWith("seaside/entrees/")) {
                traiterEntree(topic, payload);

            } else if (topic.equals("seaside/telemetry/master")) {
                traiterBatterieMaster(payload);

            } else if (topic.startsWith("seaside/telemetry/slave/")) {
                traiterEtatSlave(topic, payload);

            } else if (topic.equals("seaside/status/slaves")) {
                traiterEtatSlave(topic, payload);

            } else if (topic.equals("seaside/alerte")) {
                traiterAlerteFirmware(payload);

            } else {
                log.warn("Topic MQTT non géré : {}", topic);
            }
        } catch (Exception e) {
            log.error("Erreur traitement MQTT — topic: {} | erreur: {}", topic, e.getMessage());
        }
    }

    // ─── 1. Entrée détectée par un slave ────────────────────
    // Topic : seaside/entrees/{categorieId}
    // Payload : {"comptage": 1, "mac": "AA:BB:CC:DD:EE:FF"}
    private void traiterEntree(String topic, String payload) throws Exception {
        String[] parts = topic.split("/");
        if (parts.length < 3) {
            log.warn("Topic entrée invalide : {}", topic);
            return;
        }

        UUID categorieId = UUID.fromString(parts[2]);
        Map<?, ?> data = objectMapper.readValue(payload, Map.class);
        int comptage = data.containsKey("comptage")
                ? Integer.parseInt(data.get("comptage").toString())
                : 1;

        Optional<Categorie> catOpt = categorieRepository.findById(categorieId);
        if (catOpt.isEmpty()) {
            log.warn("Catégorie introuvable : {}", categorieId);
            return;
        }

        Categorie cat = catOpt.get();
        String evenementId = cat.getEvenement().getId().toString();

        // verifier capacite
        if (cat.estComplete()) {
            webSocketPublisher.publierAlerte(evenementId, AlerteDTO.builder()
                    .code("CAPACITE_MAX")
                    .message("Capacité maximale atteinte pour : " + cat.getNom())
                    .niveau("WARN")
                    .declencheLe(LocalDateTime.now())
                    .build());
            return;
        }

        // Sauvegarder l'entrée
        Entree entree = Entree.builder()
                .categorie(cat)
                .comptage(comptage)
                .source("esp32")
                .build();
        entreeRepository.save(entree);

        log.info(" Entrée — catégorie: {} | comptage: {}", cat.getNom(), comptage);

        // Pousser la mise à jour comptage au frontend
        webSocketPublisher.publierComptage(
                cat.getEvenement().getId().toString(),
                Map.of(
                        "categorieId", categorieId.toString(),
                        "nomCategorie", cat.getNom(),
                        "comptage", comptage,
                        "totalCategorie", cat.getNombreEntrees(),
                        "revenuCategorie", cat.getRevenuCategorie(),
                        "totalEvenement", cat.getEvenement().getTotalPersonnes(),
                        "totalRevenus", cat.getEvenement().getTotalRevenus()));
    }

    // ─── 2. Batterie du master ───────────────────────────────
    // Topic   : seaside/telemetry/master
    // Payload : {"mac": "AA:BB:CC:DD:EE:FF", "batterie": 85}
    private void traiterBatterieMaster(String payload) throws Exception {
        Map<?, ?> data = objectMapper.readValue(payload, Map.class);
        String mac     = data.get("mac").toString();
        int batterie   = Integer.parseInt(data.get("batterie").toString());
        esp32Service().traiterBatterieMaster(mac, batterie);
        log.info(" Batterie master {} : {}%", mac, batterie);
    }
 
    // ─── 3. État d'un slave ──────────────────────────────────
    // Topic   : seaside/telemetry/slave/{slaveId}
    // Payload : {"mac": "AA:BB:CC:DD:EE:FF", "batterie": 45, "connecte": true}
    private void traiterEtatSlave(String topic, String payload) throws Exception {
        String[] parts = topic.split("/");
        if (parts.length < 4) return;
 
        int slaveId    = Integer.parseInt(parts[3]);
        Map<?, ?> data = objectMapper.readValue(payload, Map.class);
        String mac     = data.get("mac").toString();
        int batterie   = Integer.parseInt(data.get("batterie").toString());
        boolean connecte = Boolean.parseBoolean(data.get("connecte").toString());
 
        esp32Service().traiterEtatSlave(mac, slaveId, batterie, connecte);
        log.info(" Slave {} | master: {} | batterie: {}% | connecté: {}",
                slaveId, mac, batterie, connecte);
    }
 
    // ─── 4. Alerte envoyée par le firmware ──────────────────
    // Topic   : seaside/alerte
    // Payload : {"mac": "...", "code": "SLAVE_DECONNECTE", "message": "..."}
    private void traiterAlerteFirmware(String payload) throws Exception {
        Map<?, ?> data = objectMapper.readValue(payload, Map.class);
        String mac     = data.get("mac").toString();
        String code    = data.get("code").toString();
        String message = data.get("message").toString();
 
        // Appel de la méthode correcte dans Esp32Service
        esp32Service().traiterAlerteFirmware(mac, code, message);
        log.warn(" Alerte firmware — {}: {}", code, message);
    }
}