package com.seaside.seaside_api.feature.esp32.repository;

import com.seaside.seaside_api.feature.esp32.model.BatteryTelemetry;
import com.seaside.seaside_api.feature.esp32.model.Esp32Slave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BatteryTelemetryRepository extends JpaRepository<BatteryTelemetry, UUID> {

    List<BatteryTelemetry> findBySlaveOrderByRecordedAtDesc(Esp32Slave slave);

}