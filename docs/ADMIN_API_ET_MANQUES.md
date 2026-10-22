# Seaside — API admin livrée, manques frontend & manques firmware

> Statut : **implémenté et compilé** (`./mvnw compile` → BUILD SUCCESS, 22/10/2026).
> Toutes les routes de ce document sont sous le context-path **`/api`**
> (`server.servlet.context-path=/api`) et exigent l'en-tête `Authorization: Bearer <access_token>`.
> Les routes `/api/admin/**` sont réservées à **`ROLE_ADMIN`** (règle unique dans `SecurityConfig`).
>
> Documents liés : [FRONTEND_BRIEFING.md](FRONTEND_BRIEFING.md) · [INSCRIPTION_ORGANISATEUR_RECOMMANDATIONS.md](INSCRIPTION_ORGANISATEUR_RECOMMANDATIONS.md) · [SEQUENCE_DIAGRAMS.md](SEQUENCE_DIAGRAMS.md)

---

## 1. Ce qui vient d'être livré

| Domaine | Endpoints | Fichiers |
|---|---|---|
| Dashboard global | `GET /admin/dashboard` | `feature/stats/**` |
| Demandes organisateur (dépôt + suivi) | `POST /organisateur/demande`, `GET /organisateur/demande` | `feature/organisateur/web/OrganisateurController.java` |
| Demandes organisateur (console admin) | `GET /admin/demandes`, `POST /admin/demandes/{id}/approuver`, `/refuser`, `/ignorer` | `feature/organisateur/web/AdminDemandeController.java` |
| Licences | `GET /admin/licences`, `POST /admin/licences`, `PATCH /admin/licences/{id}/statut` | `feature/licence/web/AdminLicenceController.java` |
| Supervision événements | `GET /admin/evenements`, `GET /admin/evenements/{id}`, `PATCH /admin/evenements/{id}/activer|desactiver` | `feature/evenement/web/AdminEvenementController.java` |
| Utilisateurs (déjà existant) | `GET /admin/utilisateurs`, `GET /{id}`, `PUT /{id}`, `PATCH /{id}/activer`, `/desactiver`, `/role`, `/mot-de-passe`, `DELETE /{id}` | `feature/utilisateur/web/UtilisateurController.java` |
| Emails de décision | approbation / refus motivé / nouvelle demande → admin | `feature/auth/service/EmailService.java` |
| Sécurité | politique mot de passe, OTP durci, verrouillage login, rate limiting, Swagger off en prod, 500 sanitisé | `shared/security/**`, `feature/auth/service/**` |

Le nouveau rôle **`ORGANISATEUR`** existe dans `RoleUsers` (ADMIN, ORGANISATEUR, CLIENT, COMPTOIRE).

### 1.1 Note de déploiement — migration `V2__role_organisateur.sql`

La table `utilisateurs` a été créée par Hibernate avec une contrainte
`CHECK (role IN ('ADMIN','CLIENT','COMPTOIRE'))`. `ddl-auto=update` **ne modifie jamais une contrainte
existante** : sans migration, toute approbation de demande échouait en base (`violates check constraint
"utilisateurs_role_check"`) — défaut constaté en test et corrigé.

La migration Flyway [`V2__role_organisateur.sql`](../backend/src/main/resources/db/migration/V2__role_organisateur.sql)
remplace le `CHECK` en y ajoutant `ORGANISATEUR`. Elle est **défensive** : si la table n'existe pas encore
(base neuve), elle ne fait rien et Hibernate génère directement la bonne contrainte.

Sur une base déjà existante, Flyway bascule à la version 2 au démarrage (log attendu :
`Migrating schema "public" to version "2 - role organisateur"`).

---

## 2. Contrat de la console admin (prêt à consommer)

### 2.1 Dashboard KPI — `GET /api/admin/dashboard`

```json
{
  "totalUtilisateurs": 42, "utilisateursActifs": 40,
  "totalAdmins": 1, "totalOrganisateurs": 6, "totalClients": 34, "totalComptoires": 1,
  "demandesEnAttente": 3,
  "totalEvenements": 12, "evenementsActifs": 5,
  "totalPersonnes": 12840, "totalRevenus": 64200000.00,
  "totalLicences": 9, "licencesActives": 7,
  "totalMasters": 9, "mastersConnectes": 4, "totalSlaves": 27
}
```

