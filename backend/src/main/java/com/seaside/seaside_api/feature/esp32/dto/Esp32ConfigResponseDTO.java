package com.seaside.seaside_api.feature.esp32.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Esp32ConfigResponseDTO {
    private String evenementId;
    private String nomEvenement;
    private Integer seuilAlerteBatterie; // ex: 20%
    private List<SlaveConfigDTO> slaves;
}