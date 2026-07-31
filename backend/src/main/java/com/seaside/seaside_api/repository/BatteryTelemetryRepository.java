package com.seaside.seaside_api.repository;

import com.seaside.seaside_api.entity.BatteryTelemetry;
import com.seaside.seaside_api.entity.Esp32Slave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BatteryTelemetryRepository extends JpaRepository<BatteryTelemetry, UUID> {

    List<BatteryTelemetry> findBySlaveOrderByRecordedAtDesc(Esp32Slave slave);
}