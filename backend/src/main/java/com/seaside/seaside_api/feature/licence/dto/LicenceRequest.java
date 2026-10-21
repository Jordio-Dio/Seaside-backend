package com.seaside.seaside_api.feature.licence.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** POST /admin/licences — délivrer une licence à un module slave. */
@Data
public class LicenceRequest {

    @NotNull(message = "Le module slave est obligatoire")
    private UUID slaveId;

    // Date d'expiration souhaitée (facultative — illimitée si absente)
    private LocalDate dateExpiration;
}
