-- ════════════════════════════════════════════════════════════
-- V2 — Le rôle ORGANISATEUR (demande validée par l'admin) doit
-- être autorisé par la contrainte CHECK de utilisateurs.role.
--
-- Historique : la table a été créée par Hibernate (ddl-auto=update)
-- avec un CHECK limité à ADMIN / CLIENT / COMPTOIRE. `ddl-auto=update`
-- ne modifie JAMAIS une contrainte existante → toute approbation de
-- demande d'organisateur échouait en base (erreur 500/400).
--
-- Migration défensive : sur une base neuve, la table n'existe pas
-- encore (Flyway s'exécute avant Hibernate) et le CHECK est généré
-- directement à partir de l'enum courant → on ne fait rien.
-- ════════════════════════════════════════════════════════════

DO $$
BEGIN
    IF to_regclass('public.utilisateurs') IS NOT NULL THEN
        ALTER TABLE utilisateurs DROP CONSTRAINT IF EXISTS utilisateurs_role_check;
        ALTER TABLE utilisateurs ADD CONSTRAINT utilisateurs_role_check
            CHECK (role IN ('ADMIN', 'ORGANISATEUR', 'CLIENT', 'COMPTOIRE'));
    END IF;
END $$;
