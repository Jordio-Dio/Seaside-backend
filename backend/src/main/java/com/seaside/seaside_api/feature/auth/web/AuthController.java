package com.seaside.seaside_api.feature.auth.web;

//import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seaside.seaside_api.feature.auth.dto.LoginRequest;
import com.seaside.seaside_api.feature.auth.dto.RefreshRequest;
import com.seaside.seaside_api.feature.auth.dto.RegisterRequest;
import com.seaside.seaside_api.feature.auth.dto.AuthResponse;
import com.seaside.seaside_api.feature.utilisateur.model.Utilisateur;
import com.seaside.seaside_api.feature.auth.service.AuthService;
import com.seaside.seaside_api.feature.auth.service.RefreshTokenService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> inscrire(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.inscrire(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> seConnecter(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.seConnecter(request);
        return ResponseEntity.ok(response);
    }

    // POST /Auth/refresh - public (pas besoin d'acces token)
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> rafraichir(@Valid @RequestBody RefreshRequest request){
        return ResponseEntity.ok(authService.rafraichir(request.getRefreshToken()));
    }

    // POST /auth/logout natao protege (besoin d'acccess token pour savoir qui se deconnecte)
    @PostMapping("/logout")
    public ResponseEntity<Void> seDeconnecter(Authentication authentication) {

        if (authentication != null) {
            Utilisateur user = (Utilisateur) authentication.getPrincipal();
            refreshTokenService.supprimerParUtilisateur(user);
        }

        return ResponseEntity.noContent().build();
    }

    
}
