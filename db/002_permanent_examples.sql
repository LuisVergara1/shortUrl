BEGIN;

ALTER TABLE linkslice.url ALTER COLUMN local_date_time DROP NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM linkslice.url
        WHERE short_url IN ('luisvergara', 'swagger', 'githubbackend', 'githubfrontend')
            AND local_date_time IS NOT NULL
    ) THEN
        RAISE EXCEPTION 'Un código reservado para los enlaces permanentes está ocupado';
    END IF;
END
$$;

INSERT INTO linkslice.url (short_url, url_original) VALUES
    ('luisvergara', 'https://luisvergara.dev'),
    ('swagger', '/swagger-ui/index.html'),
    ('githubbackend', 'https://github.com/LuisVergara1/shortUrl'),
    ('githubfrontend', 'https://github.com/LuisVergara1/shortUrlReact')
ON CONFLICT (short_url) DO NOTHING;

COMMIT;
