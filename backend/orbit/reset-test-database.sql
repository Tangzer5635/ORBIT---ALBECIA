-- ATTENTION : supprime toutes les données des tables applicatives du schéma public.
-- À lancer uniquement sur la base de TEST ORBIT. Le schéma reste en place.
DO $$
DECLARE r RECORD;
BEGIN
  FOR r IN
    SELECT tablename FROM pg_tables
    WHERE schemaname = 'public'
      AND tablename NOT IN ('flyway_schema_history', 'databasechangelog', 'databasechangeloglock')
  LOOP
    EXECUTE format('TRUNCATE TABLE public.%I RESTART IDENTITY CASCADE', r.tablename);
  END LOOP;
END $$;
