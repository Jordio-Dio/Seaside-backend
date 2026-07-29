package com.seaside.seaside_api.entity;

import jakarta.persistence.*;
import lombok.*;
import com.seaside.seaside_api.entity.enums.LicenseStatus;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "license")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class License {

    @Id
    @GeneratedValue
    @Column(name = "id_license")
    private UUID id;

    @Column(name = "license_key", nullable = false, unique = true)
    private String licenseKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LicenseStatus status;

    @Column(name = "activation_date")
    private LocalDate activationDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_slave", nullable = false, unique = true)
    private Esp32Slave slave;
}
