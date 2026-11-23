-- Wipes all app data except one account (its login, OAuth link, sessions, admin role,
-- privacy settings and rating style). Provider responses in "ApiCache" are kept so pages stay fast.
--
-- Usage:
--   docker exec -i local_postgres psql -U admin -d media_java -v ON_ERROR_STOP=1 -v keep=cloudy < scripts/seed/wipe.sql

\if :{?keep}
\else
  \echo 'Pass the account to keep: -v keep=<username>'
  \quit
\endif

BEGIN;

-- Stop if the account to keep does not exist, so we never wipe every user by accident.
SELECT set_config('seed.keep_id', id::text, true) FROM public.users WHERE username = :'keep';
DO $$
BEGIN
  IF current_setting('seed.keep_id', true) IS NULL OR current_setting('seed.keep_id', true) = '' THEN
    RAISE EXCEPTION 'Account to keep was not found; nothing was deleted';
  END IF;
END $$;

DELETE FROM public.user_activities;
DELETE FROM public.user_friend_preferences;
DELETE FROM public.friendships;
DELETE FROM public.activity_log;
DELETE FROM public.user_list_items;
DELETE FROM public.user_lists;
DELETE FROM public.user_ratings;
DELETE FROM public.user_watchlist;
DELETE FROM public.user_badges;
DELETE FROM public."UserStatsCache";
DELETE FROM public.media_stats;
DELETE FROM public.global_rankings;
DELETE FROM public.episodes;
DELETE FROM public.seasons;
DELETE FROM public.media;
DELETE FROM public.people;
DELETE FROM public.companies;
DELETE FROM public."BackgroundJob";
DELETE FROM public."SystemLog";
DELETE FROM public."VerificationToken";

DELETE FROM public.user_privacy_settings WHERE user_id <> current_setting('seed.keep_id')::uuid;
DELETE FROM public."Session" WHERE "userId" <> current_setting('seed.keep_id')::uuid;
DELETE FROM public."Account" WHERE "userId" <> current_setting('seed.keep_id')::uuid;
DELETE FROM public.users WHERE id <> current_setting('seed.keep_id')::uuid;

UPDATE public.users SET "showcaseBadges" = ARRAY[]::text[] WHERE id = current_setting('seed.keep_id')::uuid;

COMMIT;

SELECT username, role FROM public.users;
