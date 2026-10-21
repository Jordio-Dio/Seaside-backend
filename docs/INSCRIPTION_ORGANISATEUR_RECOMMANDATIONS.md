# Onboarding Organisateur — Scénarios, recommandations et points de vigilance

> **Contexte** : SmartCount / Seaside — module de comptage et billetterie pour événements.
> **Objet** : répondre à la question « qui doit pouvoir inscrire un organisateur ? » et sécuriser le parcours d'inscription.
> **Statut** : note de cadrage pour arbitrage CEO. Aucune décision implémentée à ce stade.

---

## 1. Synthèse exécutive

**Votre intuition est juste, et la situation actuelle est plus grave qu'un simple défaut de vérification.**

| # | Constat | Sévérité |
|---|---------|----------|
| 1 | **N'importe qui peut créer un compte ADMIN** — le champ `role` du formulaire d'inscription est accepté tel quel par `POST /auth/register`. Un simple `{"role": "ADMIN"}` donne un accès complet (la méthode de filtrage existante est commentée dans `AuthService`). | 🔴 **Bloquant** |
| 2 | Aucune validation humaine ni électronique avant activation : `inscrire()` crée l'utilisateur **actif** et lui délivre immédiatement access + refresh token. | 🔴 Bloquant |
| 3 | Aucun rate-limiting sur `/auth/register`, `/auth/confirm-otp`, `/auth/forgot-password`, `/auth/login` — les endpoints OTP (6 chiffres) sont brute-forceables. | 🔴 Bloquant |
| 4 | Faiblesse mot de passe : minimum **6 caractères** (attente : 12+ avec NIST SP 800-63B). | 🟠 |
| 5 | Pas de verrouillage de compte, pas de captcha, pas d'anti-énumération d'email. | 🟠 |
| 6 | Swagger/OpenAPI public **en production**, erreurs 500 qui exposent le nom de la classe d'exception. | 🟠 |
| 7 | Aucun statut d'attente, aucune demande d'approbation, aucune piste d'audit côté admin. | 🟠 (fonctionnel) |

**Recommandation en une phrase** : adopter le **scénario 3 (hybride)** — inscription libre en compte « organisateur en attente », activation du tableau de bord uniquement après approbation admin (et licence), avec expiration automatique des demandes non traitées — c'est aussi le modèle observé chez Weezevent et Shotgun.

---

## 1 bis. Décisions arrêtées (arbitrages CEO, 21 octobre 2026)