`totalRevenus` est un `BigDecimal` (sérialisé en nombre décimal, pas en chaîne).
**Cartes suggérées** : utilisateurs / organisateurs · demandes en attente (badge rouge si > 0) ·
événements actifs · personnes & revenus · licences actives · parc ESP32 connecté.

### 2.2 Demandes d'organisateur

**Côté demandeur (CLIENT connecté) :**

| Méthode | Route | Corps | Réponse |
|---|---|---|---|
| `POST` | `/organisateur/demande` | `{ "message": "…" }` (facultatif, ≤ 2000 car.) | `201` + `DemandeOrganisateurResponse` |
| `GET` | `/organisateur/demande` | — | `200` + dernière demande |

Refus métier (400) : « Une demande est déjà en attente de traitement », « Votre demande a déjà été approuvée »,
« Vous êtes déjà organisateur ».

**Côté admin :**

| Méthode | Route | Corps | Effet |
|---|---|---|---|
| `GET` | `/admin/demandes?statut=EN_ATTENTE` | — (filtre facultatif) | liste triée par date décroissante |
| `POST` | `/admin/demandes/{id}/approuver` | — | statut `APPROUVEE` + **rôle `ORGANISATEUR`** + email |
| `POST` | `/admin/demandes/{id}/refuser` | `{ "motif": "…" }` (**obligatoire**, ≤ 1000 car.) | statut `REFUSEE` + email motivé |
| `POST` | `/admin/demandes/{id}/ignorer` | — | statut `IGNOREE` (re-traitable ensuite) |

```json
{
  "id": "3f1c…", "utilisateurId": "9ab2…",
  "nomUtilisateur": "Rado", "email": "rado@mail.mg",
  "statut": "EN_ATTENTE",
  "message": "J'organise des concerts depuis 3 ans",
  "motif": null,
  "dateDemande": "2026-10-22T09:14:03",
  "dateTraitement": null,
  "traiteParId": null
}
```

Statuts : `EN_ATTENTE`, `APPROUVEE`, `REFUSEE`, `IGNOREE`, `EXPIREE` (jamais produit automatiquement — voir §4.1).
Une demande `REFUSEE` **ne peut plus** être re-traitée : le demandeur doit en déposer une nouvelle.

