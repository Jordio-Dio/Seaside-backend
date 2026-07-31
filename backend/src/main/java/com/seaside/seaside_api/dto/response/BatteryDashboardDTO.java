package com.seaside.seaside_api.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatteryDashboardDTO {

    private Esp32MasterDTO master;

    private List<Esp32SlaveDTO> slaves;

    private Integer moyenneBatterie;

    private Integer nombreModules;

    private Integer nombreConnectes;

    private Integer nombreDeconnectes;
}