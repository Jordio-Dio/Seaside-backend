# Seaside API — Architecture

> Dernière mise à jour : refactorisation **Feature-First (Vertical Slice)** — Octobre 2026
>
> 📐 Diagrammes C4 : voir [docs/ARCHITECTURE_DIAGRAMS.md](docs/ARCHITECTURE_DIAGRAMS.md)
> 🔁 **Tous les diagrammes de séquence** (auth + firmware ESP32 + licence + temps réel) : voir [docs/SEQUENCE_DIAGRAMS.md](docs/SEQUENCE_DIAGRAMS.md)
> 🧑‍💻 Briefing d'intégration frontend (contrats API + réservation de places) : voir [docs/FRONTEND_BRIEFING.md](docs/FRONTEND_BRIEFING.md)

## 1. Vue d'ensemble

L'API est organisée par **domaine métier** (feature) plutôt que par couche technique.
Chaque feature est un **coup vertical** autonome : web → service → repository → model, avec ses propres DTO.

```
com.seaside.seaside_api
├── SeasideApiApplication.java      # Point d'entrée (scan de tous les sous-packages)
│
├── shared/                         # Transverse — utilisé par toutes les features
│   ├── config/                     # OpenApiConfig (Swagger security scheme)
│   ├── security/                   # SecurityConfig, JwtFilter, JwtUtil, UserDetailsServiceImpl
│   ├── exception/                  # GlobalExceptionHandler
│   └── dto/                        # ErrorResponse (contrat d'erreur global)
│
└── feature/
    ├── auth/                       # Inscription, OTP email, login JWT, reset mot de passe
    │   ├── web/                    # AuthController, AuthPasswordController
    │   ├── service/                # AuthService, VerificationEmailService, PasswordResetService,
    │   │                           # RefreshTokenService, EmailService
    │   ├── repository/             # VerificationOtpRepository, RefreshTokenRepository,
    │   │                           # PasswordResetTokenRepository
    │   ├── model/                  # VerificationOtp, RefreshToken, PasswordResetToken
    │   └── dto/                    # LoginRequest, RegisterRequest, AuthResponse, Otp*, ...
    │
    ├── utilisateur/                # Profil, gestion admin des comptes et rôles
    │   ├── web/ service/ repository/
    │   ├── model/                  # Utilisateur (UserDetails), RoleUsers
    │   └── dto/                    # UtilisateurResponse, ChangeRoleRequest, ...
    │
    ├── evenement/                  # Événements + catégories de billets (agrégat)
    │   ├── web/ service/ repository/ mapper/
    │   ├── model/                  # Evenement, Categorie
    │   └── dto/                    # EvenementRequest, CategorieRequest, EvenementResponse
    │
    ├── entree/                     # Comptage des entrées (source ESP32 ou manuelle)
    │   ├── web/ service/ repository/
    │   ├── model/                  # Entree, EntryCount
    │   └── dto/                    # EntreeRequest
    │
    ├── esp32/                      # Périphériques IoT, MQTT, WebSocket, télémétrie
    │   ├── web/                    # Esp32Controller, TestWebSocketController
    │   ├── service/                # Esp32Service (legacy), Esp32CommandService,
    │   │                           # Esp32QueryService, Esp32TelemetryService, SeasideMqttService
    │   ├── repository/             # Esp32MasterRepository, Esp32SlaveRepository,
    │   │                           # ModuleEsp32Repository, SlaveModuleRepository, BatteryTelemetryRepository
    │   ├── model/                  # Esp32Master, Esp32Slave, ModuleEsp32, SlaveModule,
    │   │                           # BatteryTelemetry, Telemetrie
    │   ├── mqtt/                   # MqttConfig, Publisher, Subscriber, Routers,
    │   │   ├── router/             #   SeasideMqttRouter, MqttTelemetryRouter
    │   │   ├── telemetry/          #   SeasideMqttSubscriber, MqttTelemetrySubscriber
    │   │   └── WebSocketConfig, WebSocketPublisher (STOMP /topic/...)
    │   ├── mapper/                 # Esp32Mapper
    │   └── dto/                    # Esp32MasterDTO, SlaveStatusDTO, AlerteDTO,
    │                               # DashboardsStatsDTO, CategorieStatDTO, ...
    │
    ├── licence/                    # 🚧 À développer : licences des modules ESP32
    │   ├── model/                  # License, LicenseStatus (ACTIVE/EXPIRED/BLOCKED)
    │   └── repository/             # LicenseRepository
    │
    └── stats/                      # 🚧 Stubs : statistiques globales
        ├── web/ service/ dto/
```

