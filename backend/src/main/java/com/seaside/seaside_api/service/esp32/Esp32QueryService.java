package com.seaside.seaside_api.service.esp32;

import com.seaside.seaside_api.dto.response.*;
import com.seaside.seaside_api.entity.Esp32Master;
import com.seaside.seaside_api.entity.Esp32Slave;
import com.seaside.seaside_api.mapper.Esp32Mapper;
import com.seaside.seaside_api.repository.BatteryTelemetryRepository;
import com.seaside.seaside_api.repository.Esp32MasterRepository;
import com.seaside.seaside_api.repository.Esp32SlaveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class Esp32QueryService {

    private final Esp32MasterRepository masterRepository;
    private final Esp32SlaveRepository slaveRepository;
    private final BatteryTelemetryRepository telemetryRepository;
    private final Esp32Mapper mapper;

    //récupérer tous les masters
    public List<Esp32MasterDTO> getAllMasters() {

    return masterRepository.findAll()
            .stream()
            .map(mapper::toMasterDTO)
            .toList();
}

//Récupérer un master
public Esp32MasterDTO getMaster(UUID id) {

    Esp32Master master = masterRepository.findById(id)
            .orElseThrow(() ->
                    new RuntimeException("Master introuvable"));

    return mapper.toMasterDTO(master);
}

//Dashboard d'un master
public BatteryDashboardDTO getDashboard(UUID masterId) {

    Esp32Master master = masterRepository.findById(masterId)
            .orElseThrow(() ->
                    new RuntimeException("Master introuvable"));

    return mapper.toDashboard(master);
}

//Liste des slaves d'un master
public List<Esp32SlaveDTO> getSlaves(UUID masterId) {

    Esp32Master master = masterRepository.findById(masterId)
            .orElseThrow(() ->
                    new RuntimeException("Master introuvable"));

    return master.getSlaves()
            .stream()
            .map(mapper::toSlaveDTO)
            .toList();
}

//Une slave
public Esp32SlaveDTO getSlave(UUID id) {

    Esp32Slave slave = slaveRepository.findById(id)
            .orElseThrow(() ->
                    new RuntimeException("Slave introuvable"));

    return mapper.toSlaveDTO(slave);
}

// Historique de la batterie d'un slave
public List<BatteryTelemetryDTO> getHistorique(UUID slaveId) {

    Esp32Slave slave = slaveRepository.findById(slaveId)
            .orElseThrow(() ->
                    new RuntimeException("Slave introuvable"));

    return telemetryRepository
            .findBySlaveOrderByRecordedAtDesc(slave)
            .stream()
            .map(mapper::toTelemetryDTO)
            .toList();
}


}