| # | Décision | Conséquence architecture |
|---|----------|--------------------------|
| D1 | **Acquisition en libre-service (self-serve)** + validation admin avant tout affichage public | Le scénario 3 est confirmé ; la porte « admin crée le compte » reste en backup interne, pas le canal principal |
| D2 | **Abonnement organisateur payé en espèces, en main propre à l'admin** (aucun paiement en ligne pour l'instant — l'intégration mobile money est **reportée**) ; l'admin saisit le paiement et **débloque** l'accès | L'admin enregistre le paiement (montant, période, référence) → `Abonnement.ACTIF` + licence activée ; pas d'agrégateur, pas de webhook, pas de réconciliation ; une API de paiement (Efaina/CinetPay/Orange) sera à intégrer au moment du passage à l'encaissement en ligne |
| D3 | **Renouvellement d'événement = nouvelle génération de licence**, effectuée via/par le module ESP32 | La génération de licence reste **autorisée par le backend** (l'abonnement payé → le backend émet ; le module reçoit/valide). Le firmware ne doit jamais être la source de vérité commerciale (il est clonable) |
| D4 | **Publication des événements sur une page publique type e-commerce**, avec **validation admin** (accepter / masquer) ; aucun événement affiché sans validation | Workflow de publication à ajouter à `Evenement` : `BROUILLON → SOUMISE → PUBLIEE ↔ MASQUEE` + journal d'audit + emails |
| D5 | **La réservation visiteur n'engage la place qu'après paiement** | Modèle « intention de réservation » non engageante + confirmation définitive au paiement ; **risque de survente à gérer** (capacité revérifiée à l'instant du paiement, quota de sécurité) — voir §5 |

### Points ouverts découlant de ces décisions (à trancher — voir aussi §7, questions 12–15)

1. **Délai de grâce** après absence de renouvellement d'abonnement avant suspension (7 j recommandés) et comportement de la licence pendant la grâce (gelée ou révoquée ?).
2. **Réservations payées comment ?** L'abonnement est en espèces, mais les **billets** des visiteurs : paiement au guichet en espèces (modèle actuel, engagement de la place au check-in/casier) ou futur paiement en ligne ? À trancher avant d'implémenter le module de réservation.
3. **Qui marque une réservation comme payée** si paiement hors ligne : l'organisateur au guichet, l'admin, ou automatiquement au check-in module ?

### Références d'agrégateurs mobile money (recherche du 21/10)

- **Efaina** — API unifiée MVola + Orange Money + Airtel Money, cible Madagascar : https://www.efaina.com/
- **CinetPay** — agrégateur 9 pays Afrique (dont Madagascar), liens de paiement + API : https://cinetpay.com/
- **Orange Money Web Payment API** (officielle, couvre Madagascar) : https://developer.orange.com/apis/om-webpay
- MVola (Telma) — API marchande via son programme partenaires (cf. GSMA Mobile Money API) : https://www.gsma.com/solutions-and-impact/connectivity-for-good/mobile-for-development/programme/mobile-for-development/mvola-madagascar-a-case-study-for-the-gsma-mobile-money-api-and-open-platforms/

> Critère de choix : **fiabilité des webhooks + API de statut consultable** (vous devrez faire de la réconciliation) et **possibilité de reversement vers un compte organisateur tiers** si la plateforme doit détenir les fonds.

## 2. Les 3 scénarios (vos notes) + le modèle marché

### Scénario 1 — Self-service avec validation admin
*Le client s'inscrit, attend la validation de l'admin + licence, puis accède au dashboard.*

| ✅ Avantages | ❌ Risques / limites |
|---|---|
| Zéro friction d'acquisition (SEO, bouche-à-oreille, growth) | Spam de comptes (bots, curieux, concurrents) |
| Standard SaaS moderne (Eventbrite, Weezevent) | File d'attente = friction business (SLA de validation à tenir) |
| L'admin garde la porte | Un compte « en attente » existe en base → surface d'attaque RGPD (données de personnes non-clientes) |

**Correctifs intégrés (vos notes) qui doivent absolument faire partie du design** :
- Bouton **« Ignorer / Refuser »** côté admin (motif obligatoire pour l'audit) ;
- **Expiration automatique** des demandes non traitées. *Ma recommandation : **7 jours** avec relance automatique à J-2, pas 2–3 jours* — 48 h est trop court pour un organisateur professionnel (week-end, vacances) et votre admin risque de perdre des clients légitimes ; 7 jours reste compatible avec la minimisation RGPD (voir §6).

### Scénenario 2 — Admin-provisionné
*L'admin crée le compte et donne la licence.*

| ✅ Avantages | ❌ Risques / limites |
|---|---|
| Contrôle total, zéro spam | **Frein commercial mortel** : pas d'autoserve = pas de croissance sans équipe commerciale |
| Vente humaine possible (devis, négociation) | Coût de gestion manuel qui explose avec le nombre de clients |
| Qualité des comptes garantie | Mauvaise expérience : l'organisateur ne peut rien « essayer » |

**Verdict** : acceptable uniquement en **première phase de lancement** (10–20 premiers clients conquis commercialement), pas comme modèle durable.

### Scénario 3 — Hybride (vos notes, point 3) ⭐ **RECOMMANDÉ**
**Les deux, séquencés :**

1. **Inscription libre** en ligne → le compte est créé avec le rôle `CLIENT` + un statut `EN_ATTENTE_VALIDATION` (jamais `ADMIN`, jamais actif côté licence) ;
2. **Pendant l'attente**, l'organisateur a un accès limité « bac à sable » : il peut **configurer** son événement, ses catégories, ses modules ESP32, mais **pas publier, pas compter en production** ;
3. **L'admin reçoit une notification email** + voit la demande dans une **console d'approbation** (liste, documents éventuels, boutons Approuver / Refuser avec motif) ;
4. À l'approbation : **licence activée** (entité `License` déjà présente, `LicenseStatus` existe) → l'organisateur reçoit un email et son dashboard s'active ;
5. Rejet ou expiration (7 j) : compte désactivé ou purgé, email motivé, réinscription possible après 30 j (anti-abus).

C'est exactement le compromis que tracent les acteurs du marché (voir §5) : **self-serve pour l'acquisition, contrôle humain pour l'activation**.

### Scénario 4 — Self-serve complet différé (modèle Eventbrite)
*L'organisateur fait tout seul ; la vérification n'intervient qu'au moment du paiement (KYC bancaire via Stripe Connect).*

**Intéressant si et seulement si vous monétisez des encaissements en ligne** (billeterie avec commission). À réserver à une phase 2 du produit — ne pas le mettre en place maintenant, mais **ne pas fermer la porte** (voir questions CEO, §7).

---

## 3. Workflow de validation recommandé (le « exactement quoi »)

### 3.1 Machine à états

```
NOUVEAUE_DEMANDE ──► EN_REVUE ──► APPROUVEE ──► LICENCE_ACTIVEE
       │                │
       │                ├────► REFUSEE (motif) ──► purge à J+30
       │                │
       └── expiration 7 j ──► EXPIREE (purge à J+30)
```

### 3.2 Nouveaux éléments de domaine

| Élément | Rôle |
|---|---|
| `DemandeOrganisateur` | Id, utilisateurId, statut, dateDemande, dateTraitement, traitePar (admin), motif, documents (URL S3 éventuel) |
| `License.status` (existe déjà) | `NON_DELIVREE / EN_ATTENTE / ACTIVEE / SUSPENDUE / EXPIREE` + `dateEmission`, `dateExpiration`, `emetteur` |
| Rôles | Ajouter `ORGANISATEUR` à l'enum `RoleUsers` (aujourd'hui : ADMIN, CLIENT, COMPTOIRE). `CLIENT` = visiteur/comptoir ; `ORGANISATEUR` = détenteur de licence activée |

### 3.3 Endpoints (nouveaux)

| Méthode | Route | Accès | Description |
|---|---|---|---|
| POST | `/organisateur/demande` | Authentifié | Déposer sa demande (après inscription) |
| GET | `/organisateur/demande` | Authentifié | Suivre sa demande (statut, motif) |
| GET | `/admin/demandes` | ADMIN | Liste paginée des demandes |
| POST | `/admin/demandes/{id}/approuver` | ADMIN | Approve + génère licence + email |
| POST | `/admin/demandes/{id}/refuser` | ADMIN | Rejet avec motif obligatoire + email |
| POST | `/admin/demandes/{id}/ignorer` | ADMIN | Mise de côté (remise en file) — votre bouton « ignorer » |
| — | Job `@Scheduled` | — | Expiration auto des demandes > 7 j + purge J+30 des refusées/expirées |

### 3.4 Sécurité — les verrous obligatoires (P0, indépendants du scénario retenu)

Ces 6 correctifs **doivent** être faits avant toute mise en production, quel que soit le scénario arbitré :

1. **Ne jamais lire le rôle dans la requête d'inscription** — supprimer `role` de `RegisterRequest`, forcer `CLIENT` côté serveur (le code actuel accepte `"role":"ADMIN"` du client — c'est une élévation de privilège totale). La méthode `getRole()` existante est commented-out : la réactiver (ou équivalent) ;
2. **Rate limiting** sur les endpoints d'auth : ex. 5 req/min/IP sur register, confirm-otp (le code OTP à 6 chiffres actuel est brute-forceable en < 1 h sans cela), forgot-password ; 10 req/min sur login + **verrouillage temporaire du compte** après 5 échecs (message générique « email ou mot de passe incorrect » pour éviter l'énumération) ;
3. **Mots de passe** : minimum 12 caractères (NIST SP 800-63B), interdiction des 10 000 mots les plus courants, indicateur de robustesse côté front ;
4. **OTP** : expiration 5 min, usage unique, tentative max 5, réenvoi limité (attente 60 s) — et ne jamais dire « compte inexistant » ;
5. **Production hygiene** : Swagger désactivé en prod (`springdoc` sur profil dev uniquement), messages d'erreur 500 sans classe d'exception (le `GlobalExceptionHandler` expose `ex.getClass().getSimpleName()`), `ErrorResponse` (la classe vide de `shared/dto`) à harmoniser ;
6. **WebSocket** : `/ws/**` et `/ws-raw` sont actuellement **totalement publics** — tout le monde peut suivre les compteurs live d'un événement. Ajouter une validation JWT sur la négociation SockJS/STOMP (ticket de connexion ou en-tête), scoping par `utilisateurId`/licence.