## 2. Règles d'architecture

1. **Une feature = un dossier vertical.** Tout ce qui sert un domaine y vit : controller, service,
   repository, entity, DTO, mapper. Pas de dossier `controller/` ou `service/` global.
2. **`shared/` est le seul kernel transverse.** On n'y met que du code réellement utilisé par
   plusieurs features (sécurité, erreurs globales, config). Une feature ne doit jamais dépendre
   d'une autre via `shared` en contournant son API.
3. **Dépendances inter-features autorisées mais explicites** (imports visibles), ex :
   - `feature.licence.model.License` → `feature.esp32.model.Esp32Slave`
   - `feature.esp32` → `feature.evenement.model.Categorie` (un slave = une porte = une catégorie)
   - `feature.auth` → `feature.utilisateur.model.Utilisateur`
4. **Pas d'imports wildcard** (`entity.*`) : chaque dépendance est explicite pour rendre les
   couplages visibles lors des refactorisations.
5. **Les DTO ne sortent jamais du package de leur feature**, et une entity JPA n'est jamais
   exposée telle quelle dans une API (mapper vers le DTO de la feature).
6. **Les endpoints REST ne changent pas** (`/api/auth/...`, `/api/evenements/...`) : ce refactor
   est **100 % interne**, le contrat frontend est préservé.

## 3. Conventions de nommage (métier, en français)

| Concept | Convention | Exemple |
|---|---|---|
| Controller | `XxxController` | `EvenementController` |
| Service | `XxxService` | `VerificationEmailService` |
| Repository | `XxxRepository` | `RefreshTokenRepository` |
| Entity JPA | nom métier singulier | `Evenement`, `Esp32Slave` |
| Requête API | `XxxRequest` | `LoginRequest` |
| Réponse API | `XxxResponse` / `XxxDTO` | `AuthResponse`, `SlaveStatusDTO` |

## 4. Flux transverses

- **JWT** : `SecurityConfig` (routes publiques) → `JwtFilter` → `JwtUtil` → `UserDetailsServiceImpl`
  (qui charge `Utilisateur` depuis `feature.utilisateur.repository`).
- **MQTT → WebSocket** : broker HiveMQ → `MqttSubscriber` / `SeasideMqttSubscriber` →
  services esp32 → `WebSocketPublisher` (STOMP) → frontend React (topic par événement).
- **Emails** : `feature.auth.service.EmailService` (@Async, Gmail SMTP) — OTP et reset password.

## 5. Prochains chantiers sur cette base

| Chantier | Point de départ |
|---|---|
| Licences ESP32 | `feature/licence` (model + repo existants) : ajouter `web/`, `service/`, activation via MQTT, expiration |
| Ticketing visiteur | nouvelle feature `feature/ticketing` : `Reservation` liée à `Categorie`, QR code, endpoint public |
| Migration Flyway V1 | versionner le schéma actuel (`db/migration/V1__init_schema.sql`) — requis avant toute prod |
| Diagrammes C4 | ✅ livrés dans [docs/ARCHITECTURE_DIAGRAMS.md](docs/ARCHITECTURE_DIAGRAMS.md) (C4 L1/L2 + 4 séquences) |
