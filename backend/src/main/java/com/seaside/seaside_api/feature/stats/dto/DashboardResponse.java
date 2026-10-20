package com.seaside.seaside_api.feature.stats.dto;
 
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
 
// ─── Réponse Catégorie ───────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class CategorieResponse {
    private UUID id;
    private String nom;
    private BigDecimal prix;
    private Integer capacite;
    private String couleur;
    private Boolean estActif;
    private Integer nombreEntrees;       // calculé
    private BigDecimal revenuCategorie;  // calculé
}
 
// ─── Réponse Événement ───────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class EvenementResponse {
    private UUID id;
    private String nom;
    private LocalDate dateEvenement;
    private String lieu;
    private String description;
    private Integer capaciteMaximale;
    private Boolean estActif;
    private LocalDateTime dateCreation;
    private List<CategorieResponse> categories;
    private Integer totalPersonnes;      // calculé depuis les catégories
    private BigDecimal totalRevenus;     // calculé depuis les catégories
}
 
// ─── Réponse Entrée ──────────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class EntreeResponse {
    private UUID id;
    private UUID categorieId;
    private String nomCategorie;
    private Integer comptage;
    private String source;
    private LocalDateTime enregistreLe;
}
 
// ─── Réponse Dashboard ───────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardResponse {
    private Integer totalPersonnes;
    private BigDecimal totalRevenus;
    private List<CategorieStatResponse> parCategorie;
    private List<EntreeRecenteResponse>  dernieresEntrees;
}
 
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class CategorieStatResponse {
    private String nomCategorie;
    private String couleur;
    private Integer nombrePersonnes;
    private BigDecimal revenu;
    private BigDecimal prix;
}
 
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class EntreeRecenteResponse {
    private String nomCategorie;
    private String couleur;
    private Integer comptage;
    private String source;
    private LocalDateTime enregistreLe;
}
 