package com.seaside.seaside_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "entry_count")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntryCount {

    @Id
    @GeneratedValue
    @Column(name = "id_count")
    private UUID id;

    @Column(name = "count_value", nullable = false)
    private Integer countValue;

    @Column(name = "count_time", nullable = false)
    private LocalDateTime countTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_event", nullable = false)
    private Evenement event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_slave", nullable = false)
    private Esp32Slave slave;

    @PrePersist
    public void prePersist() {
        if (countTime == null) {
            countTime = LocalDateTime.now();
        }
    }
}