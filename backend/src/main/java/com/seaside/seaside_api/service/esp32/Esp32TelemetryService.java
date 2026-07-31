package com.seaside.seaside_api.service.esp32;

import com.seaside.seaside_api.dto.response.AlerteDTO;
import com.seaside.seaside_api.entity.BatteryTelemetry;
import com.seaside.seaside_api.entity.Esp32Master;
import com.seaside.seaside_api.entity.Esp32Slave;
import com.seaside.seaside_api.mqtt.WebSocketPublisher;
import com.seaside.seaside_api.repository.BatteryTelemetryRepository;
import com.seaside.seaside_api.repository.Esp32MasterRepository;
import com.seaside.seaside_api.repository.Esp32SlaveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class Esp32TelemetryService {

private final Esp32MasterRepository masterRepository;
private final Esp32SlaveRepository slaveRepository;
private final BatteryTelemetryRepository telemetryRepository;
private final WebSocketPublisher webSocketPublisher;

private static final int SEUIL_CRITIQUE = 10;
private static final int SEUIL_FAIBLE = 20;

// Batterie du master
public void traiterBatterieMaster(String mac, int batterie) {

    masterRepository.findByMacAddress(mac).ifPresent(master -> {

        master.setBatteryLevel(batterie);
        master.setLastSeen(LocalDateTime.now());
        masterRepository.save(master);

        verifierBatterieMaster(master, batterie);
        publierTelemetrie(master);
    });
}

// État d’un slave
public void traiterEtatSlave(String mac,
                             int slaveId,
                             int batterie,
                             boolean connecte) {

    slaveRepository.findByMacAddress(mac).ifPresent(slave -> {

        boolean ancienEtat = Boolean.TRUE.equals(slave.getConnecte());

        slave.setBatteryLevel(batterie);
        slave.setConnecte(connecte);
        slave.setLastSeen(LocalDateTime.now());

        slaveRepository.save(slave);

        enregistrerTelemetrie(slave, batterie);

        if (ancienEtat && !connecte) {
            publierAlerte(slave,
                    "SLAVE_DECONNECTE",
                    "Module " + slaveId + " déconnecté",
                    "CRITIQUE");
        }

        if (connecte && batterie <= SEUIL_FAIBLE) {
            publierAlerte(slave,
                    "BATTERIE_SLAVE_FAIBLE",
                    "Batterie du module " + slaveId + " à " + batterie + "%",
                    batterie <= SEUIL_CRITIQUE ? "CRITIQUE" : "WARN");
        }

        publierTelemetrie(slave.getMaster());
    });
}

// Enregistrer l’historique batterie
private void enregistrerTelemetrie(Esp32Slave slave, int batterie) {

    BatteryTelemetry telemetry = BatteryTelemetry.builder()
            .slave(slave)
            .batteryLevel(batterie)
            .recordedAt(LocalDateTime.now())
            .build();

    telemetryRepository.save(telemetry);
}

// Vérification batterie master
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

// Publication WebSocket
private void publierTelemetrie(Esp32Master master) {

    Map<String, Object> data = new HashMap<>();

    data.put("masterId", master.getId());
    data.put("batteryLevel", master.getBatteryLevel());
    data.put("lastSeen", master.getLastSeen());

    webSocketPublisher.publierTelemetrie(
            master.getId().toString(),
            data);
}

// Publication alerte
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


}
