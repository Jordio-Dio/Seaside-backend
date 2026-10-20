package com.seaside.seaside_api.feature.esp32.model;

 
import jakarta.persistence.*;
import lombok.*;
 
import java.time.LocalDateTime;
import java.util.UUID;
 
// ─── Télémétrie Master ───────────────────────────────────
@Entity
@Table(name = "telemetrie_master")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class TelemetrieMaster {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_esp32_id", nullable = false)
    private ModuleEsp32 module;
 
    @Column(name = "batterie", nullable = false)
    private Integer batterie; // 0-100%
 
    @Column(name = "releve_le", nullable = false, updatable = false)
    private LocalDateTime releveLe;
 
    @PrePersist
    protected void onCreate() { this.releveLe = LocalDateTime.now(); }
}
 
// ─── Télémétrie Slave ────────────────────────────────────
@Entity
@Table(name = "telemetrie_slave")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class TelemetrieSlave {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slave_module_id", nullable = false)
    private SlaveModule slave;
 
    @Column(name = "batterie", nullable = false)
    private Integer batterie; // 0-100%
 
    @Column(name = "est_connecte", nullable = false)
    private Boolean estConnecte;
 
    @Column(name = "releve_le", nullable = false, updatable = false)
    private LocalDateTime releveLe;
 
    @PrePersist
    protected void onCreate() { this.releveLe = LocalDateTime.now(); }
}