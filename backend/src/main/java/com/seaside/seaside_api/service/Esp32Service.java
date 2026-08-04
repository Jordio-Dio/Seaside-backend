package com.seaside.seaside_api.service;

import com.seaside.seaside_api.dto.request.AssocierSlaveRequest;
import com.seaside.seaside_api.dto.request.EnregistrerMasterRequest;
import com.seaside.seaside_api.dto.response.*;
import com.seaside.seaside_api.entity.*;
import com.seaside.seaside_api.mqtt.MqttPublisher;
import com.seaside.seaside_api.mqtt.WebSocketPublisher;
import com.seaside.seaside_api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class Esp32Service {

    private final ModuleEsp32Repository  moduleRepository;
    private final SlaveModuleRepository  slaveRepository;
    private final EvenementRepository    evenementRepository;
    private final CategorieRepository    categorieRepository;
    private final MqttPublisher          mqttPublisher;
    private final WebSocketPublisher     webSocketPublisher;

    private static final int SEUIL_CRITIQUE = 10;
    private static final int SEUIL_FAIBLE   = 20;

    // ════════════════════════════════════════════════════════
    // ENREGISTREMENT & CONFIGURATION
    // ════════════════════════════════════════════════════════

    // ─── Enregistrer un master ESP32 ────────────────────────
    public ModuleEsp32 enregistrerMaster(EnregistrerMasterRequest req, UUID utilisateurId) {
        Evenement ev = evenementRepository
                .findByIdAndUtilisateurId(req.getEvenementId(), utilisateurId)
                .orElseThrow(() -> new RuntimeException("Événement introuvable"));

        // Si déjà enregistré → mise à jour
        Optional<ModuleEsp32> existant = moduleRepository.findByAdresseMac(req.getAdresseMac());
        if (existant.isPresent()) {
            ModuleEsp32 m = existant.get();
            m.setEvenement(ev);
            if (req.getNom() != null) m.setNom(req.getNom());
            return moduleRepository.save(m);
        }

        ModuleEsp32 module = ModuleEsp32.builder()
                .adresseMac(req.getAdresseMac())
                .nom(req.getNom() != null ? req.getNom() : "Master " + req.getAdresseMac())
                .evenement(ev)
                .estConnecte(false)
                .build();

        return moduleRepository.save(module);
    }

    // ─── Associer un slave à une catégorie (dynamique) ──────
    public SlaveStatusDTO associerSlave(UUID masterId, AssocierSlaveRequest req, UUID utilisateurId) {
        ModuleEsp32 master = trouverMaster(masterId, utilisateurId);

        SlaveModule slave = slaveRepository
                .findBySlaveIdAndMasterId(req.getSlaveId(), masterId)
                .orElse(SlaveModule.builder()
                        .slaveId(req.getSlaveId())
                        .master(master)
                        .build());

        slave.setNom(req.getNom() != null ? req.getNom() : "Slave " + req.getSlaveId());

        if (req.getCategorieId() != null) {
            Categorie cat = categorieRepository.findById(req.getCategorieId())
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
            slave.setCategorie(cat);
        }

        slaveRepository.save(slave);

        // Envoyer la nouvelle config au firmware automatiquement
        envoyerConfigFirmware(master);

        return toSlaveDTO(slave);
    }

    // ─── Envoyer config au firmware via MQTT ─────────────────
    // Appelé automatiquement après chaque association slave ↔ catégorie
    public void envoyerConfigFirmware(ModuleEsp32 master) {
        List<SlaveModule> slaves = slaveRepository.findByMasterId(master.getId());

        List<SlaveConfigDTO> slavesConfig = slaves.stream()
                .filter(s -> s.getCategorie() != null)
                .map(s -> SlaveConfigDTO.builder()
                        .slaveId(s.getSlaveId())
                        .categorieId(s.getCategorie().getId().toString())
                        .nomCategorie(s.getCategorie().getNom())
                        .build())
                .collect(Collectors.toList());

        Esp32ConfigResponseDTO config = Esp32ConfigResponseDTO.builder()
                .evenementId(master.getEvenement().getId().toString())
                .nomEvenement(master.getEvenement().getNom())
                .seuilAlerteBatterie(SEUIL_FAIBLE)
                .slaves(slavesConfig)
                .build();

        // Topic dédié au master par son adresse MAC
        String topic = "seaside/config/" + master.getAdresseMac();
        mqttPublisher.publier(topic, config);

        log.info("Config envoyée → master {} | {} slaves configurés",
                master.getAdresseMac(), slavesConfig.size());
    }

    // ─── Renvoyer config manuellement (bouton dans le frontend)
    public void envoyerConfigManuelle(UUID masterId, UUID utilisateurId) {
        ModuleEsp32 master = trouverMaster(masterId, utilisateurId);
        envoyerConfigFirmware(master);
    }

    // ════════════════════════════════════════════════════════
    // TRAITEMENT TÉLÉMÉTRIE — appelé par MqttSubscriber
    // ════════════════════════════════════════════════════════

    // ─── Batterie master ─────────────────────────────────────
    public void traiterBatterieMaster(String mac, int batterie) {
        moduleRepository.findByAdresseMac(mac).ifPresent(master -> {
            master.setBatterieMaster(batterie);
            master.setEstConnecte(true);
            master.setDerniereActivite(LocalDateTime.now());
            moduleRepository.save(master);

            String evenementId = master.getEvenement().getId().toString();

            // Alertes batterie
            if (batterie <= SEUIL_CRITIQUE) {
                webSocketPublisher.publierAlerte(evenementId, AlerteDTO.builder()
                        .code("BATTERIE_MASTER_CRITIQUE")
                        .message("Batterie master à " + batterie + "% — intervention urgente !")
                        .niveau("CRITIQUE")
                        .declencheLe(LocalDateTime.now())
                        .build());
            } else if (batterie <= SEUIL_FAIBLE) {
                webSocketPublisher.publierAlerte(evenementId, AlerteDTO.builder()
                        .code("BATTERIE_MASTER_FAIBLE")
                        .message("🔋 Batterie master à " + batterie + "% — recharger bientôt")
                        .niveau("WARN")
                        .declencheLe(LocalDateTime.now())
                        .build());
            }

            publierTelemetrie(master);
        });
    }

    // ─── État d'un slave ─────────────────────────────────────
    public void traiterEtatSlave(String mac, int slaveId, int batterie, boolean connecte) {
        moduleRepository.findByAdresseMac(mac).ifPresent(master -> {
            slaveRepository.findBySlaveIdAndMasterId(slaveId, master.getId())
                    .ifPresent(slave -> {
                        boolean etaitConnecte = Boolean.TRUE.equals(slave.getEstConnecte());
                        slave.setBatterie(batterie);
                        slave.setEstConnecte(connecte);
                        slave.setDerniereActivite(LocalDateTime.now());
                        slaveRepository.save(slave);

                        String evId = master.getEvenement().getId().toString();
                        String nom  = slave.getNom() != null ? slave.getNom() : "Slave " + slaveId;

                        // Alerte déconnexion
                        if (etaitConnecte && !connecte) {
                            webSocketPublisher.publierAlerte(evId, AlerteDTO.builder()
                                    .code("SLAVE_DECONNECTE")
                                    .message("❌ " + nom + " est déconnecté !")
                                    .niveau("CRITIQUE")
                                    .slaveId(slaveId)
                                    .declencheLe(LocalDateTime.now())
                                    .build());
                        }

                        // Alerte batterie slave
                        if (connecte && batterie <= SEUIL_FAIBLE) {
                            webSocketPublisher.publierAlerte(evId, AlerteDTO.builder()
                                    .code("BATTERIE_SLAVE_FAIBLE")
                                    .message("🔋 " + nom + " batterie à " + batterie + "%")
                                    .niveau(batterie <= SEUIL_CRITIQUE ? "CRITIQUE" : "WARN")
                                    .slaveId(slaveId)
                                    .declencheLe(LocalDateTime.now())
                                    .build());
                        }

                        publierTelemetrie(master);
                    });
        });
    }

    // ─── Alerte envoyée directement par le firmware ──────────
    // Appelé par MqttSubscriber quand topic = seaside/alerte
    public void traiterAlerteFirmware(String mac, String code, String message) {
        moduleRepository.findByAdresseMac(mac).ifPresent(master -> {
            String evId = master.getEvenement().getId().toString();

            webSocketPublisher.publierAlerte(evId, AlerteDTO.builder()
                    .code(code)
                    .message(message)
                    .niveau(code.contains("CRITIQUE") ? "CRITIQUE" : "WARN")
                    .declencheLe(LocalDateTime.now())
                    .build());

            log.warn("Alerte firmware — master: {} | code: {} | msg: {}", mac, code, message);
        });
    }

    // ─── Pousser la télémétrie complète au frontend ──────────
    public void publierTelemetrie(ModuleEsp32 master) {
        List<SlaveModule> slaves = slaveRepository.findByMasterId(master.getId());

        List<SlaveStatusDTO> slaveDTOs = slaves.stream()
                .map(this::toSlaveDTO)
                .collect(Collectors.toList());

        Map<String, Object> telemetrie = new HashMap<>();
        telemetrie.put("evenementId",           master.getEvenement().getId().toString());
        telemetrie.put("batterieMaster",         master.getBatterieMaster());
        telemetrie.put("masterConnecte",         master.getEstConnecte());
        telemetrie.put("derniereActiviteMaster", master.getDerniereActivite());
        telemetrie.put("totalPersonnes",         master.getEvenement().getTotalPersonnes());
        telemetrie.put("totalRevenus",           master.getEvenement().getTotalRevenus());
        telemetrie.put("slaves",                 slaveDTOs);

        webSocketPublisher.publierTelemetrie(
                master.getEvenement().getId().toString(), telemetrie);
    }

    // ════════════════════════════════════════════════════════
    // LECTURE — Dashboard REST
    // ════════════════════════════════════════════════════════

    // ════════════════════════════════════════════════════════════


@Transactional(readOnly = true)
public DashboardsStatsDTO getDashboard(UUID evenementId, UUID utilisateurId) {

    Evenement ev = evenementRepository
            .findByIdAndUtilisateurId(evenementId, utilisateurId)
            .orElseThrow(() -> new RuntimeException("Événement introuvable"));

    List<ModuleEsp32> masters = moduleRepository.findByEvenementId(evenementId);

    List<SlaveStatusDTO> slaves = masters.stream()
            .flatMap(m -> slaveRepository.findByMasterId(m.getId()).stream())
            .map(this::toSlaveDTO)
            .collect(Collectors.toList());

    Integer batterieMaster     = masters.isEmpty() ? null  : masters.get(0).getBatterieMaster();
    Boolean masterConnecte     = masters.isEmpty() ? false : masters.get(0).getEstConnecte();
    LocalDateTime derniereAct  = masters.isEmpty() ? null  : masters.get(0).getDerniereActivite();

    // ─── CORRECTION : extraire dans une méthode privée ──────
    // Java ne peut pas inférer le type dans un map() imbriqué
    List<CategorieStatDTO> categorieStats = ev.getCategories().stream()
            .map(cat -> toCategorieStatDTO(cat, slaves))  // ← méthode séparée
            .collect(Collectors.toList());

    return DashboardsStatsDTO.builder()
            .evenementId(ev.getId().toString())
            .nomEvenement(ev.getNom())
            .totalPersonnes(ev.getTotalPersonnes())
            .totalRevenus(ev.getTotalRevenus())
            .categories(categorieStats)
            .batterieMaster(batterieMaster)
            .masterConnecte(masterConnecte)
            .derniereActiviteMaster(derniereAct)
            .slaves(slaves)
            .alertes(new ArrayList<>())
            .build();
}

// ─── Méthode extraite — résout "Cannot infer type argument(s)" ──
// Le problème venait du map() imbriqué dans un autre map()
// Java perdait le type générique → on sort la logique ici
private CategorieStatDTO toCategorieStatDTO(Categorie cat, List<SlaveStatusDTO> slaves) {

    // Trouver le slave associé à cette catégorie
    Integer slaveId = slaves.stream()
            .filter(s -> s.getCategorieId() != null
                    && s.getCategorieId().equals(cat.getId()))
            .map(SlaveStatusDTO::getSlaveId)
            .findFirst()
            .orElse(null);

    return CategorieStatDTO.builder()
            .categorieId(cat.getId())
            .nom(cat.getNom())
            .prix(cat.getPrix())
            .nombreEntrees(cat.getNombreEntrees())
            .revenu(cat.getRevenuCategorie())
            .capacite(cat.getCapacite())
            .estComplete(cat.estComplete())
            .slaveId(slaveId)
            .build();
}

    // ════════════════════════════════════════════════════════
    // UTILITAIRES PRIVÉS
    // ════════════════════════════════════════════════════════

    private SlaveStatusDTO toSlaveDTO(SlaveModule s) {
        int bat = s.getBatterie() != null ? s.getBatterie() : 0;
        String niveau = bat <= SEUIL_CRITIQUE ? "CRITIQUE"
                : bat <= SEUIL_FAIBLE ? "FAIBLE" : "OK";

        return SlaveStatusDTO.builder()
                .slaveId(s.getSlaveId())
                .nom(s.getNom())
                .batterie(bat)
                .estConnecte(Boolean.TRUE.equals(s.getEstConnecte()))
                .categorieAssociee(s.getCategorie() != null ? s.getCategorie().getNom() : null)
                .categorieId(s.getCategorie() != null ? s.getCategorie().getId() : null)
                .derniereActivite(s.getDerniereActivite())
                .niveauBatterie(niveau)
                .build();
    }

    private ModuleEsp32 trouverMaster(UUID masterId, UUID utilisateurId) {
        return moduleRepository.findById(masterId)
                .filter(m -> m.getEvenement().getUtilisateur().getId().equals(utilisateurId))
                .orElseThrow(() -> new RuntimeException("Module introuvable ou accès refusé"));
    }
}