*(Les points MQTT/ESP32 (TLS, signature des modules, seed aléatoire) figurent au §6 — hors périmètre auth mais critiques.)*

---

## 4. Benchmark : comment le font les acteurs du marché (2025-2026)

| Plateforme | Inscription organisateur | Vérification | Encaissement / payout |
|---|---|---|---|
| **Eventbrite** | Self-serve total : créer un compte, créer et publier un événement sans invitation | KYC financier (Stripe) exigé **avant les payouts**, pas avant publication | Fonds tenus jusqu'à l'événement, payout à J+3 après l'événement |
| **Weezevent** | Self-serve : email vérifié, création autonome, publication libre | Droit de demander pièce d'identité, Kbis, justificatif **avant le premier virement** ; blocage possible si doute | Reversements 2×/mois (1er et 16), premier reversement retardé pour contrôle |
| **Shotgun** | Self-serve mais **vérification Stripe obligatoire pour TOUS les organisateurs, même événements gratuits**, avant toute vente | Identité + Kbis/asbl + justificatif de domicile via Stripe Connect | Virements directs vers le compte de l'organisateur |
| **DICE** | Compte promoteur via « Get started » / sales — relation commerciale managée | Non documenté publiquement | Non documenté publiquement |
| **Ticketmaster (TM1)** | Accès commercial managé (pas d'inscription ouverte documentée) | Oui, contractuel, par compte client | Par accord client |

**Lecture pour SmartCount** :
- Le marché converge vers : **compte libre → activation contrôlée → vérification financière uniquement si paiement** ;
- **Shotgun prouve qu'un contrôle strict est marketable** (« vérification obligatoire » est présentée comme une garantie de qualité, pas comme un frein) ;
- **Weezevent valide votre mécanique d'expiration** : droit de retenir les fonds et d'exiger des documents — votre contrat doit prévoir la même chose (clause de rétractation/résiliation) ;
- Si vous encaissez un jour pour le compte des organisateurs : **PSD2** s'applique (l'exception « agent commercial » est très étroite — Q&A EBA 2020_5354), il faut soit un partenaire PSP (Stripe Connect, **destination charges** + onboarding KYC embarqué), soit un statut d'établissement de paiement. À traiter avec un conseil juridique **avant** d'encaisser le premier euro pour un tiers.

