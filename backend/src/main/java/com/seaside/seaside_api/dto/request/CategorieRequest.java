package com.seaside.seaside_api.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategorieRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String nom; // vip , simple, ...

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.0", message = "Le prix ne peut pas etre negatif")
    private BigDecimal prix;

    @Min(value = 0)
    private Integer capacite = 0;

    private String couleur; // code dispositif / licence associée à l'entrée

}
