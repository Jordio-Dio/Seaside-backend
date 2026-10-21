package com.seaside.seaside_api.feature.licence.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seaside.seaside_api.feature.esp32.model.Esp32Slave;
import com.seaside.seaside_api.feature.esp32.repository.Esp32SlaveRepository;
import com.seaside.seaside_api.feature.licence.dto.ChangerStatutLicenceRequest;
import com.seaside.seaside_api.feature.licence.dto.LicenceRequest;
import com.seaside.seaside_api.feature.licence.dto.LicenceResponse;
import com.seaside.seaside_api.feature.licence.model.License;
import com.seaside.seaside_api.feature.licence.model.LicenseStatus;
import com.seaside.seaside_api.feature.licence.repository.LicenseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cycle de vie des licences (console admin) : délivrance, activation,
 * blocque/révocation. Une licence est attachée à un module slave unique
 * (contrainte OneToOne de l'entité License).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LicenceService {

    private final LicenseRepository licenseRepository;
    private final Esp32SlaveRepository esp32SlaveRepository;

    public List<LicenceResponse> lister() {
        return licenseRepository.findAllByOrderByActivationDateDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public LicenceResponse creer(LicenceRequest req) {
        Esp32Slave slave = esp32SlaveRepository.findById(req.getSlaveId())
                .orElseThrow(() -> new RuntimeException("Module slave introuvable"));

        if (licenseRepository.existsBySlaveId(slave.getId())) {
            throw new RuntimeException("Ce module possède déjà une licence");
        }

        License licence = License.builder()
                .licenseKey(genererCle())
                .status(LicenseStatus.ACTIVE)
                .activationDate(LocalDate.now())
                .expirationDate(req.getDateExpiration())
                .slave(slave)
                .build();
        licence = licenseRepository.save(licence);
        log.info("Licence {} délivrée pour le slave {}", licence.getLicenseKey(), slave.getId());
        return toResponse(licence);
    }

    @Transactional
    public LicenceResponse changerStatut(UUID id, ChangerStatutLicenceRequest req) {
        License licence = licenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Licence introuvable"));

        licence.setStatus(req.getStatut());
        if (req.getStatut() == LicenseStatus.ACTIVE && licence.getActivationDate() == null) {
            licence.setActivationDate(LocalDate.now());
        }
        licence = licenseRepository.save(licence);
        log.info("Licence {} → {}", id, req.getStatut());
        return toResponse(licence);
    }

    // Clé lisible et non devinable : SEA- + 16 caractères aléatoires
    private String genererCle() {
        return "SEA-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16).toUpperCase();
    }

    private LicenceResponse toResponse(License l) {
        Esp32Slave slave = l.getSlave();
        return LicenceResponse.builder()
                .id(l.getId())
                .licenseKey(l.getLicenseKey())
                .statut(l.getStatus())
                .activationDate(l.getActivationDate())
                .expirationDate(l.getExpirationDate())
                .slaveId(slave != null ? slave.getId() : null)
                .nomSlave(slave != null ? slave.getNom() : null)
                .build();
    }
}
