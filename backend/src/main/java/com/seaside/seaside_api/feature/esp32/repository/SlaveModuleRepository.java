package com.seaside.seaside_api.feature.esp32.repository;


import com.seaside.seaside_api.feature.esp32.model.SlaveModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
@Repository
public interface SlaveModuleRepository extends JpaRepository<SlaveModule, UUID> {
 
    List<SlaveModule> findByMasterId(UUID masterId);
 
    // Trouver un slave par son numéro et le master auquel il appartient
    Optional<SlaveModule> findBySlaveIdAndMasterId(Integer slaveId, UUID masterId);
}
 