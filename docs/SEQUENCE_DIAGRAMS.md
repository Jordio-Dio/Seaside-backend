# Seaside / SmartCount — Tous les diagrammes de séquence

> Catalogue **exhaustif** (aucun flux absent) — Octobre 2026.
> Sources : code backend vérifié fichier par fichier + **documentation firmware**
> `SmartCount_Documentation_Payloads.pdf` (9 pages, équipe firmware).
> Contexte et diagrammes C4 : [ARCHITECTURE_DIAGRAMS.md](ARCHITECTURE_DIAGRAMS.md).

---

## Carte de tous les diagrammes du projet

| # | Diagramme | Ce qu'il explique | Source |
|---|---|---|---|
| 1 | Inscription avec OTP email | Vérification MX, code 6 chiffres, BCrypt, refresh token | backend |
| 2 | Login JWT / requête protégée / refresh / logout | tout le cycle de vie de l'authentification | backend |
| 3 | **Entrée détectée — chaîne complète** | capteur ultrason → ESP-NOW → master → MQTT → BDD → STOMP | firmware + backend |
| 4 | Télémétrie maître/esclave + alertes | battements MQTT 5 s, seuils batterie, déconnexions | firmware + backend |
| 5 | Commande distante vers un master | REST admin → `seaside/config/{MAC}` → firmware ; seuil, incrément, rapport visuel | firmware + backend |
| 6 | Appairage ESP-NOW master ↔ slave | paquet PAIRING, rôle slave, 10 slaves max | firmware |
| 7 | Mise en route réseau d'un master | AP `SmartCount-XXX` → `192.168.4.1` → WiFi STA → MQTT TLS | firmware |
| 8 | **Licence** : génération, injection, effet | format `SMC-…`, blocage de la publication des entrées | firmware + `feature/licence` à venir |
| 9 | Annonce d'un esclave | `POST /api/register_slave` toutes les 10 s, règle « active = vu < 30 s » | firmware |
| 10 | WebSocket local du master | premier message entier brut puis `{"total":n}` | firmware |
| 11 | Session dashboard frontend | login → REST dashboard → abonnements STOMP → flux temps réel | frontend + backend |
| 12 | **Onboarding organisateur** (nouveau) | inscription → demande → validation admin → licence active | backend (à implémenter) |
| 13 | **Abonnement espèces + déblocage** (nouveau) | paiement physique → admin saisit → `ACTIF` → licence/réactivation | backend (à implémenter) |
| 14 | **Publication d'événement** (nouveau) | soumission → admin accepte/masque → page publique | backend (à implémenter) |
| 15 | **Réservation visiteur** (nouveau) | intention non engageante → paiement guichet → place engagée | backend (à implémenter) |

---

## 1. Inscription avec vérification OTP email

```mermaid
sequenceDiagram
    autonumber
    actor U as Utilisateur
    participant FE as SPA React
    participant AC as AuthPasswordController<br/>feature/auth/web
    participant VES as VerificationEmailService<br/>feature/auth/service
    participant ES as EmailService<br/>@Async
    participant DB as PostgreSQL

    U->>FE: saisit email + mot de passe
    FE->>AC: POST /api/auth/verify-email
    AC->>VES: envoyerOtp(email)
    VES->>DB: email déjà utilisé ?
    alt email déjà utilisée
        VES-->>FE: 400 "Cet email est déjà associé à un compte"
    end
    VES->>VES: vérifie l'enregistrement MX DNS du domaine
    VES->>DB: supprime les anciens OTP, INSERT code 6 chiffres expire 10 min
    VES->>ES: envoyerOtpVerification(email, code)
    ES-->>U: email Gmail avec le code OTP
    AC-->>FE: 200 OtpResponse {message, email}

    U->>FE: saisit le code à 6 chiffres
    FE->>AC: POST /api/auth/confirm-otp
    AC->>VES: validerOtp(email, code)
    alt code absent ou déjà utilisé
        VES-->>FE: 400 "Code invalide ou déjà utilisé"
    else code expiré
        VES-->>FE: 400 "Code expiré"
    else code valide
        VES->>DB: est_verifie = true, non réutilisable
        AC-->>FE: 200 "Email vérifié avec succès"
    end

    FE->>AC: POST /api/auth/register
    AC->>VES: emailEstVerifie(email)
    AC->>DB: INSERT utilisateurs, hash BCrypt cost 12
    AC->>DB: DELETE + INSERT refresh_token UUID 7 j
    AC-->>FE: 200 AuthResponse {token, refreshToken, role}
```

