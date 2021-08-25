-- Set search path.
--
-- This does not affect the current session.
DO $$ BEGIN
    EXECUTE format(
        'ALTER DATABASE %I SET search_path = public, routing, flyway',
        current_database()
    );
END $$;
