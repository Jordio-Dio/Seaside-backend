package com.seaside.seaside_api.feature.licence.dto;

import com.seaside.seaside_api.feature.licence.model.LicenseStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * PATCH /admin/licences/{id}/statut
 * ACTIVE = activée, BLOCKED = révoquée/bloquée, EXPIRED = expirée.
 */
@Data
public class ChangerStatutLicenceRequest {

    @NotNull(message = "Le statut est obligatoire")
    private LicenseStatus statut;
}