---

## 2. Login JWT, requête protégée, refresh, logout

```mermaid
sequenceDiagram
    autonumber
    actor U as Utilisateur
    participant FE as SPA React
    participant AC as AuthController
    participant AM as AuthenticationManager
    participant UDS as UserDetailsServiceImpl
    participant JU as JwtUtil
    participant RTS as RefreshTokenService
    participant JF as JwtFilter
    participant API as Controller protégé
    participant DB as PostgreSQL

    rect rgb(235, 245, 255)
    note over U,DB: 1 — Connexion
    U->>FE: email + mot de passe
    FE->>AC: POST /api/auth/login
    AC->>AM: authenticate(email, motDePasse)
    AM->>UDS: loadUserByUsername(email)
    UDS->>DB: SELECT utilisateurs WHERE email
    AM->>AM: vérifie hash BCrypt(12)
    alt identifiants invalides
        AM-->>AC: BadCredentialsException
        AC-->>FE: 401 {code:401, message:"Email ou mot de passe incorrect"}
    end
    AC->>JU: genererToken — claims sub=email, role, exp 1 h
    AC->>RTS: creer(utilisateur)
    RTS->>DB: DELETE ancien refresh + INSERT UUID 7 j
    AC-->>FE: 200 {token, refreshToken, type:"Bearer"}
    end

    rect rgb(235, 255, 240)
    note over FE,API: 2 — Requête protégée, stateless
    FE->>JF: GET /api/evenements — Header Authorization Bearer
    JF->>JU: extraireEmail(token) — signature HS256
    JF->>UDS: loadUserByUsername(email)
    JF->>JU: estValide — email correspond + non expiré
    JF->>JF: SecurityContextHolder.setAuthentication
    JF->>API: chaîne de filtres → controller
    API-->>FE: 200 données
    note over JF: token absent/expiré → pas de contexte → 403
    end

    rect rgb(255, 245, 235)
    note over FE,DB: 3 — Refresh après expiration de l'access token
    FE->>AC: POST /api/auth/refresh {refreshToken}
    AC->>RTS: valider(refreshToken)
    RTS->>DB: SELECT refresh_token
    alt refresh absent ou expiré
        RTS->>DB: DELETE si expiré
        RTS-->>FE: 400 "Reconnexion requise"
    end
    AC-->>FE: 200 nouvel access token, refresh inchangé
    end

    rect rgb(250, 235, 245)
    note over FE,DB: 4 — Déconnexion
    FE->>AC: POST /api/auth/logout
    AC->>RTS: supprimerParUtilisateur
    RTS->>DB: DELETE refresh_tokens
    note right of DB: sans refresh token, plus de renewal<br/>l'access token meurt en 1 h maximum
    end
```

---

## 3. Entrée détectée — chaîne complète ESP-NOW → MQTT → BDD → STOMP

Le parcours réel d'une personne : capteur ultrason du **slave**, radio ESP-NOW,
master qui publie sur `seaside/entrees/{licence}` (uniquement si la licence est valide),
backend Spring, puis dashboard React.

```mermaid
sequenceDiagram
    autonumber
    actor V as Visiteur (porte)
    participant CAP as Capteur ultrason<br/>du slave
    participant SLV as Firmware slave
    participant MST as Firmware master
    participant BR as HiveMQ Cloud<br/>MQTT TLS 8883
    participant AD as MqttConfig.mqttAdapter<br/>Spring Integration QoS 1
    participant RT as SeasideMqttRouter
    participant SV as SeasideMqttService
    participant DB as PostgreSQL
    participant WS as WebSocketPublisher
    participant FE as SPA React

    V->>CAP: passage détecté
    CAP->>SLV: impulsion capteur
    SLV->>SLV: count slave + 1
    SLV->>MST: ESP-NOW paquet COUNT<br/>type=1, moduleId, count, seuil
    note right of SLV: struct de 148 octets fixe<br/>COUNT/PAIRING/CONFIG/RESET/FORMAT
    MST->>MST: diff = count - dernier count connu, min 0<br/>total += diff, current += diff
    note over MST: +3 d'un coup = 3 messages entrees identiques

    MST->>MST: licence valide ?
    alt licence valide ou absente
        MST->>BR: PUBLISH seaside/entrees/{licence}<br/>{mac, moduleId, role, count, total,<br/>battery, licence, timestamp, connected}
        note right of MST: licence = code SMC-…<br/>ou "default" sans licence
    else licence expirée
        MST--xBR: aucune publication — comptage local poursuivi
    end

    BR->>AD: message livré, QoS 1
    AD->>RT: router(topic, payload)
    RT->>SV: traiterEntree(topic, data)
    SV->>DB: master = findFirstByLicence(licence)
    note over SV: fallback : categorie.findFirstByCouleur(licence)
    alt master ou catégorie introuvable
        SV->>SV: log.warn, message ignoré
    else capacité de la catégorie atteinte
        SV->>WS: publierAlerte CAPACITE_MAX
        WS->>FE: /topic/alerte/{evenementId}
    else entrée acceptée
        SV->>DB: INSERT entrees comptage=1 source=esp32
        SV->>DB: SUM comptages catégorie + événement
        WS->>FE: /topic/comptage/{evenementId}<br/>{categorieId, nomCategorie, comptage,<br/>totalCategorie, revenuCategorie,<br/>totalEvenement, totalRevenus}
    end
```

