package com.seaside.seaside_api.shared.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── Format d'erreur uniforme ────────────────────────────
    @Data
    @AllArgsConstructor
    static class ErrorResponse {
        private int code;
        private String message;
        private LocalDateTime timestamp;
    }

    // Erreurs de validation (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> erreurs = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String champ = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            erreurs.put(champ, message);
        });
        return ResponseEntity.badRequest().body(erreurs);
    }

    // Mauvais email ou mot de passe
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, "Email ou mot de passe incorrect",
                        LocalDateTime.now()));
    }

    // Erreurs métier générales (email déjà utilisé, ....)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        log.warn("Erreur métier : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, ex.getMessage(), LocalDateTime.now()));
    }

    // ─── Erreurs d'infrastructure Spring (sinon → 500 à tort) ─
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNonTrouve(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Ressource introuvable", LocalDateTime.now()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethode(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse(405, "Méthode non autorisée : " + ex.getMethod(),
                        LocalDateTime.now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageIllisible(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Corps de requête invalide ou mal formé",
                        LocalDateTime.now()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleParamManquant(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Paramètre requis manquant : " + ex.getParameterName(),
                        LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Type invalide pour le paramètre : " + ex.getName(),
                        LocalDateTime.now()));
    }

    // Toutes les autres erreurs
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Erreur interne non gérée", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500,
                        "Erreur interne du serveur : " + ex.getClass().getSimpleName()
                                + " — " + (ex.getMessage() != null ? ex.getMessage() : "détail inconnu"),
                        LocalDateTime.now()));
    }
}