> ℹ️ **Prise d'effet du rôle** : les autorisations sont recalculées **depuis la BDD à chaque requête**
> (`JwtFilter` recharge l'utilisateur), donc l'approbation est effective immédiatement, même avec
> l'ancien access token. En revanche le `role` que le front a mémorisé au login devient faux →
> récupérer le rôle courant via `GET /api/utilisateurs/moi` (champ `role`), ou `POST /api/auth/refresh`
> (qui relit la BDD), puis mettre à jour les guards d'interface.

### 2.3 Licences

| Méthode | Route | Corps |
|---|---|---|
| `GET` | `/admin/licences` | — |
| `POST` | `/admin/licences` | `{ "slaveId": "uuid", "dateExpiration": "2027-06-30" }` (date facultative = illimitée) |
| `PATCH` | `/admin/licences/{id}/statut` | `{ "statut": "ACTIVE" \| "BLOCKED" \| "EXPIRED" }` |

```json
{
  "id": "5c7d…", "licenseKey": "SEA-4F2A9C11D8B37E60",
  "statut": "ACTIVE",
  "activationDate": "2026-10-22", "expirationDate": "2027-06-30",
  "slaveId": "b1e0…", "nomSlave": "Portique entrée VIP"
}
```

Règles métier : une licence au maximum par module slave (`Ce module possède déjà une licence`),
clé générée serveur (`SEA-` + 16 caractères), `activationDate` remplie à la création et à la première activation.
**L'écran « délivrer une licence » a besoin de la liste des slaves** → `GET /api/esp32/masters` puis
`GET /api/esp32/masters/{id}/slaves` (pas de route « slaves sans licence », voir §4.2).

> 🚨 La licence n'est **pas encore vérifiée par le firmware** : elle n'est reliée qu'à `Esp32Slave`,
> alors que le firmware s'identifie avec `Esp32Master.licence` (chaîne libre). Voir §5.3.

### 2.4 Supervision des événements

| Méthode | Route | Réponse |
|---|---|---|
| `GET` | `/admin/evenements` | tous les événements, tous propriétaires confondus |
| `GET` | `/admin/evenements/{id}` | détail |
| `PATCH` | `/admin/evenements/{id}/activer` | réactivation (y compris événement d'un tiers) |
| `PATCH` | `/admin/evenements/{id}/desactiver` | suspension (fraude, litige…) |

```json
{
  "id": "0d9a…", "nom": "Concert Tamatave", "dateEvenement": "2026-11-14",
  "lieu": "Stade", "capaciteMaximale": 5000, "estActif": true,
  "dateCreation": "2026-10-01T10:12:00",
  "totalPersonnes": 1284, "totalRevenus": 6420000.00, "nbCategories": 3,
  "proprietaireId": "c0f1…", "proprietaireNomUtilisateur": "Rado",
  "proprietaireEmail": "rado@mail.mg"
}
```

> Les routes propriétaire `PATCH /evenements/{id}/activer|desactiver` restent réservées au propriétaire
> (`trouverMonEvenement`) : l'admin doit passer par `/admin/evenements/...`.

### 2.5 Écrans admin à construire (périmètre couvert par l'API)

1. **Dashboard** — cartes KPI (`/admin/dashboard`).
2. **File d'approbation** — tableau + filtres par statut, panneau latéral de détail, boutons Approuver / Refuser (modale motif) / Ignorer, badge du nombre `demandesEnAttente`.
3. **Licences** — tableau (clé, statut, dates, slave), formulaire de délivrance (sélection master → slave), actions Activer / Bloquer / Expirer.
4. **Événements** — tableau global avec propriétaire, revenus, comptage, actions Activer / Suspendre.
5. **Utilisateurs** — liste, activation/désactivation, changement de rôle, reset mot de passe forcé, suppression.
6. **Suivi demandeur** (côté client) — bouton « Devenir organisateur » + écran de statut (statut, motif de refus, date).

---

## 3. Sécurité : ce que le frontend doit gérer maintenant

| Comportement | Détail | Impact UI |
|---|---|---|
| **429 Too Many Requests** | fenêtre glissante par IP+route : `/auth/login` 10/min, `/auth/register` `/auth/verify-email` `/auth/confirm-otp` `/auth/forgot-password` `/auth/reset-password` 5/min, `/public/reservations` 30/min | corps `{code:429, message, timestamp}` → toast + bouton désactivé pendant 60 s |
| **Mot de passe ≥ 12 caractères** | `PasswordPolicy` sur inscription, reset et changement ; refuse les mots de passe courants et ceux contenant l'email | afficher la longueur minimale **avant** soumission, pas seulement l'erreur 400 |
| **OTP** | valable **5 min**, **5 tentatives** max, renvoi possible après **60 s** | compte à rebours 5:00, compteur de tentatives (messages serveur : « il vous reste N tentative(s) »), bouton renvoyer grisé 60 s |
| **Verrouillage login** | 5 échecs consécutifs sur un email → blocage **15 min** | message 400 contient le délai restant → bannière, pas d'alerte d'erreur brute |
| **500 génériques** | plus aucune stacktrace ni nom de classe dans la réponse | message neutre + « réessayer », jamais parser `message` comme une cause technique |
| **Swagger désactivé en prod** | `springdoc.*.enabled=false` (profil `prod`) | ne pas exposer de lien `/swagger-ui.html` en production |
| **Nouveau rôle** | `ORGANISATEUR` dans l'enum et dans le claim `role` | guards de routes, libellés, filtre utilisateurs |
| **Rôle recalculé à chaque requête** | une promotion/révocation s'applique sans reconnexion | rafraîchir le rôle stocké côté client (`GET /utilisateurs/moi`) après une action admin |
| **Formats d'erreur** | 400 validation = `{champ: message}` ; 400/401/404/405/429/500 = `{code, message, timestamp}` | normaliser dans un seul composant d'erreur |

---

## 4. Manques backend (à planifier) — côté frontend

### 4.1 Priorité P0

1. **Pagination + recherche** : `GET /admin/demandes`, `/admin/licences`, `/admin/evenements`, `/admin/utilisateurs` renvoient **toute** la table. Prévoir `?page=&size=&sort=` (Spring `Page`) avant la mise en prod.
2. **Séries temporelles pour graphiques** : rien pour tracer « entrées par heure/jour ». `Entree.enregistreLe` existe → ajouter `GET /admin/stats/series?from=&to=&granularite=HEURE|JOUR`.
3. **Historique des alertes** : les alertes ne partent qu'en WebSocket (`/topic/alerte/{evenementId}`) et sont perdues. Ajouter table `alertes` + `GET /admin/alertes?evenementId=&depuis=`.
4. **Expiration des demandes** : `StatutDemande.EXPIREE` est défini mais aucun job ne l'applique (7 jours annoncés). À brancher (`@Scheduled`).

### 4.2 Priorité P1

5. **Licences** : `GET /admin/licences/{id}`, filtre `?statut=`, suppression/révocation définitive, et surtout `GET /admin/slaves-sans-licence` pour alimenter le formulaire de délivrance sans deux appels.
6. **Export CSV** des entrées, événements et utilisateurs (attendu par le CEO pour le reporting).
7. **Création d'événement pour un organisateur** : `POST /evenements` crée toujours sous le compte courant ; pas de route « événement d'un tiers » côté admin.
8. **Journal d'audit** : seul `DemandeOrganisateur` conserve `traiteParId`/`dateTraitement`. Les actions admin sur licences, événements et utilisateurs ne sont pas tracées.
9. **Rôle `COMPTOIRE`** : aucun endpoint (scan de billet, vente au guichet) → l'écran comptoir est impossible aujourd'hui.

### 4.3 Priorité P2

10. Notifications/annonces email de masse.
11. `GET /admin/utilisateurs?email=` (support) et renvoi d'email de vérification.
12. Endpoint public de validation de licence pour le firmware (§5.3).

---

## 5. Firmware — contrat réel et manques

### 5.1 Chaîne MQTT réellement branchée

`MqttConfig` → `SeasideMqttRouter` → `SeasideMqttSubscriber` → `SeasideMqttService`.

> ⚠️ Les classes `MqttSubscriber` et `MqttTelemetrySubscriber` (anciennes) ne sont **plus** câblées mais restent
> compilées : elles attendent d'autres noms de champs (`masterMac`, `comptage`, `code`). **Le firmware doit
> implémenter le tableau ci-dessous, pas l'ancien contrat.**

Abonnement serveur (`mqtt.topic`) : `seaside/entrees/#`, `seaside/telemetry/#`, `seaside/alerte` — QoS 1.

| Sens | Topic | Payload attendu | Traitement backend |
|---|---|---|---|
| Firmware → API | `seaside/telemetry/master` | `{"mac":"AA:BB:…","moduleId":"…","battery":87,"licence":"SEA-…","connected":true}` | met à jour le master (retrouvé par `mac`, sinon par `licence`), `lastSeen`, alerte batterie ≤ 20 % |
| Firmware → API | `seaside/telemetry/slave/{n}` | `{"mac":"AA:BB:…","battery":64,"seuil":90,"active":true}` | slave retrouvé **par `mac` du payload** (le `{n}` du topic est ignoré), historique batterie, alerte `SLAVE_DECONNECTE` / `BATTERIE_SLAVE_FAIBLE` |
| Firmware → API | `seaside/entrees/{licence}` | payload **ignoré** — **1 message = +1 personne** | retrouve le master par `licence` (repli : `Categorie.couleur`), incrémente la catégorie, pousse `WS /topic/comptage/{evenementId}` |
| Firmware → API | `seaside/alerte` | `{"mac":"AA:BB:…","alerte":"texte libre"}` | pousse `WS /topic/alerte/{evenementId}` (code `FIRMWARE`, niveau `WARN`) — ignorée si le master n'a pas de catégorie |
| API → Firmware | `seaside/config/{mac}` | `{"seuil":120}` | publié par `POST /api/esp32/masters/{id}/config/envoyer` (QoS 1, **non retenu**) |

Types : `battery`, `seuil` = **nombres**, `active`/`connected` = **booléens** (`"true"` sous forme de chaîne
reste toléré côté backend, mais le JSON natif est attendu).

### 5.2 Manques firmware (à implémenter côté ESP32)

1. **Heartbeat master** : publier `seaside/telemetry/master` au démarrage **puis toutes les 60 s** (`licence`, `battery`, `connected:true`). Sans cela `mastersConnectes` reste faux dans le dashboard.
2. **Last Will & Testament** : enregistrer un will sur `seaside/telemetry/master` avec `connected:false` pour signaler une coupure brutale (le backend n'a **aucun** timeout : `master.connected` ne repasse jamais à `false` tout seul — voir §5.4).
3. **Un message par personne** sur `seaside/entrees/{licence}` : le backend ne lit pas de champ `comptage`. Tout comptage par lot est perdu → ne pas grouper.
4. **S'abonner à `seaside/config/{mac}`** et appliquer `seuil` (+ acquittement) ; **redemander la config au boot** (elle n'est pas retenue par le broker).
5. **File d'attente hors-ligne** (LittleFS/SPIFFS) : stocker les impulsions pendant une coupure Wi-Fi et les rejouer à la reconnexion, avec QoS 1 et `cleanSession=false` ou buffer applicatif.
6. **Identité stable du slave** : le slave est identifié par sa **MAC** dans le payload ; `macAddress` doit être unique et correspondre à celle enregistrée via `POST /api/esp32/masters/{id}/slaves`. Le numéro du topic doit égaler `slaveNumber`.
7. **Alertes locales** : envoyer `seaside/alerte` pour les incidents matériels (capteur HS, surchauffe, batterie critique) ; les seuils de batterie sont déjà calculés côté serveur.
8. **Reconnexion** : backoff exponentiel + `automaticReconnect` côté client, jamais de boucle de connexion à 1 s.
9. **Sécurité** : un seul compte MQTT partagé (`Fortico`) → n'importe qui connaissant les identifiants peut injecter des entrées. Prévoir des identifiants par device (ou certificats) avant la mise en service réelle.
10. **Pas de mise à jour OTA** : aucun endpoint firmware d'update. À cadrer (URL signée + version dans `seaside/telemetry/master`).

### 5.3 Licence côté firmware — fonction inexistante

Aucun moyen pour un module de **valider sa licence** :

- `License` (table `license`) est liée à `Esp32Slave.id_slave` ;
- le firmware, lui, s'annonce avec `Esp32Master.licence` (chaîne `unique` de 50 car.) ;
- les deux modèles ne se parlent pas, et `LicenceService` n'expose aucune route publique.

**Décision à prendre (bloquant pour l'exploitation commerciale)** :

- **Option A (rapide)** — considérer `Esp32Master.licence` comme la clé de licence : l'admin la saisit dans
  `POST /api/esp32/masters`, le firmware l'envoie dans la télémétrie. La table `license` sert alors
  uniquement d'inventaire/date d'expiration.
- **Option B (propre, recommandée)** — réconcilier les deux modèles puis exposer
  `POST /api/esp32/licence/valider` (public, rate-limité) : `{ "licenceKey": "SEA-…", "mac": "AA:BB:…" }`
  → `{ "statut": "ACTIVE", "expirationDate": "2027-06-30" }`. Le firmware l'appelle au boot et passe en
  mode dégradé (aucun comptage, LED rouge, alerte `seaside/alerte`) si `BLOCKED`/`EXPIRED`.

### 5.4 Dette backend qui impacte directement le firmware

| # | Problème | Conséquence |
|---|---|---|
| 1 | Deux contrats MQTT coexistants (anciennes classes non supprimées) | risque d'implémenter le mauvais payload |
| 2 | `seaside/entrees/{licence}` ignore le payload (pas de `comptage`, pas d'horodatage) | pas de lot, pas de rejeu daté |
| 3 | Aucune déduplication (pas d'`id` de message) | un message rejoué après reconnexion compte **deux** personnes |
| 4 | Aucun timeout de heartbeat | `connected` reste `true` après une coupure ; dashboard faux |
| 5 | `seaside/config/{mac}` publié sans `retained` | un module hors-ligne rate la config |
| 6 | Entrées horodatées uniquement à la réception (`@PrePersist`) | un rejeu après coupure fausse les stats par heure |
| 7 | Aucune validation de licence (voir §5.3) | pas de monétisation contrôlable |

---

## 6. Checklist de recette

**Frontend (admin)** — se connecter avec un compte `ADMIN` :

- [ ] dashboard : les 16 KPI s'affichent, `demandesEnAttente` alimente le badge
- [ ] déposer une demande avec un compte `CLIENT`, la voir apparaître dans `/admin/demandes`
- [ ] approuver → l'utilisateur passe `ORGANISATEUR` (visible via `GET /utilisateurs/moi`), email reçu
- [ ] refuser sans motif → 400 `{motif: "Le motif de refus est obligatoire"}`
- [ ] délivrer une licence → clé `SEA-…`, deuxième tentative sur le même slave → 400
- [ ] bloquer la licence puis la réactiver
- [ ] suspendre un événement d'un autre organisateur → `estActif:false`
- [ ] 11 connexions ratées sur `/auth/login` → 429
- [ ] mot de passe de 8 caractères à l'inscription → 400 avec le message de longueur

**Firmware** (broker HiveMQ, un master enregistré avec `licence`) :

- [ ] `mosquitto_pub -t seaside/entrees/<licence> -m '{}'` → +1 dans `WS /topic/comptage/{evenementId}`
- [ ] `mosquitto_pub -t seaside/telemetry/master -m '{"mac":"…","battery":85,"licence":"…","connected":true}'` → `mastersConnectes +1`
- [ ] `mosquitto_pub -t seaside/telemetry/slave/1 -m '{"mac":"<mac slave>","battery":10,"active":true}'` → alerte batterie
- [ ] commande `POST /api/esp32/masters/{id}/config/envoyer` → message reçu sur `seaside/config/{mac}`
- [ ] master non enregistré → log `Master non enregistré`, aucun impact en base

---

## 7. Vérifications effectuées (22/10/2026)

Recette réelle sur PostgreSQL local (backend démarré, JWT `ADMIN`), puis données de test supprimées :

| Vérification | Résultat |
|---|---|
| `./mvnw compile` / `./mvnw test` | BUILD SUCCESS, tests OK |
| `GET /admin/dashboard` (token ADMIN) | `200` + 16 KPI |
| `GET /admin/demandes?statut=EN_ATTENTE`, `/licences`, `/evenements` | `200` |
| `POST /admin/demandes/{id}/refuser` sans motif | `400 {motif: "Le motif de refus est obligatoire"}` |
| `POST /admin/demandes/{id}/refuser` avec motif | `200`, statut `REFUSEE`, `dateTraitement` + `traiteParId` remplis |
| re-traitement d'une demande `REFUSEE` | `400 « Cette demande a déjà été traitée (REFUSEE) »` |
| `POST /admin/demandes/{id}/approuver` | `200`, rôle `ORGANISATEUR` en base — **échouait avant la migration V2** (contrainte `CHECK`) |
| `POST /admin/licences` | `201`, clé `SEA-…`, `activationDate` = jour même |
| deuxième licence sur le même slave | `400 « Ce module possède déjà une licence »` |
| `PATCH /admin/licences/{id}/statut` → `BLOCKED` | `200`, statut persisté |
| `GET/PATCH /admin/evenements/{id}/activer|desactiver` avec un événement réel | `200`, `estActif` inversé |
| `PATCH /admin/evenements/{id}` inexistant | `400 « Événement introuvable »` |
| `GET /admin/dashboard` sans token | `403` |
| changement de rôle pris en compte sans reconnexion | oui (autorités relues en base à chaque requête) |
