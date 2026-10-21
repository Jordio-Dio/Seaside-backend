package com.seaside.seaside_api.feature.licence.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.seaside.seaside_api.feature.licence.model.LicenseStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Réponse de la console admin pour une licence. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenceResponse {

    private UUID id;
    private String licenseKey;
    private LicenseStatus statut;
    private LocalDate activationDate;
    private LocalDate expirationDate;
    private UUID slaveId;
    private String nomSlave;
}