---

## 4. Télémétrie maître/esclave et alertes

Le master publie son état **toutes les 5 secondes** ; chaque paquet ESP-NOW COUNT
reçu d'un slave déclenche aussi une télémétrie slave.

```mermaid
sequenceDiagram
    autonumber
    participant MST as Firmware master
    participant BR as HiveMQ Cloud
    participant SV as SeasideMqttService
    participant DB as PostgreSQL
    participant WS as WebSocketPublisher
    participant FE as SPA React

    loop toutes les 5 s
        MST->>BR: PUBLISH seaside/telemetry/master<br/>{timestamp, battery, licence, total, current,<br/>ip, connected, master{moduleId, role, mac,<br/>count, seuil, battery, active}, modules[],<br/>modulesCount, masterCount, slavesTotal}
        BR->>SV: chaîne MqttConfig → Router → Subscriber
        SV->>SV: mac = data.get("mac") ⚠ absent en top-level
        SV->>DB: UPDATE esp32_master battery/connected/last_seen
        SV->>SV: verifierBatterieMaster seuils 10 % et 20 %
        alt batterie <= 10 %
            WS->>FE: /topic/alerte BATTERIE_MASTER_CRITIQUE
        else batterie <= 20 %
            WS->>FE: /topic/alerte BATTERIE_MASTER_FAIBLE
        end
        WS->>FE: /topic/telemetrie/{evenementId}<br/>{batterieMaster, masterConnecte,<br/>derniereActiviteMaster, totalPersonnes,<br/>totalRevenus, slaves[]}
    end

    MST->>BR: PUBLISH seaside/telemetry/slave/{moduleId}<br/>{slaveId:"Slave-1", mac, count, seuil, active}
    note right of MST: count = valeur absolue, pas un delta<br/>battery absent du payload réel
    BR->>SV: même chaîne
    SV->>DB: UPDATE esp32_slave par mac, INSERT battery_telemetry
    alt slave passe de actif à inactif
        WS->>FE: /topic/alerte SLAVE_DECONNECTE CRITIQUE
    else batterie slave <= 20 %
        WS->>FE: /topic/alerte BATTERIE_SLAVE_FAIBLE
    end
    WS->>FE: /topic/telemetrie/{evenementId} rafraîchi
    note over FE: ⚠ le firmware ne remet JAMAIS active à false<br/>le backend doit détecter le silence, pas le flag
```

---

## 5. Commande distante vers un master (REST → MQTT → firmware)

```mermaid
sequenceDiagram
    autonumber
    actor A as Admin SPA React
    participant EC as Esp32Controller
    participant ES as Esp32Service
    participant MP as MqttPublisher
    participant BR as HiveMQ Cloud
    participant FW as Firmware master

    A->>EC: POST /api/esp32/masters/{id}/config/envoyer<br/>Bearer JWT
    EC->>ES: envoyerConfigManuelle
    ES->>ES: vérifie que l'événement appartient à l'utilisateur
    ES->>MP: publier("seaside/config/{MAC}", config)
    MP->>BR: PUBLISH seaside/config/{MAC}<br/>{evenementId, nomEvenement,<br/>seuilAlerteBatterie, slaves[]}
    note right of MP: le firmware est abonné aux deux formes<br/>MAC avec et sans deux-points
    BR->>FW: livraison

    rect rgb(245, 245, 255)
    note over FW,BR: Commandes génériques reconnues par le firmware
    FW->>FW: {"commande":"incrementer"} — comme le bouton +
    FW->>FW: {"commande":"decrementer"} — minimum 0
    FW->>FW: {"commande":"reset_module"} — redémarre SANS zérorer
    FW->>FW: {"commande":"format"} — formatage mémoire
    FW->>FW: {"seuil":60} — 10 à 100, alerte si hors bornes
    FW->>BR: PUBLISH seaside/rapport/visuel — sur {"command":"report"}
    note right of FW: aucun accusé de réception : prochain heartbeat 5 s<br/>pour confirmer
    end
```

