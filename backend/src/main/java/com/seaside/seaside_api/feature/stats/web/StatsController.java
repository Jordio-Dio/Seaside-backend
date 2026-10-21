package com.seaside.seaside_api.feature.stats.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seaside.seaside_api.feature.stats.dto.StatsResponse;
import com.seaside.seaside_api.feature.stats.service.StatsService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

/**
 * Console admin — cartes KPI du dashboard.
 * Accès restreint à ROLE_ADMIN via le préfixe /admin/** (SecurityConfig).
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class StatsController {

    private final StatsService statsService;

    // GET /admin/dashboard — KPIs globaux (users, events, entrées,
    // revenus, demandes, licences, parc ESP32)
    @GetMapping("/dashboard")
    public ResponseEntity<StatsResponse> dashboard() {
        return ResponseEntity.ok(statsService.dashboard());
    }
}
