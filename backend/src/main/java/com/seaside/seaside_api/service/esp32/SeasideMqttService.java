package com.seaside.seaside_api.service.esp32;

import com.seaside.seaside_api.dto.response.AlerteDTO;
import com.seaside.seaside_api.dto.response.SlaveStatusDTO;
import com.seaside.seaside_api.entity.BatteryTelemetry;
import com.seaside.seaside_api.entity.Categorie;
import com.seaside.seaside_api.entity.Entree;
import com.seaside.seaside_api.entity.Esp32Master;
import com.seaside.seaside_api.entity.Esp32Slave;
import com.seaside.seaside_api.entity.Evenement;
import com.seaside.seaside_api.mqtt.WebSocketPublisher;
import com.seaside.seaside_api.repository.BatteryTelemetryRepository;
import com.seaside.seaside_api.repository.CategorieRepository;
import com.seaside.seaside_api.repository.EntreeRepository;
import com.seaside.seaside_api.repository.Esp32MasterRepository;
import com.seaside.seaside_api.repository.Esp32SlaveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SeasideMqttService {

    private static final int SEUIL_CRITIQUE = 10;
    private static final int SEUIL_FAIBLE = 20;

    private final Esp32MasterRepository masterRepository;
    private final Esp32SlaveRepository slaveRepository;
    private final CategorieRepository categorieRepository;
    private final EntreeRepository entreeRepository;
    private final BatteryTelemetryRepository telemetryRepository;
    private final WebSocketPublisher webSocketPublisher;

    // ─── 1. Télémétrie master ────────────────────────────────
    // seaside/telemetry/master — {mac, moduleId, battery, licence, connected, ...}
    public void traiterTelemetrieMaster(Map<String, Object> data) {
        String mac = str(data.get("mac"));
        if (mac == null) {
            log.warn("Payload master sans 'mac'");
            return;
        }

        Esp32Master master = masterRepository.findByMacAddress(mac).orElse(null);
        if (master == null) {
            String licence = str(data.get("licence"));
            if (licence != null) master = masterRepository.findFirstByLicence(licence).orElse(null);
        }
        if (master == null) {
            log.warn("Master non enregistré — mac: {} | licence: {}",
                    mac, str(data.get("licence")));
            return;
        }

        master.setMacAddress(mac);
        Integer batterie = integer(data.get("battery"));
        if (batterie != null) master.setBatteryLevel(batterie);
        Boolean connected = bool(data.get("connected"));
        if (connected != null) master.setConnected(connected);
        String licence = str(data.get("licence"));
        if (licence != null) master.setLicence(licence);
        String moduleId = str(data.get("moduleId"));
        if (moduleId != null) master.setModuleId(moduleId);
        master.setLastSeen(LocalDateTime.now());
        masterRepository.save(master);

        if (batterie != null) verifierBatterieMaster(master, batterie);
        publierTelemetrie(master);
    }

    // ─── 2. Télémétrie slave ─────────────────────────────────
    // seaside/telemetry/slave/{slaveId} — {slaveId, mac, count, seuil, active}
    public void traiterTelemetrieSlave(Map<String, Object> data) {
        String mac = str(data.get("mac"));
        if (mac == null) {
            log.warn("Payload slave sans 'mac'");
            return;
        }

        Esp32Slave slave = slaveRepository.findByMacAddress(mac).orElse(null);
        if (slave == null) {
            log.warn("Slave non enregistré — mac: {}", mac);
            return;
        }

        boolean etaitConnecte = Boolean.TRUE.equals(slave.getConnecte());
        Boolean active = bool(data.get("active"));
        if (active != null) {
            slave.setActive(active);
            slave.setConnecte(active);
        }
        Integer seuil = integer(data.get("seuil"));
        if (seuil != null) slave.setSeuil(seuil);
        slave.setLastSeen(LocalDateTime.now());
        slaveRepository.save(slave);

        Integer batterie = integer(data.get("battery"));
        if (batterie != null) {
            slave.setBatteryLevel(batterie);
            telemetryRepository.save(BatteryTelemetry.builder()
                    .slave(slave)
                    .batteryLevel(batterie)
                    .build());
        }

        if (etaitConnecte && Boolean.FALSE.equals(active)) {
            publierAlerte(slave, "SLAVE_DECONNECTE",
                    "Module esclave déconnecté",
                    "CRITIQUE");
        }
        if (Boolean.TRUE.equals(active) && batterie != null && batterie <= SEUIL_FAIBLE) {
            publierAlerte(slave, "BATTERIE_SLAVE_FAIBLE",
                    "Batterie du module esclave à " + batterie + "%",
                    batterie <= SEUIL_CRITIQUE ? "CRITIQUE" : "WARN");
        }

        if (slave.getMaster() != null) publierTelemetrie(slave.getMaster());
    }

    // ─── 3. Entrée détectée ──────────────────────────────────
    // seaside/entrees/{licence} — chaque notification = +1 personne
    public void traiterEntree(String topic, Map<String, Object> data) {
        String[] parts = topic.split("/");
        if (parts.length < 3) {
            log.warn("Topic entrée invalide : {}", topic);
            return;
        }
        String licence = parts[2];

        Esp32Master master = masterRepository.findFirstByLicence(licence).orElse(null);
        if (master == null) {
            Optional<Categorie> parCode = categorieRepository.findFirstByCouleur(licence);
            if (parCode.isEmpty()) {
                log.warn("Aucun master/catégorie pour la licence '{}'", licence);
                return;
            }
            traiterEntreeCategorie(parCode.get(), 1);
            return;
        }
        if (master.getCategory() == null) {
            log.warn("Master {} sans catégorie associée", licence);
            return;
        }
        traiterEntreeCategorie(master.getCategory(), 1);
    }

    private void traiterEntreeCategorie(Categorie cat, int comptage) {
        Evenement ev = cat.getEvenement();
        String evId = ev.getId().toString();

        if (cat.estComplete()) {
            webSocketPublisher.publierAlerte(evId, AlerteDTO.builder()
                    .code("CAPACITE_MAX")
                    .message("Capacité maximale atteinte pour : " + cat.getNom())
                    .niveau("WARN")
                    .declencheLe(LocalDateTime.now())
                    .build());
            return;
        }

        Entree entree = Entree.builder()
                .categorie(cat)
                .comptage(comptage)
                .source("esp32")
                .build();
        entreeRepository.save(entree);

        int totalCategorie = entreeRepository.sumComptageByCategorie(cat.getId());
        int totalEvenement = entreeRepository.sumComptageByEvenement(ev.getId());
        BigDecimal revenuCategorie = cat.getPrix().multiply(BigDecimal.valueOf(totalCategorie));
        BigDecimal totalRevenus = totalRevenus(ev);

        webSocketPublisher.publierComptage(evId, Map.of(
                "categorieId", cat.getId().toString(),
                "nomCategorie", cat.getNom(),
                "comptage", comptage,
                "totalCategorie", totalCategorie,
                "revenuCategorie", revenuCategorie,
                "totalEvenement", totalEvenement,
                "totalRevenus", totalRevenus));
    }

    // ─── 4. Alerte envoyée par le firmware ───────────────────
    // seaside/alerte — {mac, moduleId, alerte}
    public void traiterAlerte(Map<String, Object> data) {
        String mac = str(data.get("mac"));
        String message = str(data.get("alerte"));
        Esp32Master master = mac != null ? masterRepository.findByMacAddress(mac).orElse(null) : null;
        if (master == null || master.getCategory() == null) {
            log.warn("Alerte ignorée — master introuvable ou sans catégorie (mac: {})", mac);
            return;
        }

        String evId = master.getCategory().getEvenement().getId().toString();
        webSocketPublisher.publierAlerte(evId, AlerteDTO.builder()
                .code("FIRMWARE")
                .message(message)
                .niveau("WARN")
                .declencheLe(LocalDateTime.now())
                .build());
    }

    // ─── WebSocket — télémétrie complète par événement ───────
    private void publierTelemetrie(Esp32Master master) {
        Categorie cat = master.getCategory();
        if (cat == null) {
            log.debug("Pas de catégorie → pas de télémétrie WS pour {}", master.getMacAddress());
            return;
        }
        Evenement ev = cat.getEvenement();
        String evId = ev.getId().toString();

        List<SlaveStatusDTO> slaves = master.getSlaves().stream()
                .map(s -> toSlaveStatusDTO(s, cat))
                .toList();

        Map<String, Object> telemetrie = new HashMap<>();
        telemetrie.put("evenementId", evId);
        telemetrie.put("batterieMaster", master.getBatteryLevel());
        telemetrie.put("masterConnecte", master.getConnected());
        telemetrie.put("derniereActiviteMaster", master.getLastSeen());
        telemetrie.put("totalPersonnes", entreeRepository.sumComptageByEvenement(ev.getId()));
        telemetrie.put("totalRevenus", totalRevenus(ev));
        telemetrie.put("slaves", slaves);

        webSocketPublisher.publierTelemetrie(evId, telemetrie);
    }

    private void publierAlerte(Esp32Slave slave, String code, String message, String niveau) {
        Esp32Master master = slave.getMaster();
        if (master == null || master.getCategory() == null) return;
        String evId = master.getCategory().getEvenement().getId().toString();

        webSocketPublisher.publierAlerte(evId, AlerteDTO.builder()
                .code(code)
                .message(message)
                .niveau(niveau)
                .slaveId(slave.getSlaveNumber())
                .declencheLe(LocalDateTime.now())
                .build());
    }

    private void verifierBatterieMaster(Esp32Master master, int batterie) {
        if (master.getCategory() == null) return;
        String evId = master.getCategory().getEvenement().getId().toString();

        if (batterie <= SEUIL_CRITIQUE) {
            webSocketPublisher.publierAlerte(evId, AlerteDTO.builder()
                    .code("BATTERIE_MASTER_CRITIQUE")
                    .message("Batterie master à " + batterie + "% — intervention urgente !")
                    .niveau("CRITIQUE")
                    .declencheLe(LocalDateTime.now())
                    .build());
        } else if (batterie <= SEUIL_FAIBLE) {
            webSocketPublisher.publierAlerte(evId, AlerteDTO.builder()
                    .code("BATTERIE_MASTER_FAIBLE")
                    .message("Batterie master à " + batterie + "% — recharger bientôt")
                    .niveau("WARN")
                    .declencheLe(LocalDateTime.now())
                    .build());
        }
    }

    private SlaveStatusDTO toSlaveStatusDTO(Esp32Slave s, Categorie cat) {
        int bat = s.getBatteryLevel() != null ? s.getBatteryLevel() : 0;
        String niveau = bat <= SEUIL_CRITIQUE ? "CRITIQUE" : bat <= SEUIL_FAIBLE ? "FAIBLE" : "OK";
        return SlaveStatusDTO.builder()
                .slaveId(s.getSlaveNumber())
                .nom(s.getNom() != null ? s.getNom() : "Slave " + s.getSlaveNumber())
                .batterie(bat)
                .estConnecte(Boolean.TRUE.equals(s.getConnecte()))
                .categorieAssociee(cat.getNom())
                .categorieId(cat.getId())
                .derniereActivite(s.getLastSeen())
                .niveauBatterie(niveau)
                .build();
    }

    private BigDecimal totalRevenus(Evenement ev) {
        return ev.getCategories().stream()
                .map(c -> c.getPrix().multiply(
                        BigDecimal.valueOf(entreeRepository.sumComptageByCategorie(c.getId()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    // Tolérant : null / vide / décimal ("98.5") / texte → pas d'exception.
    // Si la valeur n'est pas convertible, on renvoie null → le champ est ignoré.
    private Integer integer(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        if (s.isEmpty()) return null;
        try {
            if (s.contains(".")) return (int) Math.round(Double.parseDouble(s));
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Tolérant : "true"/"false" (insensible à la casse). Tout le reste → null
    // (champ ignoré, jamais de fausse valeur par défaut).
    private Boolean bool(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        if ("true".equalsIgnoreCase(s)) return true;
        if ("false".equalsIgnoreCase(s)) return false;
        return null;
    }
}