---

## 6. Appairage ESP-NOW master ↔ slave

```mermaid
sequenceDiagram
    autonumber
    actor Tech as Technicien
    participant M as Firmware master
    participant S as Firmware slave

    Tech->>M: appui "add slave" via API locale<br/>POST /api/slaves/add {mac, moduleId?, wifiSSID?, wifiPassword?}
    alt slave déjà présent
        M-->>Tech: 200 {status:"already_exists"}
    end
    M->>S: ESP-NOW paquet PAIRING type=2<br/>moduleId, wifiSSID, wifiPassword, masterMAC
    alt module déjà slave
        S->>S: paquet ignoré
    else module neuf
        S->>S: enregistre SSID/mot de passe, passe en rôle slave
        S->>S: sauvegarde LittleFS, redémarre après 2 s
    end
    S->>M: COUNT suivant — confirmé à la liste des slaves
    note over M: 10 slaves maximum — le 11e est ignoré<br/>liste RAM perdue à chaque redémarrement du master
```

---

## 7. Mise en route réseau d'un master

```mermaid
sequenceDiagram
    autonumber
    actor Tech as Technicien
    participant PC as PC / Smartphone
    participant M as Firmware master
    participant W as Réseau WiFi local
    participant BR as HiveMQ Cloud

    M->>M: démarrage — rôle attribué automatiquement<br/>premier module = master
    M->>W: ouvre l'AP SoftAP SmartCount-XXX<br/>XXX = 3 derniers caractères hex de la MAC
    Tech->>PC: se connecte à l'AP, IP 192.168.4.1
    Tech->>M: POST /api/wifi {ssid, password} — WiFi STA
    M->>W: connexion au WiFi local
    Tech->>M: POST /api/mqtt {server, port:8883, user, password}
    M->>BR: connexion TLS 8883<br/>clientId Seaside2-Master-{MAC sans ':'}
    note right of M: certificat non vérifié côté firmware<br/>identifiants stockés en clair dans config.json
    M->>BR: heartbeat seaside/telemetry/master toutes les 5 s
```

---

## 8. Licence : génération, injection, effet sur les entrées

Le firmware **auto-génère** ses licences (`POST /api/license/generate`) ou accepte
une licence externe (`POST /api/config/license`). Le backend `feature/licence`
(entité `License` existante, service à venir) devra s'aligner sur ce format.

```mermaid
sequenceDiagram
    autonumber
    actor Tech as Technicien / Admin
    participant M as Firmware master<br/>API locale 192.168.4.1
    participant BK as Seaside API<br/>feature/licence à venir
    participant BR as HiveMQ Cloud
    actor V as Visiteur

    alt licence générée par le module
        Tech->>M: POST /api/license/generate {durationDays:90}
        M-->>Tech: 200 {licenseCode:"SMC-A1B2C3D4E5F6-20270118-9F3A",<br/>licenseDate, licenseDuration, licenseExpiry}
    else licence fournie par l'extérieur
        Tech->>M: POST /api/config/license<br/>{licence, date, duration, expiry}
        M-->>Tech: 200
    end
    note over M: format SMC-{MAC sans ':'}-{AAAAMMJJ expire}<br/>ou 00000000 illimitée -{4 hex}
    M->>M: GET /api/status → licenseValid

    BK->>M: future sync : validation periodique<br/>de la licence côté backend
    note over BK: ⚧ feature/licence encore vide —<br/>à construire pour piloter les licences depuis le cloud

    loop à chaque passage détecté
        V->>M: capteur ultrason
        alt licenseValid = true
            M->>BR: PUBLISH seaside/entrees/{licence}
        else licence expirée
            M--xBR: publication bloquée
            note over M: le comptage local continue,<br/>seule la publication est bloquée
        end
    end
```

---

## 9. Annonce d'un esclave vers la passerelle

