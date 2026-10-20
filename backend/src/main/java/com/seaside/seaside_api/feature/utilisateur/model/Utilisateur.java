package com.seaside.seaside_api.feature.utilisateur.model;
import com.seaside.seaside_api.feature.evenement.model.Evenement;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.seaside.seaside_api.feature.utilisateur.model.RoleUsers;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "utilisateurs")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Builder
public class Utilisateur implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "nom_utilisateur", nullable = false, unique = true, length = 50)
    private String nomUtilisateur;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @NotBlank
    @Column(name = "mots_de_passe_hash", nullable = false, length = 255)
    private String motsDePasseHash;

    @Column(name = "est_actif", nullable = false)
    @Builder.Default
    private boolean estActif = true;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private RoleUsers role = RoleUsers.CLIENT;

    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Evenement> evenements;

    //--------------- Hooks JPA ----------------------
    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
    }

    // --------------UserDetails (SPRING Security) ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { 
        return motsDePasseHash; 
    }
    @Override public String getUsername() { 
        return email; 
    }
    @Override public boolean isEnabled() { 
        return estActif; 
    }

    @Override
    public boolean isAccountNonExpired() { return true ;}

    @Override
    public boolean isAccountNonLocked() { return estActif; }

    @Override
    public boolean isCredentialsNonExpired() {return true; }
}
