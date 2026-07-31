package com.seaside.seaside_api.repository;

import com.seaside.seaside_api.entity.License;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LicenseRepository extends JpaRepository<License, UUID> {

    Optional<License> findByLicenseKey(String licenseKey);
}