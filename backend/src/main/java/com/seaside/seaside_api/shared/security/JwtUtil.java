package com.seaside.seaside_api.shared.security;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;

@Component
public class JwtUtil {
    
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private Long expiration;// en milliseconde  (24h) mais en seconde

    // ------------Gereration du Token ------------
    public String genererToken(UserDetails userdetails) {
        Map<String, Object> claims = new HashMap<>();
    

        // Ajout du role
        String role = userdetails.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
        claims.put("role", role);

        return construireToken(claims, userdetails.getUsername());
        
    }

    private String construireToken(Map<String, Object> claims, String email) {
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getCleSecrete())
                .compact();
    }


    //  POUR EXTRAIRE LE role (optionnel, pour déboguer)
    public String extraireRole(String token) {
        return Jwts.parser()
                .verifyWith(getCleSecrete())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }


     // ------------ VALIDATION DU TOKEN -----------------
     public boolean estValide(String token, UserDetails userDetails) {
        final String email = extraireEmail(token);
        return email.equals(userDetails.getUsername()) && !estExpire(token);
     }

     public String extraireEmail(String token) {
        return Jwts.parser()
                .verifyWith(getCleSecrete())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
     }

     public boolean estExpire(String token) {
        return extraireExpiration(token).before(new Date());
     }

     private Date extraireExpiration(String token) {
        return Jwts.parser()
                .verifyWith(getCleSecrete())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
     }

     // ─── Clé secrète ────────────────────────────────────────
    // Convertit la clé base64 du application.properties en SecretKey
 
    private SecretKey getCleSecrete() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