```mermaid
sequenceDiagram
    autonumber
    participant S as Firmware slave
    participant M as Master / passerelle<br/>réseau local

    loop toutes les 10 s
        S->>M: POST /api/register_slave<br/>{mac, ip, moduleId, count, seuil}
        alt champs mac ou ip manquants
            M-->>S: 400 {error:"Champs mac ou ip manquants"}
        else OK
            M-->>S: 200 {status:"ok"}
        end
    end
    note over M: règle « active » = vu il y a moins de 30 s<br/>⚠ ces annonces alimentent /api/slaves et /api/modules_count<br/>MAIS n'incrémentent PAS total — seul ESP-NOW compte
```

---

## 10. WebSocket local du master

```mermaid
sequenceDiagram
    autonumber
    participant B as Navigateur local
    participant M as Firmware master<br/>ws://192.168.4.1/ws

    B->>M: connexion WebSocket
    M->>B: message texte = entier nu "42"<br/>total pour un master, compteur local pour un slave
    loop à chaque changement de compteur
        M->>B: message texte = JSON {"total":42}
    end
    note over B: aucun message attendu du client<br/>⚠ format hétérogène : 1er message entier, puis JSON<br/>distinct du STOMP /ws du backend
```

---

## 11. Session dashboard frontend (REST + STOMP)

```mermaid
sequenceDiagram
    autonumber
    actor O as Organisateur
    participant FE as SPA React
    participant API as Seaside API
    participant ST as STOMP /ws
    participant BR as HiveMQ Cloud

    FE->>API: POST /api/auth/login
    API-->>FE: {token, refreshToken}
    FE->>API: GET /api/evenements
    API-->>FE: liste des événements + catégories
    FE->>ST: connexion SockJS ws://…/api/ws
    ST-->>FE: connecté
    FE->>ST: SUBSCRIBE /topic/comptage/{evenementId}
    FE->>ST: SUBSCRIBE /topic/telemetrie/{evenementId}
    FE->>ST: SUBSCRIBE /topic/alerte/{evenementId}
    FE->>API: GET /api/esp32/evenements/{id}/dashboard
    API-->>FE: DashboardsStatsDTO — état initial

    par les temps qui viennent
        BR->>API: entrée / télémétrie / alerte firmware
        API->>ST: publication sur le topic correspondant
        ST->>FE: mise à jour temps réel du compteur
    end
```

---

## 12. Onboarding organisateur — demande, validation admin, licence

