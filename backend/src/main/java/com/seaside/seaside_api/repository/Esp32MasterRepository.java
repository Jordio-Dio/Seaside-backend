package com.seaside.seaside_api.repository;

import com.seaside.seaside_api.entity.Esp32Master;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Esp32MasterRepository extends JpaRepository<Esp32Master, UUID> {

    Optional<Esp32Master> findByMacAddress(String macAddress);

    Optional<Esp32Master> findFirstByLicence(String licence);

    List<Esp32Master> findByCategory_Evenement_Id(UUID evenementId);
}