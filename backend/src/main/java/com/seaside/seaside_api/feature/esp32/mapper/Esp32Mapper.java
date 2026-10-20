package com.seaside.seaside_api.feature.esp32.mapper;

import com.seaside.seaside_api.feature.esp32.dto.BatteryDashboardDTO;
import com.seaside.seaside_api.feature.esp32.dto.BatteryTelemetryDTO;
import com.seaside.seaside_api.feature.esp32.dto.Esp32MasterDTO;
import com.seaside.seaside_api.feature.esp32.dto.Esp32SlaveDTO;
import com.seaside.seaside_api.feature.esp32.model.BatteryTelemetry;
import com.seaside.seaside_api.feature.esp32.model.Esp32Master;
import com.seaside.seaside_api.feature.esp32.model.Esp32Slave;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class Esp32Mapper {

        // ================= MASTER =================

        public Esp32MasterDTO toMasterDTO(Esp32Master master) {

                if (master == null) {
                        return null;
                }

                return Esp32MasterDTO.builder()
                                .id(master.getId())
                                .macAddress(master.getMacAddress())
                                .adresseMac(master.getMacAddress())
                                .nom(master.getNom())
                                .batteryLevel(master.getBatteryLevel())
                                .lastSeen(master.getLastSeen())
                                .licence(master.getLicence())
                                .moduleId(master.getModuleId())
                                .seuil(master.getSeuil())
                                .connected(master.getConnected())

                                .categorieId(
                                                master.getCategory() != null ? master.getCategory().getId() : null)

                                .categorieNom(
                                                master.getCategory() != null ? master.getCategory().getNom() : null)

                                .nombreSlaves(master.getSlaves().size())

                                .nombreSlavesConnectes(
                                                (int) master.getSlaves().stream()
                                                                .filter(s -> Boolean.TRUE.equals(s.getConnecte()))
                                                                .count())

                                .slaves(
                                                master.getSlaves().stream()
                                                                .map(this::toSlaveDTO)
                                                                .toList())

                                .build();
        }

        // ================= SLAVE =================

        public Esp32SlaveDTO toSlaveDTO(Esp32Slave slave) {

                if (slave == null) {
                        return null;
                }

                return Esp32SlaveDTO.builder()
                                .id(slave.getId())
                                .slaveNumber(slave.getSlaveNumber())
                                .slaveId(slave.getSlaveId())
                                .nom(slave.getNom())
                                .macAddress(slave.getMacAddress())
                                .batteryLevel(slave.getBatteryLevel())
                                .connecte(slave.getConnecte())
                                .active(slave.getActive())
                                .seuil(slave.getSeuil())
                                .lastSeen(slave.getLastSeen())
                                .masterId(slave.getMaster().getId())
                                .build();
        }

        // ================= TELEMETRIE =================

        public BatteryTelemetryDTO toTelemetryDTO(BatteryTelemetry telemetry) {

                if (telemetry == null) {
                        return null;
                }

                return BatteryTelemetryDTO.builder()
                                .id(telemetry.getId())
                                .batteryLevel(telemetry.getBatteryLevel())
                                .signalStrength(telemetry.getSignalStrength())
                                .temperature(telemetry.getTemperature())
                                .recordedAt(telemetry.getRecordedAt())
                                .slaveId(telemetry.getSlave().getId())
                                .build();
        }

        // ================= DASHBOARD =================

        public BatteryDashboardDTO toDashboard(Esp32Master master) {

                List<Esp32SlaveDTO> slaves = master.getSlaves()
                                .stream()
                                .map(this::toSlaveDTO)
                                .toList();

                List<Integer> batteries = slaves.stream()
                                .map(Esp32SlaveDTO::getBatteryLevel)
                                .filter(Objects::nonNull)
                                .toList();

                int moyenne = batteries.isEmpty()
                                ? 0
                                : batteries.stream()
                                                .mapToInt(Integer::intValue)
                                                .sum() / batteries.size();

                int connectes = (int) slaves.stream()
                                .filter(s -> Boolean.TRUE.equals(s.getConnecte()))
                                .count();

                return BatteryDashboardDTO.builder()
                                .master(toMasterDTO(master))
                                .slaves(slaves)
                                .moyenneBatterie(moyenne)
                                .nombreModules(slaves.size())
                                .nombreConnectes(connectes)
                                .nombreDeconnectes(slaves.size() - connectes)
                                .build();
        }

}