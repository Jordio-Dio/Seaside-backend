package com.seaside.seaside_api.repository;

import com.seaside.seaside_api.entity.Esp32Master;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface Esp32MasterRepository extends JpaRepository<Esp32Master, UUID> {

    Optional<Esp32Master> findByMacAddress(String macAddress);
}