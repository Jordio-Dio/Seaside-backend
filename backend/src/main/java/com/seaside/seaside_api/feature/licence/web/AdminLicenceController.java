package com.seaside.seaside_api.feature.licence.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seaside.seaside_api.feature.licence.dto.ChangerStatutLicenceRequest;
import com.seaside.seaside_api.feature.licence.dto.LicenceRequest;
import com.seaside.seaside_api.feature.licence.dto.LicenceResponse;
import com.seaside.seaside_api.feature.licence.service.LicenceService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Console admin — gestion des licences (délivrance / statut).
 * Routes sous /admin/** → ROLE_ADMIN exigé (SecurityConfig).
 */
@RestController
@RequestMapping("/admin/licences")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminLicenceController {

    private final LicenceService licenceService;

    // GET /admin/licences — toutes les licences
    @GetMapping
    public ResponseEntity<List<LicenceResponse>> lister() {
        return ResponseEntity.ok(licenceService.lister());
    }

    // POST /admin/licences — délivrer une licence à un slave
    // body : { "slaveId": "uuid", "dateExpiration": "2027-01-01"?, ... }
    @PostMapping
    public ResponseEntity<LicenceResponse> creer(@Valid @RequestBody LicenceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(licenceService.creer(req));
    }

    // PATCH /admin/licences/{id}/statut — body : { "statut": "ACTIVE" | "BLOCKED" | "EXPIRED" }
    @PatchMapping("/{id}/statut")
    public ResponseEntity<LicenceResponse> changerStatut(
            @PathVariable UUID id,
            @Valid @RequestBody ChangerStatutLicenceRequest req) {
        return ResponseEntity.ok(licenceService.changerStatut(id, req));
    }
}