---

## 5. Fonctionnalités recommandées (roadmap)

### P0 — Sécurité de base (bloquant prod)
1. Correction élévation de privilège `role` (§3.4.1) ;
2. Rate limiting + lockout + captcha invisible (Turnstile/hCaptcha) sur register/forgot-password ;
3. Politique mots de passe + OTP robuste ;
4. Swagger off en prod + erreurs 500 sanitizées ;
5. Authentification du WebSocket.

### P1 — Onboarding organisateur (le scénario 3)
6. Entité `DemandeOrganisateur` + machine à états + job d'expiration 7 j + purge 30 j ;
7. Console admin des demandes (approuver / refuser motivé / ignorer) + emails transactionnels (de demandes, de décision, de licence) ;
8. Cycle de vie de `Licence` branché sur l'approbation (activation, expiration, révocation) ;
9. **Bac à sable** : l'organisateur en attente peut configurer mais pas publier — meilleure conversion et meilleure qualité des demandes ;
10. **Journal d'audit** : qui a approuvé quoi, quand, avec quel motif (obligation de traçabilité + protection en cas de litige) ;
11. CGU acceptées à l'inscription (case obligatoire + horodatage + version du document = preuve de consentement, art. 7 RGPD).

### P1 bis — Nouveaux volets issus des décisions (D2–D5)

11. **Abonnement en espèces (saisie admin)** : entité `Abonnement` (statuts `EN_ATTENTE / ACTIF / EN_GRACE / SUSPENDU`, période, moyen, référence de paiement) ; l'admin enregistre le paiement reçu en main propre → passage à `ACTIF` → licence activée → déblocage du dashboard organisateur ; **journal d'audit des paiements** (obligatoire : c'est une preuve de créance en cas de litige) ; job de fin de période → `EN_GRACE` (7 j) puis `SUSPENDU` (licences révoquées, données conservées, lecture seule) ; l'intégration mobile money (Efaina/CinetPay/Orange OM WebPay) est **reportée** à la phase d'encaissement en ligne ;
12. **Page publique événements (type e-commerce)** : vue publique paginée/filtrable (ville, date, catégorie), **aucun événement visible sans passage en `PUBLIEE`** ; chaque publication est horodatée et journalisée ;
13. **Workflow de publication admin** : `BROUILLON → SOUMISE → PUBLIEE ↔ MASQUEE` avec boutons Accepter / Masquer (motif) dans la console admin, emails automatiques à l'organisateur à chaque changement d'état ;
14. **Réservation visiteur sans engagement** : une réservation crée une **intention** (TTL 15 min) qui **n'occupe pas la capacité** ; la place n'est engagée qu'à la confirmation du paiement mobile money — implique une revérification de capacité atomique au paiement (risque de survente) et une stratégie de dépassement (remboursement immédiat ou liste d'attente, à arbitrer) ;

