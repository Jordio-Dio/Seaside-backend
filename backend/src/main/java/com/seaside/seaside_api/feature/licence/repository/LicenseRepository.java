package com.seaside.seaside_api.feature.licence.repository;

import com.seaside.seaside_api.feature.licence.model.License;
import com.seaside.seaside_api.feature.licence.model.LicenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LicenseRepository extends JpaRepository<License, UUID> {

    Optional<License> findByLicenseKey(String licenseKey);

    // ─── Console admin : licences par état / par slave ─────
    java.util.List<License> findAllByOrderByActivationDateDesc();
    long countByStatus(LicenseStatus status);
    boolean existsBySlaveId(UUID slaveId);
}