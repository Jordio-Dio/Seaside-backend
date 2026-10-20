package com.seaside.seaside_api.feature.esp32.service;

import com.seaside.seaside_api.feature.esp32.dto.BatteryDashboardDTO;
import com.seaside.seaside_api.feature.esp32.dto.BatteryTelemetryDTO;
import com.seaside.seaside_api.feature.esp32.dto.CategorieStatDTO;
import com.seaside.seaside_api.feature.esp32.dto.DashboardsStatsDTO;
import com.seaside.seaside_api.feature.esp32.dto.Esp32MasterDTO;
import com.seaside.seaside_api.feature.esp32.dto.Esp32SlaveDTO;
import com.seaside.seaside_api.feature.esp32.dto.SlaveStatusDTO;
import com.seaside.seaside_api.feature.evenement.model.Categorie;
import com.seaside.seaside_api.feature.esp32.model.Esp32Master;
import com.seaside.seaside_api.feature.esp32.model.Esp32Slave;
import com.seaside.seaside_api.feature.evenement.model.Evenement;
import com.seaside.seaside_api.feature.esp32.mapper.Esp32Mapper;
import com.seaside.seaside_api.feature.esp32.repository.BatteryTelemetryRepository;
import com.seaside.seaside_api.feature.entree.repository.EntreeRepository;
import com.seaside.seaside_api.feature.evenement.repository.EvenementRepository;
import com.seaside.seaside_api.feature.esp32.repository.Esp32MasterRepository;
import com.seaside.seaside_api.feature.esp32.repository.Esp32SlaveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class Esp32QueryService {

    private static final int SEUIL_CRITIQUE = 10;
    private static final int SEUIL_FAIBLE = 20;

    private final Esp32MasterRepository masterRepository;
    private final Esp32SlaveRepository slaveRepository;
    private final BatteryTelemetryRepository telemetryRepository;
    private final EvenementRepository evenementRepository;
    private final EntreeRepository entreeRepository;
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

//Dashboard complet d'un événement (comptages + télémétrie + slaves)
public DashboardsStatsDTO getEvenementDashboard(UUID evenementId, UUID utilisateurId) {

    Evenement ev = evenementRepository
            .findByIdAndUtilisateurId(evenementId, utilisateurId)
            .orElseThrow(() ->
                    new RuntimeException("Événement introuvable ou accès refusé"));

    List<Esp32Master> masters = masterRepository
            .findByCategory_Evenement_Id(evenementId);

    List<SlaveStatusDTO> slaves = masters.stream()
            .flatMap(m -> m.getSlaves().stream())
            .map(this::toSlaveStatusDTO)
            .toList();

    Esp32Master ref = masters.stream()
            .max(Comparator.comparing(Esp32Master::getLastSeen,
                    Comparator.nullsFirst(Comparator.naturalOrder())))
            .orElse(null);

    List<CategorieStatDTO> categories = ev.getCategories().stream()
            .map(this::toCategorieStatDTO)
            .toList();

    return DashboardsStatsDTO.builder()
            .evenementId(ev.getId().toString())
            .nomEvenement(ev.getNom())
            .totalPersonnes(entreeRepository.sumComptageByEvenement(evenementId))
            .totalRevenus(totalRevenus(ev))
            .categories(categories)
            .batterieMaster(ref != null ? ref.getBatteryLevel() : null)
            .masterConnecte(ref != null ? Boolean.TRUE.equals(ref.getConnected()) : false)
            .derniereActiviteMaster(ref != null ? ref.getLastSeen() : null)
            .slaves(slaves)
            .alertes(new ArrayList<>())
            .build();
}

private CategorieStatDTO toCategorieStatDTO(Categorie cat) {
    int nombreEntrees = entreeRepository.sumComptageByCategorie(cat.getId());
    return CategorieStatDTO.builder()
            .categorieId(cat.getId())
            .nom(cat.getNom())
            .couleur(cat.getCouleur())
            .prix(cat.getPrix())
            .nombreEntrees(nombreEntrees)
            .revenu(cat.getPrix().multiply(BigDecimal.valueOf(nombreEntrees)))
            .capacite(cat.getCapacite())
            .estComplete(nombreEntrees >= cat.getCapacite())
            .build();
}

private SlaveStatusDTO toSlaveStatusDTO(Esp32Slave s) {
    int bat = s.getBatteryLevel() != null ? s.getBatteryLevel() : 0;
    String niveau = bat <= SEUIL_CRITIQUE ? "CRITIQUE" : bat <= SEUIL_FAIBLE ? "FAIBLE" : "OK";
    Categorie cat = s.getMaster() != null ? s.getMaster().getCategory() : null;
    return SlaveStatusDTO.builder()
            .slaveId(s.getSlaveNumber())
            .nom(s.getNom() != null ? s.getNom() : "Slave " + s.getSlaveNumber())
            .batterie(bat)
            .estConnecte(Boolean.TRUE.equals(s.getConnecte()))
            .categorieAssociee(cat != null ? cat.getNom() : null)
            .categorieId(cat != null ? cat.getId() : null)
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


}