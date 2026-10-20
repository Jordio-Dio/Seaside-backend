package com.seaside.seaside_api.feature.esp32.service;

import com.seaside.seaside_api.feature.esp32.dto.AssocierSlaveRequest;
import com.seaside.seaside_api.feature.esp32.dto.EnregistrerMasterRequest;
import com.seaside.seaside_api.feature.esp32.dto.Esp32MasterDTO;
import com.seaside.seaside_api.feature.esp32.dto.Esp32SlaveDTO;
import com.seaside.seaside_api.feature.evenement.model.Categorie;
import com.seaside.seaside_api.feature.esp32.model.Esp32Master;
import com.seaside.seaside_api.feature.esp32.model.Esp32Slave;
import com.seaside.seaside_api.feature.evenement.model.Evenement;
import com.seaside.seaside_api.feature.utilisateur.model.Utilisateur;
import com.seaside.seaside_api.feature.esp32.mapper.Esp32Mapper;
import com.seaside.seaside_api.feature.esp32.mqtt.MqttPublisher;
import com.seaside.seaside_api.feature.evenement.repository.CategorieRepository;
import com.seaside.seaside_api.feature.esp32.repository.Esp32MasterRepository;
import com.seaside.seaside_api.feature.esp32.repository.Esp32SlaveRepository;
import com.seaside.seaside_api.feature.evenement.repository.EvenementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class Esp32CommandService {

    private static final int SEUIL_DEFAUT = 120;

    private final Esp32MasterRepository masterRepository;
    private final Esp32SlaveRepository slaveRepository;
    private final CategorieRepository categorieRepository;
    private final EvenementRepository evenementRepository;
    private final Esp32Mapper mapper;
    private final MqttPublisher mqttPublisher;

    private UUID moi() {
        return ((Utilisateur) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal()).getId();
    }

    // ─── Enregistrer / mettre à jour un master ──────────────
    public Esp32MasterDTO enregistrerMaster(EnregistrerMasterRequest req) {
        UUID userId = moi();

        Categorie categorie = null;
        if (req.getCategorieId() != null) {
            categorie = categorieRepository.findById(req.getCategorieId())
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
            verifierProprieteEvenement(categorie.getEvenement(), userId);
        } else if (req.getEvenementId() != null) {
            Evenement ev = evenementRepository
                    .findByIdAndUtilisateurId(req.getEvenementId(), userId)
                    .orElseThrow(() -> new RuntimeException("Événement introuvable"));
            categorie = ev.getCategories().stream().findFirst().orElse(null);
        }

        Esp32Master master = masterRepository.findByMacAddress(req.getAdresseMac())
                .orElseGet(() -> Esp32Master.builder()
                        .macAddress(req.getAdresseMac())
                        .build());

        master.setCategory(categorie);
        if (req.getNom() != null) master.setNom(req.getNom());
        if (req.getLicence() != null) master.setLicence(req.getLicence());
        if (req.getModuleId() != null) master.setModuleId(req.getModuleId());
        if (req.getSeuil() != null) master.setSeuil(req.getSeuil());

        masterRepository.save(master);

        log.info("Master enregistré/mis à jour → {} | licence: {}",
                master.getMacAddress(), master.getLicence());
        return mapper.toMasterDTO(master);
    }

    // ─── Enregistrer / mettre à jour un slave sous un master ─
    public Esp32SlaveDTO associerSlave(UUID masterId, AssocierSlaveRequest req) {
        Esp32Master master = masterRepository.findById(masterId)
                .orElseThrow(() -> new RuntimeException("Master introuvable"));

        if (master.getCategory() != null) {
            verifierProprieteEvenement(master.getCategory().getEvenement(), moi());
        }

        Esp32Slave slave = (req.getMacAddress() != null)
                ? slaveRepository.findByMacAddress(req.getMacAddress()).orElse(null)
                : null;
        if (slave == null) {
            slave = Esp32Slave.builder().build();
        }

        slave.setMaster(master);
        slave.setSlaveNumber(req.getSlaveId());
        slave.setSlaveId("SLAVE_" + req.getSlaveId());
        if (req.getMacAddress() != null) slave.setMacAddress(req.getMacAddress());
        if (req.getNom() != null) slave.setNom(req.getNom());

        // Sous le modèle B, la catégorie (entrée) appartient au master
        if (req.getCategorieId() != null) {
            Categorie cat = categorieRepository.findById(req.getCategorieId())
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
            verifierProprieteEvenement(cat.getEvenement(), moi());
            master.setCategory(cat);
            masterRepository.save(master);
        }

        slaveRepository.save(slave);

        log.info("Slave sous master {} | n°{} | mac: {}",
                master.getMacAddress(), req.getSlaveId(), req.getMacAddress());
        return mapper.toSlaveDTO(slave);
    }

    // ─── Envoyer la config (seuil) au firmware ──────────────
    public void envoyerConfig(UUID masterId) {
        Esp32Master master = masterRepository.findById(masterId)
                .orElseThrow(() -> new RuntimeException("Master introuvable"));

        if (master.getCategory() != null) {
            verifierProprieteEvenement(master.getCategory().getEvenement(), moi());
        }

        int seuil = master.getSeuil() != null ? master.getSeuil() : SEUIL_DEFAUT;
        String topic = "seaside/config/" + master.getMacAddress();
        mqttPublisher.publier(topic, Map.of("seuil", seuil));

        log.info("Config envoyée → {} | seuil: {} cm", topic, seuil);
    }

    private void verifierProprieteEvenement(Evenement evenement, UUID userId) {
        if (evenement != null && !evenement.getUtilisateur().getId().equals(userId)) {
            throw new RuntimeException("Accès refusé à cet événement");
        }
    }
}
