package com.seaside.seaside_api.feature.esp32.web;

import com.seaside.seaside_api.feature.esp32.dto.AssocierSlaveRequest;
import com.seaside.seaside_api.feature.esp32.dto.EnregistrerMasterRequest;
import com.seaside.seaside_api.feature.esp32.dto.BatteryTelemetryDTO;
import com.seaside.seaside_api.feature.esp32.dto.DashboardsStatsDTO;
import com.seaside.seaside_api.feature.esp32.dto.Esp32MasterDTO;
import com.seaside.seaside_api.feature.esp32.dto.Esp32SlaveDTO;
import com.seaside.seaside_api.feature.utilisateur.model.Utilisateur;
import com.seaside.seaside_api.feature.esp32.service.Esp32CommandService;
import com.seaside.seaside_api.feature.esp32.service.Esp32QueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/esp32")
@RequiredArgsConstructor
public class Esp32Controller {

    private final Esp32CommandService commandService;

    private final Esp32QueryService queryService;

    // ─── Utilitaire : récupérer l'id de l'utilisateur connecté
    private UUID moi() {
        return ((Utilisateur) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal()).getId();
    }

    // ════════════════════════════════════════════════════════
    // ENREGISTREMENT & CONFIGURATION
    // ════════════════════════════════════════════════════════

    // POST /esp32/masters
    // Enregistrer un nouveau master ESP32
    @PostMapping("/masters")
    public ResponseEntity<Esp32MasterDTO> enregistrerMaster(
            @Valid @RequestBody EnregistrerMasterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commandService.enregistrerMaster(req));
    }

    // POST /esp32/masters/{masterId}/slaves
    // Enregistrer un slave sous un master
    @PostMapping("/masters/{masterId}/slaves")
    public ResponseEntity<Esp32SlaveDTO> associerSlave(
            @PathVariable UUID masterId,
            @Valid @RequestBody AssocierSlaveRequest req) {
        return ResponseEntity.ok(commandService.associerSlave(masterId, req));
    }

    // POST /esp32/masters/{masterId}/config/envoyer
    // Renvoyer manuellement la config (seuil) au firmware
    @PostMapping("/masters/{masterId}/config/envoyer")
    public ResponseEntity<Void> envoyerConfig(@PathVariable UUID masterId) {
        commandService.envoyerConfig(masterId);
        return ResponseEntity.ok().build();
    }

    // ════════════════════════════════════════════════════════
    // LECTURE
    // ════════════════════════════════════════════════════════

    // GET /esp32/evenements/{evenementId}/dashboard
    // Dashboard complet : comptages + télémétrie + slaves
    @GetMapping("/evenements/{evenementId}/dashboard")
    public ResponseEntity<DashboardsStatsDTO> getDashboard(
            @PathVariable UUID evenementId) {
        return ResponseEntity.ok(queryService.getEvenementDashboard(evenementId, moi()));
    }

    @GetMapping("/masters")
    public ResponseEntity<List<Esp32MasterDTO>> getMasters() {
        return ResponseEntity.ok(queryService.getAllMasters());
    }

    @GetMapping("/masters/{id}")
    public ResponseEntity<Esp32MasterDTO> getMaster(@PathVariable UUID id) {
        return ResponseEntity.ok(queryService.getMaster(id));
    }

    @GetMapping("/masters/{id}/slaves")
    public ResponseEntity<List<Esp32SlaveDTO>> getSlaves(@PathVariable UUID id) {
        return ResponseEntity.ok(queryService.getSlaves(id));
    }

    @GetMapping("/slaves/{id}")
    public ResponseEntity<Esp32SlaveDTO> getSlave(@PathVariable UUID id) {
        return ResponseEntity.ok(queryService.getSlave(id));
    }

    @GetMapping("/slaves/{id}/history")
    public ResponseEntity<List<BatteryTelemetryDTO>> getHistorique(@PathVariable UUID id) {
        return ResponseEntity.ok(queryService.getHistorique(id));
    }
}