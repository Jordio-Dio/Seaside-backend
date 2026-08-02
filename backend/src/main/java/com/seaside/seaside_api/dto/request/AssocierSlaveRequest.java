package com.seaside.seaside_api.dto.request;

 
import jakarta.validation.constraints.NotNull;
import lombok.Data;
 
import java.util.UUID;
 
// ─── Associer un slave à un master (dynamique) ─────────
// La catégorie appartient au master sous le modèle B
@Data
public class AssocierSlaveRequest {
  
    @NotNull(message = "Le numéro du slave est obligatoire")
    private Integer slaveId;       // numéro physique du slave (1, 2, 3...)
  
    private String nom;            // "Slave Porte VIP"

    private String macAddress;     // adresse MAC du slave

    // Catégorie rattachée au master de ce slave
    // null = catégorie inchangée
    private UUID categorieId;
}