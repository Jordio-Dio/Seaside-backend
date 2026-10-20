package com.seaside.seaside_api.feature.esp32.repository;

import com.seaside.seaside_api.feature.esp32.model.Esp32Master;
import com.seaside.seaside_api.feature.esp32.model.Esp32Slave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface Esp32SlaveRepository extends JpaRepository<Esp32Slave, UUID> {

    Optional<Esp32Slave> findByMacAddress(String macAddress);

    Optional<Esp32Slave> findByMasterAndSlaveNumber(Esp32Master master,
                                                Integer slaveNumber);
}