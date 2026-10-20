package com.seaside.seaside_api.feature.evenement.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EvenementRequest {
   
    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String nom;

    @NotNull(message = "La date est obligatoire")
    private LocalDate dateEvenement;

    @Size(max = 200)
    private String lieu;

    private String description;

    @Min(value = 0, message = "La capacite ne peut pas etre negative")
    private Integer capaciteMaximale = 0;
}

