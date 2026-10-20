package com.seaside.seaside_api.feature.esp32.service;

import com.seaside.seaside_api.feature.esp32.dto.AlerteDTO;
import com.seaside.seaside_api.feature.esp32.dto.BatteryTelemetryDTO;
import com.seaside.seaside_api.feature.esp32.model.BatteryTelemetry;
import com.seaside.seaside_api.feature.esp32.model.Esp32Master;
import com.seaside.seaside_api.feature.esp32.model.Esp32Slave;
import com.seaside.seaside_api.feature.esp32.mqtt.WebSocketPublisher;
import com.seaside.seaside_api.feature.esp32.repository.BatteryTelemetryRepository;
import com.seaside.seaside_api.feature.esp32.repository.Esp32MasterRepository;
import com.seaside.seaside_api.feature.esp32.repository.Esp32SlaveRepository;
import com.seaside.seaside_api.feature.esp32.mapper.Esp32Mapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class Esp32TelemetryService {

private final Esp32MasterRepository masterRepository;
private final Esp32SlaveRepository slaveRepository;
private final BatteryTelemetryRepository telemetryRepository;
private final WebSocketPublisher webSocketPublisher;

private final Esp32Mapper mapper;

private static final int SEUIL_CRITIQUE = 10;
private static final int SEUIL_FAIBLE = 20;

// ===================== MASTER =====================

public void traiterBatterieMaster(String masterMac, int batterie) {

    Esp32Master master = masterRepository.findByMacAddress(masterMac)
            .orElseThrow(() -> new RuntimeException(
                    "Master introuvable : " + masterMac));

    master.setBatteryLevel(batterie);
    master.setLastSeen(LocalDateTime.now());

    masterRepository.save(master);

    verifierBatterieMaster(master, batterie);
    publierTelemetrie(master);

    log.info("Master {} mis à jour | batterie={} %", masterMac, batterie);
}

// ===================== SLAVE =====================

public void traiterEtatSlave(String masterMac,
                             int slaveNumber,
                             int batterie,
                             boolean connecte) {

    Esp32Master master = masterRepository.findByMacAddress(masterMac)
            .orElseThrow(() -> new RuntimeException(
                    "Master introuvable : " + masterMac));

    Esp32Slave slave = slaveRepository
            .findByMasterAndSlaveNumber(master, slaveNumber)
            .orElseThrow(() -> new RuntimeException(
                    "Slave " + slaveNumber +
                    " introuvable pour le master " + masterMac));

    boolean ancienEtat = Boolean.TRUE.equals(slave.getConnecte());

    slave.setBatteryLevel(batterie);
    slave.setConnecte(connecte);
    slave.setLastSeen(LocalDateTime.now());

    slaveRepository.save(slave);

    enregistrerTelemetrie(slave, batterie);

    if (ancienEtat && !connecte) {
        publierAlerte(slave,
                "SLAVE_DECONNECTE",
                "Module " + slaveNumber + " déconnecté",
                "CRITIQUE");
    }

    if (connecte && batterie <= SEUIL_FAIBLE) {
        publierAlerte(slave,
                "BATTERIE_SLAVE_FAIBLE",
                "Batterie du module " + slaveNumber +
                " à " + batterie + "%",
                batterie <= SEUIL_CRITIQUE ? "CRITIQUE" : "WARN");
    }

    publierTelemetrie(master);

    log.info("Slave {} du master {} mis à jour", slaveNumber, masterMac);
}

// ===================== TÉLÉMÉTRIE =====================

private void enregistrerTelemetrie(Esp32Slave slave, int batterie) {

    BatteryTelemetry telemetry = BatteryTelemetry.builder()
            .slave(slave)
            .batteryLevel(batterie)
            .recordedAt(LocalDateTime.now())
            .build();

    telemetryRepository.save(telemetry);
}

// ===================== ALERTES =====================

private void verifierBatterieMaster(Esp32Master master, int batterie) {

    if (batterie <= SEUIL_CRITIQUE) {

        webSocketPublisher.publierAlerte(
                master.getId().toString(),
                AlerteDTO.builder()
                        .code("BATTERIE_MASTER_CRITIQUE")
                        .message("Batterie master à " + batterie + "%")
                        .niveau("CRITIQUE")
                        .declencheLe(LocalDateTime.now())
                        .build());

    } else if (batterie <= SEUIL_FAIBLE) {

        webSocketPublisher.publierAlerte(
                master.getId().toString(),
                AlerteDTO.builder()
                        .code("BATTERIE_MASTER_FAIBLE")
                        .message("Batterie master à " + batterie + "%")
                        .niveau("WARN")
                        .declencheLe(LocalDateTime.now())
                        .build());
    }
}

// ===================== WEBSOCKET =====================

private void publierTelemetrie(Esp32Master master) {

    Map<String, Object> data = new HashMap<>();

    data.put("masterId", master.getId());
    data.put("batteryLevel", master.getBatteryLevel());
    data.put("lastSeen", master.getLastSeen());

    webSocketPublisher.publierTelemetrie(
            master.getId().toString(),
            data);
}

private void publierAlerte(Esp32Slave slave,
                           String code,
                           String message,
                           String niveau) {

    webSocketPublisher.publierAlerte(
            slave.getMaster().getId().toString(),
            AlerteDTO.builder()
                    .code(code)
                    .message(message)
                    .niveau(niveau)
                    .declencheLe(LocalDateTime.now())
                    .build());
}

public List<BatteryTelemetryDTO> getHistorique(UUID slaveId) {

    Esp32Slave slave = slaveRepository.findById(slaveId)
            .orElseThrow(() -> new RuntimeException("Slave introuvable"));

    return telemetryRepository
            .findBySlaveOrderByRecordedAtDesc(slave)
            .stream()
            .map(mapper::toTelemetryDTO)
            .toList();
}

}