Décisions D1/D2 (cf. `INSCRIPTION_ORGANISATEUR_RECOMMANDATIONS.md`) : inscription
libre en rôle `CLIENT` (**le rôle n'est plus lisible dans la requête** — correction P0),
demande d'activation envoyée à l'admin unique, activation seulement après validation +
paiement de l'abonnement **en espèces**, saisi par l'admin.

```mermaid
sequenceDiagram
    autonumber
    actor ORG as Organisateur
    participant FE as SPA React
    participant DC as DemandesController<br/>feature/organisateur/web
    participant DS as DemandeService<br/>feature/organisateur/service
    actor ADM as Admin (console)
    participant LS as LicenceService<br/>feature/licence à venir
    participant ES as EmailService @Async
    participant DB as PostgreSQL

    rect rgb(235, 245, 255)
    note over ORG,DB: 1 — Inscription (voir diagramme 1 pour l'OTP)
    ORG->>FE: POST /api/auth/register
    note over FE: le champ `role` est **ignoré** :<br/>le serveur force toujours CLIENT
    FE->>DC: POST /api/auth/register (nom, email, motDePasse)
    DC->>DB: INSERT utilisateurs role=CLIENT, est_actif=true
    note over DB: le compte existe mais le dashboard<br/>reste **verrouillé** tant qu'aucune licence active
    end

    rect rgb(245, 245, 255)
    note over ORG,DB: 2 — Dépôt de la demande d'activation
    ORG->>FE: formulaire "Activer mon compte organisateur"
    FE->>DC: POST /api/organisateur/demandes
    DC->>DS: creerDemande(utilisateur, motif, type)
    DS->>DB: INSERT demandes (SOUMISE, dateDemande)
    DS->>ES: notifierNouvelleDemande(admin)
    ES-->>ADM: email "Nouvelle demande à traiter"
    DC-->>FE: 200 DemandeResponse {statut:SOUMISE}
    note over DS: job @Scheduled :<br/>SOUMISE > 7 j → EXPIREE,<br/>EXPIREE/REFUSEE > 30 j → purge
    end

    rect rgb(255, 245, 235)
    note over ADM,DB: 3 — Traitement par l'admin
    ADM->>FE: console admin GET /api/admin/demandes?statut=SOUMISE
    FE->>DC: requête avec JWT ROLE_ADMIN
    DC->>DS: lister
    DS-->>ADM: liste paginée
    alt Refuser
        ADM->>DC: POST /api/admin/demandes/{id}/refuser {motif}
        DC->>DS: refuser(id, motif, adminId)
        DS->>DB: UPDATE REFUSEE + motif + traitePar + dateTraitement
        DS->>ES: emailDecision(ORG, REFUSEE, motif)
        note over DS: l'organisateur peut se<br/>réinscrire après 30 j
    else Ignorer (mis de côté)
        ADM->>DC: POST /api/admin/demandes/{id}/ignorer
        DS->>DB: UPDATE EN_REVUE (remise en file)
    else Approuver
        ADM->>DC: POST /api/admin/demandes/{id}/approuver
        DS->>DB: UPDATE APPROUVEE
        DS->>ES: emailDecision(ORG, APPROUVEE)
        note over DS: la **licence** n'est délivrée<br/>qu'après paiement (diagramme 13)
    end
    end
```

---

## 13. Abonnement en espèces, déblocage et renouvellement

Aucun paiement en ligne : l'organisateur paie **physiquement** l'admin, qui saisit le
paiement dans la console. Le statut d'abonnement pilote la licence, qui pilote le déblocage.

```mermaid
sequenceDiagram
    autonumber
    actor ORG as Organisateur
    actor ADM as Admin
    participant FE as SPA React<br/>(console admin)
    participant AS as AbonnementService<br/>feature/abonnement à venir
    participant LS as LicenceService<br/>feature/licence à venir
    participant BR as HiveMQ Cloud
    participant M as Firmware master<br/>(réception licence)
    participant ES as EmailService
    participant DB as PostgreSQL

    rect rgb(255, 245, 235)
    note over ORG,DB: 1 — Activation initiale (paiement en espèces)
    ORG->>ADM: paie l'abonnement en main propre
    ADM->>FE: POST /api/admin/abonnements<br/>{utilisateurId, dureeMois, montant, reference}
    FE->>AS: enregistrerPaiement(...)
    AS->>DB: INSERT abonnements (ACTIF, debut, fin, moyen=ESPECES, traitePar)
    AS->>DB: journal d'audit (preuve de créance)
    AS->>LS: emettreLicence(utilisateur, duree)
    LS->>DB: INSERT licenses (code SMC-…, ACTIVEE, expireLe)
    LS->>BR: PUBLISH seaside/config/{MAC} {licence}
    BR->>M: injection de la nouvelle licence
    M-->>LS: acquitté (licenseValid=true)
    AS->>ES: emailActivation(ORG)
    ES-->>ORG: "Votre compte est actif — bienvenue"
    note over FE: le dashboard se déverrouille :<br/>les routes ORGANISATEUR exigent<br/>abonnement ACTIF + licence ACTIVEE
    end

    rect rgb(245, 245, 255)
    note over ORG,DB: 2 — Renouvellement
    loop job quotidien
        AS->>DB: abonnements où fin < now + 7 j ?
        alt bientôt expiré
            AS->>ES: emailRappel(ORG, "renouvelez avant le …")
        end
    end
    ORG->>ADM: paie le renouvellement (espèces)
    ADM->>AS: POST /api/admin/abonnements (nouvelle période)
    AS->>DB: UPDATE ou INSERT nouvel abonnement ACTIF
    AS->>LS: genererNouvelleLicence(ORG) — module SMC-…
    LS->>BR: PUBLISH seaside/config/{MAC}
    BR->>M: nouvelle licence
    note over AS: si impayé à fin + 7 j de grâce :<br/>ABONNEMENT SUSPENDU → révocation<br/>de la licence → les entrées du module<br/>redeviennent bloquées (cf. diagramme 8)
    end
```

---

## 14. Publication d'événement — soumission et modération admin

Décision D4 : **aucun événement visible sans validation**. Workflow ajouté à l'entité
`Evenement` : `BROUILLON → SOUMISE → PUBLIEE ↔ MASQUEE`.

```mermaid
sequenceDiagram
    autonumber
    actor ORG as Organisateur
    participant FE as SPA React
    participant EC as EvenementController<br/>feature/evenement/web
    participant PS as PublicationService<br/>feature/evenement à venir
    actor ADM as Admin (console)
    participant PUB as Page publique<br/>/evenements (site vitrine)
    participant ES as EmailService
    participant DB as PostgreSQL

    rect rgb(235, 245, 255)
    note over ORG,DB: 1 — Création et soumission (organisateur actif uniquement)
    ORG->>FE: crée l'événement + catégories (BROUILLON)
    FE->>EC: POST /api/evenements (JWT ORGANISATEUR)
    EC->>EC: vérifie abonnement ACTIF + licence
    EC->>DB: INSERT evenements statut=BROUILLON
    note over DB: BROUILLON : invisible du public,<br/>visible de l'organisateur et de l'admin
    ORG->>FE: "Soumettre à validation"
    FE->>EC: POST /api/evenements/{id}/soumettre
    EC->>DB: UPDATE SOUMISE
    EC->>ES: notifierSoumission(ADM)
    end

    rect rgb(255, 245, 235)
    note over ADM,DB: 2 — Modération
    ADM->>FE: console : GET /api/admin/evenements?statut=SOUMISE
    FE->>PS: liste
    PS-->>ADM: événements à modérer
    alt Accepter
        ADM->>PS: POST /api/admin/evenements/{id}/accepter
        PS->>DB: UPDATE PUBLIEE + journal (adminId, horodatage)
        PS->>ES: emailPublication(ORG, "votre événement est en ligne")
    else Masquer (déjà publié) / Refuser
        ADM->>PS: POST /api/admin/evenements/{id}/masquer {motif}
        PS->>DB: UPDATE MASQUEE + motif + journal
        PS->>ES: emailPublication(ORG, MASQUEE, motif)
    else Ignorer
        note over PS: remise en file, aucun changement
    end
    end

    rect rgb(235, 255, 240)
    note over PUB,DB: 3 — Page publique type e-commerce
    PUB->>EC: GET /api/public/evenements?ville=&date=&categorie=
    EC->>DB: SELECT evenements WHERE statut=PUBLIEE
    EC-->>PUB: catalogue paginé (nom, date, lieu, catégories, prix)
    PUB-->>PUB: fiche événement + formulaire de réservation (diagramme 15)
    note over EC: MASQUEE disparaît immédiatement du<br/>catalogue — pas de cache public
    end
```

---

## 15. Réservation visiteur — intention non engageante puis paiement au guichet

Décision D5 : la réservation **n'engage la place qu'après paiement**. Comme il n'y a
pas de paiement en ligne, le paiement se fait **au guichet en espèces** ; l'organisateur
(ou son point de vente) confirme, et c'est là que la capacité est engagée.

```mermaid
sequenceDiagram
    autonumber
    actor V as Visiteur
    participant PUB as Page publique<br/>(sans compte)
    participant RC as ReservationController<br/>feature/ticketing à venir
    actor ORG as Organisateur
    participant M as Firmware module<br/>(check-in)
    participant DB as PostgreSQL
    participant ES as EmailService

    rect rgb(235, 255, 240)
    note over V,DB: 1 — Intention de réservation (gratuite, non engageante)
    V->>PUB: fiche événement → "Réserver N places"
    PUB->>RC: POST /api/public/reservations<br/>{evenementId, categorieId, quantite, email}
    RC->>DB: INSERT reservations (EN_ATTENTE, expireLe = now + 15 min)
    note over RC: **la capacité n'est PAS décrémentée**<br/>— l'intention n'occupe pas le stock
    RC->>ES: emailConfirmation(EN_ATTENTE, "réglez au guichet")
    RC-->>V: 200 {codeReservation, expireLe}
    loop job minuté
        RC->>DB: EN_ATTENTE dont expireLe dépassé → ANNULEE
    end
    end

    rect rgb(255, 245, 235)
    note over V,DB: 2 — Paiement au guichet → engagement de la place
    V->>ORG: se présente au guichet, paie en espèces
    ORG->>RC: POST /api/reservations/{id}/confirmer-paiement<br/>{montant, moyen=ESPECES} (JWT ORGANISATEUR)
    RC->>DB: transaction atomique :
    RC->>DB: SELECT capacite − COUNT(payees) FOR UPDATE
    alt capacité restante ≥ quantite
        RC->>DB: UPDATE PAYEE (preuve de créance, journalisée)
        note over DB: **c'est ici que la place est engagée**
        RC->>ES: emailBillet(V, codeQR)
    else capacité insuffisante
        RC-->>ORG: 409 "Capacité épuisée — remboursez le visiteur"
        note over RC: aucun débit de capacité :<br/>la survente est impossible car rien<br/>n'était réservé avant le paiement
    end
    end

    rect rgb(235, 245, 255)
    note over V,M: 3 — Entrée sur place (chaîne du diagramme 3)
    V->>M: scan du codeQR à l'entrée
    M->>M: vérifie licence (diagramme 8)
    M->>DB: (via MQTT) Entree — check-in de la réservation
    note over M: seul le **paiement** a engagé la place :<br/>les EN_ATTENTE expirées ne bloquent personne
    end
```

---

## Référence : topics et payloads firmware (rappeleur)

| Topic | Sens | Cadence | Payload résumé |
|---|---|---|---|
| `seaside/telemetry/master` | master → broker | 5 s | `{timestamp, battery, licence, total, current, ip, connected, master{…}, modules[], modulesCount, masterCount, slavesTotal}` |
| `seaside/entrees/{licence}` | master → broker | par passage | `{mac, moduleId, role, count, total, battery, licence, timestamp, connected}` — **mac = celle du master**, toujours |
| `seaside/telemetry/slave/{moduleId}` | master → broker | par paquet COUNT slave | `{slaveId:"Slave-1", mac, count, seuil, active}` — count absolu, **sans battery** |
| `seaside/alerte` | master → broker | sur événement | `{mac, moduleId, alerte:"Erreur configuration: seuil hors limites (150 cm)"}` |
| `seaside/rapport/visuel` | master → broker | sur commande | payload complet type `/api/status` étendu |
| `seaside/config/{MAC}` et `{MAC sans ':'}` | broker → master | à la demande | `{commande|command}` ou `{seuil}` |

Conventions firmware : `count` = compteur **local** ; `total` = master + différences slaves ;
`current` = personnes présentes ; `seuil` défaut 80 ; `timestamp` = **uptime en ms**, pas une date.

---

## ⚠ Conformité firmware ↔ backend — écarts constatés

Réponse à « est-ce que la doc firmware suffit pour le projet ? » : **presque** — le cœur MQTT
 correspond, mais **2 écarts bloquants** et 4 points de vigilance doivent être tranchés.

| # | Sujet | Firmware envoie… | Backend attend / fait… | Verdict |
|---|---|---|---|---|
| 1 | `mac` dans `telemetry/master` | pas de champ `mac` **en top-level** (il est dans `master{}` et `modules[]`) | `traiterTelemetrieMaster` fait `data.get("mac")` puis **return si null** | 🔴 **BLOQUANT** — la télémétrie master entière est rejetée. Soit le firmware ajoute `"mac"` top-level, soit le backend lit `data["master"]["mac"]` |
| 2 | Taille MQTT | PubSubClient limite à **256 octets**, le payload telemetry dépasse dès 1 slave | le backend s'attend à recevoir le heartbeat 5 s | 🔴 **BLOQUANT côté firmware** — les messages sont **jetés silencieusement** sans `MQTT_MAX_PACKET_SIZE` dans `platformio.ini` (le PDF lui-même le signale) |
| 3 | `battery` dans `telemetry/slave` | absent (et `battery` slave toujours 100 factice) | backend lit `battery` → null toléré, jamais mis à jour | 🟡 mineur — batterie slave inutilisable, le doc firmware le confirme |
| 4 | `active` slave | passe à `true` à la 1re réception, **jamais remis à false** | l'alerte `SLAVE_DECONNECTE` se déclenche sur `active=false` | 🟡 **l'alerte ne se déclenchera jamais** — le backend doit détecter l'absence de télémétrie (ex. > 30 s) |
| 5 | Champ du message d'alerte | `{mac, moduleId, alerte:"Erreur configuration: seuil hors limites (150 cm)"}` | backend lit `data.get("alerte")` | ✅ compatible — champ `alerte` confirmé dans la doc firmware |
| 6 | `default` sans licence | topic `seaside/entrees/default` | lookup `findFirstByLicence("default")`, fallback `findFirstByCouleur("default")` | 🟡 fragile — la catégorie ne peut pas avoir la couleur « default » ; définir la règle |
| 7 | Horodatage | `timestamp` = uptime ms depuis boot | backend utilise `LocalDateTime.now()` | ✅ sans effet, mais ne jamais interpréter comme une date |
| 8 | QoS | PubSubClient publie en QoS 0 par défaut | backend s'abonne en QoS 1 | ✅ le flux réel sera QoS 0 — acceptable, noter la perte possible |
| 9 | `seaside/rapport/visuel` | publié sur commande | abonnement `seaside/telemetry/#` et `entrees/#` : **non couvert** | ✅ volontaire — le rapport local n'est pas consommé par le backend |
| 10 | Licences | générées **par le module** via son API locale | `feature/licence` côté backend : entité seule, rien d'implémenté | 🔵 à arbitrer : qui émet les licences, le module ou le cloud ? |