### P2 — Confiance et échelle
12. **MFA (TOTP) obligatoire pour les ADMIN**, recommandé pour les organisateurs — un compte admin = tous les compteurs clients ;
13. Suppression/purge automatique des comptes inactifs et des demandes expirées (politique de rétention documentée, CNIL) ;
14. Monitoring : alerting sur pics d'inscriptions, tentatives OTP échouées, comptes ADMIN créés ;
15. *Si monétisation des encaissements* : Stripe Connect (onboarding KYC embarqué), 3-D Secure, réconciliation, facturation ;
16. *Si growth* : magic-link / SSO Google pour réduire le spam mot de passe (mais attention : ne remplace pas le contrôle admin).

---

## 6. Autres points de vigilance du projet (hors auth, à ne pas oublier)

Ces points ont été identifiés lors des revues précédentes et restent ouverts :

1. **Télémétrie rejetée (bloquant firmware)** — les payloads `seaside/telemetry/...` ne sont pas aboutissants ; correction conjointe firmware/API prioritaire (cf. table de conformité de `SEQUENCE_DIAGRAMS.md`) ;
2. **MQTT en clair** — le broker HiveMQ doit être en TLS avec authentification par certificat ou credentials par module ; aujourd'hui la confiance est « réseau = sûr », ce n'est pas défendable ;
3. **Confiance ESP-NOW aveugle** — n'importe quel ESP32 voisin peut émuler un module. Prévoir au minimum : seed secret par module (déjà prévu côté doc firmware), idempotence des comptages (numéro de séquence) et **alerte d'écart brut/système** côté backend ;
4. **Secrets dans l'historique Git** — rotation effective de tout secret ayant jamais transité par le dépôt (MQTT, JWT secret, SMTP, URL DB) + pré-commit secret scanning (gitleaks) ;
5. **Refresh tokens non révocables côté access token** — le JWT d'accès (durée à vérifier) reste valide même après logout ; ajouter une courte durée (15 min max) et une liste de révocation pour les cas sensibles (désactivation admin d'un compte) ;
6. **Périmètre ticketing** — `feature/ticketing` est vide : ne pas commencer l'implémentation des réservations **avant** d'avoir arbitré ce document, car le parcours de réservation dépend du statut de l'organisateur (qui peut publier un événement payant ?) ;
7. **Conformité RGPD de A à Z** : registre des traitements, DPO si volume, mentions d'information à l'inscription, droit à l'effacement opérationnel (y compris dans les sauvegardes), sous-traitants (HiveMQ, SMTP, hébergeur) contractualisés.

---

## 7. Questions à poser au CEO (arbitrages)

### Business model & go-to-market
1. **Quel modèle de revenu** ? Abonnement organisateur ? Commission sur billets ? Vente de modules ESP32 ? → détermine si le KYC/PSD2 est requis dès J1 (encaissement pour compte de tiers) ou différable ;
2. **Qui valide les demandes, et avec quel SLA** ? Un admin unique en start-up, ou une file partagée ? Un délai de validation > 48 h fait fuir les organisateurs sérieux ;
3. **Y a-t-il une équipe commerciale** qui signe les premiers contrats ? Si oui, le scénario 2 (provisionnement) peut servir de phase 1 le temps de construire le self-serve ;
4. **Existe-t-il des cas d'usage non-organisateur** (association, mairie, entreprise privée avec contrôle d'accès interne) ? Le workflow de validation doit-il être adapté (documents requis : Kbis ? attestation ? rien) ?

### Politique de refus
5. **Quels motifs de refus sont légitimes** ? (prix non accepté comme dans vos notes ? zone géographique non couverte ? capacité trop faible ? suspicion de spam ? concurrent ?) → à formaliser pour l'admin et pour l'email motivé ;
6. **Combien de tentatives** avant blocage définitif d'un email (anti-abus) ?

