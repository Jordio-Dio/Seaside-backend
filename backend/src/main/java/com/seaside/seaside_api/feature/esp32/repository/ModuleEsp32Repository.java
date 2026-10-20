package com.seaside.seaside_api.feature.esp32.repository;


import com.seaside.seaside_api.feature.esp32.model.ModuleEsp32;
import com.seaside.seaside_api.feature.esp32.model.SlaveModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
@Repository
public interface ModuleEsp32Repository extends JpaRepository<ModuleEsp32, UUID> {
 
    Optional<ModuleEsp32> findByAdresseMac(String adresseMac);
 
    List<ModuleEsp32> findByEvenementId(UUID evenementId);
 
    @Query("SELECT m FROM ModuleEsp32 m WHERE m.evenement.id = :evId AND m.evenement.utilisateur.id = :userId")
    List<ModuleEsp32> findByEvenementIdAndUserId(
            @Param("evId") UUID evId,
            @Param("userId") UUID userId);
}