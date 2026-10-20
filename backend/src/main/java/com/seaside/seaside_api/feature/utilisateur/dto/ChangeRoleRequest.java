package com.seaside.seaside_api.feature.utilisateur.dto;

import com.seaside.seaside_api.feature.utilisateur.model.RoleUsers;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeRoleRequest {

    @NotNull(message = "Le role est obligatoire")
    private  RoleUsers role; /// ADMIN OU CLIENT
}