### Expérience & conformité
7. **Que peut faire l'organisateur en attendant** ? (Rien / configurer en bac à sable / voir la démo ?) — le bac à sable convertit mieux mais expose davantage de surface ;
8. **Purge des demandes refusées : 7 jours pour valider, 30 jours pour purge** — validez-vous ces durées ( RGPD minimisation ) ? Un délai plus long aide au support mais augmente l'exposition ;
9. **MFA pour les admins** : obligatoire ou recommandé ? (Je recommande obligatoire — c'est un standard 2026 pour tout compte admin de SaaS) ;
10. **Encaissement en ligne : aujourd'hui ou phase 2 ?** Si aujourd'hui, il faut intégrer Stripe Connect (ou équivalent) et son parcours KYC embarqué **dès le départ** — le rétrofitter est coûteux ;
11. **Combien d'admins au lancement**, et faut-il un compte « brûlé » de test ? (Zéro compte admin de secours en prod = risque d'impasse.) ;

### Questions issues des décisions D2–D5 (21/10, mises à jour après report du mobile money)
12. ~~Qui détient les fonds des billets~~ — **sans objet pour l'instant** : aucun paiement en ligne. La question reviendra au moment de l'encaissement des billets ; à ce moment-là, préférer le versement direct au compte de l'organisateur (la plateforme ne touche que l'abonnement, perçu en espèces).
13. **Grâce après impayé** : 7 jours de grâce avant suspension (recommandé), ou suspension immédiatement ? Et la licence pendant la grâce : gelée ou active ?
14. **Réservations de billets** : paiement au guichet en espèces (engagement de la place au check-in, via le module) — confirmez-vous ce modèle, ou un paiement en ligne est-il prévu à court terme ?
15. **Renouvellement** : l'admin génère la nouvelle licence **manuellement** à réception de l'espèce (décision D2), ou l'organisateur peut-il demander un renouvellement en ligne qui reste en attente de validation admin ?

---

## 8. Prochaine étape proposée

Une fois les questions 1–4 ci-dessus tranchées, je peux enchaîner sur l'un de ces livrables (au choix) :

- **A.** Implémentation P0 (correctifs sécurité d'auth, y compris la fermeture de l'élévation de privilège) — le plus urgent, indépendant des choix CEO ;
- **B.** Maquettes + spec technique complète du scénario 3 (diagrammes de séquence ajoutés à `SEQUENCE_DIAGRAMS.md`, contrats d'API OpenAPI) ;
- **C.** Correction du rejet de télémétrie (bloquant firmware/API), en parallèle du chantier auth.

---

### Sources

- Eventbrite — création d'événement (self-serve) : https://support.eventbrite.com/hc/en-us/articles/34824366421522-Create-an-event
- Eventbrite — vérification financière Stripe avant payouts : https://support.eventbrite.com/hc/en-us/articles/34865768399634-Verify-your-financial-information-through-Stripe-USA
- Weezevent — terms (droit de demander documents, blocage de payouts) : https://weezevent.com/en-gb/terms-of-use/
- Weezevent — demande d'informations complémentaires avant premier paiement : https://help.weezevent.com/en/articles/13428417-request-for-additional-information
- Shotgun — vérification Stripe obligatoire, y compris événements gratuits : https://support-pro.shotgun.live/hc/en-us/articles/31038951238546-Connect-your-account-to-Stripe
- DICE — partenaires / promoteurs : https://dice.fm/partners
- Ticketmaster TM1 : https://business.ticketmaster.com/solutions/event-creation-management/
- Stripe Connect — destination charges : https://docs.stripe.com/connect/destination-charges
- Stripe Connect — onboarding KYC embarqué : https://docs.stripe.com/connect/supported-embedded-components/account-onboarding
- EBA Q&A 2020_5354 (exception agent commercial étroite) : https://www.eba.europa.eu/single-rule-book-qa/qna/view/publicId/2020_5354
- CNIL — minimisation des données : https://www.cnil.fr/fr/minimiser-les-donnees-collectees
- CNIL — durées de conservation : https://www.cnil.fr/fr/passer-laction/les-durees-de-conservation-des-donnees
- OWASP — email verification : https://cheatsheetseries.owasp.org/cheatsheets/Email_Validation_and_Verification_Cheat_Sheet.html
- OWASP API Security — rate limiting : https://owasp.org/API-Security/editions/2019/en/0xa4-lack-of-resources-and-rate-limiting/
- NIST SP 800-63B (politique de mots de passe) : https://pages.nist.gov/800-63-3/sp800-63b.html
