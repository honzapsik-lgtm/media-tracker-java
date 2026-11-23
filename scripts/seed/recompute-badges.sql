-- Recomputes badges for every user with the same rules as StatsService.badgeProgress, and fills
-- each seeded profile's showcase. Safe to re-run. Included in seed-large.sql by generate_seed.py.
--   docker exec -i local_postgres psql -U admin -d media_java -v ON_ERROR_STOP=1 < scripts/seed/recompute-badges.sql

WITH r AS (
  SELECT user_id, media_id, score, review_text, is_deep_review, created_at,
         ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at) AS n,
         CASE
           WHEN media_id LIKE 'tmdb-movie-%' THEN 'movie'
           WHEN media_id ~ '^tmdb-tv-[0-9]+$' THEN 'show'
           WHEN media_id ~ '^tmdb-tv-[0-9]+-s[0-9]+$' THEN 'season'
           WHEN media_id LIKE 'igdb-%' OR media_id LIKE 'rawg-%' THEN 'game'
           WHEN media_id LIKE 'mangadex-%' OR media_id LIKE 'anilist-manga-%' OR media_id LIKE 'manga-%' THEN 'manga'
         END AS kind
  FROM public.user_ratings
),
per_user AS (
  SELECT user_id,
         COUNT(*) AS total, AVG(score) AS avg,
         COUNT(*) FILTER (WHERE kind = 'movie') AS movies,
         COUNT(*) FILTER (WHERE kind = 'show') AS shows,
         COUNT(*) FILTER (WHERE kind = 'season') AS seasons,
         COUNT(*) FILTER (WHERE kind = 'game') AS games,
         COUNT(*) FILTER (WHERE kind = 'manga') AS manga,
         COUNT(*) FILTER (WHERE review_text IS NOT NULL AND btrim(review_text) <> '') AS reviews,
         COUNT(*) FILTER (WHERE is_deep_review) AS deep,
         BOOL_OR(score <= 20) AS void_stare,
         BOOL_OR(score = 100) AS masterpiece,
         MAX(created_at) AS last_at,
         MIN(created_at) FILTER (WHERE score <= 20) AS void_at,
         MIN(created_at) FILTER (WHERE score = 100) AS kino_at
  FROM r GROUP BY user_id
),
nth AS (  -- when the Nth rating happened, for unlock dates
  SELECT user_id,
         MAX(created_at) FILTER (WHERE n = 10) AS at10, MAX(created_at) FILTER (WHERE n = 50) AS at50,
         MAX(created_at) FILTER (WHERE n = 100) AS at100, MAX(created_at) FILTER (WHERE n = 250) AS at250,
         MAX(created_at) FILTER (WHERE n = 500) AS at500
  FROM r GROUP BY user_id
),
social AS (
  SELECT u.id AS user_id,
         (SELECT COUNT(*) FROM public.friendships f WHERE f.status = 'ACCEPTED' AND u.id IN (f.sender_id, f.receiver_id)) AS friends,
         (SELECT MIN(created_at) FROM public.user_lists l WHERE l.user_id = u.id) AS first_list,
         (SELECT COUNT(*) FROM public.user_watchlist w WHERE w.user_id = u.id AND w.status = 'COMPLETED') AS completed
  FROM public.users u
),
earned AS (
  SELECT p.user_id, b.badge, COALESCE(b.at, p.last_at, NOW()) AS at
  FROM per_user p JOIN nth USING (user_id)
  CROSS JOIN LATERAL (VALUES
    ('ratings_10', p.total >= 10, nth.at10),
    ('ratings_50', p.total >= 50, nth.at50),
    ('ratings_100', p.total >= 100, nth.at100),
    ('ratings_250', p.total >= 250, nth.at250),
    ('ratings_500', p.total >= 500, nth.at500),
    ('movies_25', p.movies >= 25, NULL::timestamptz),
    ('shows_25', p.shows >= 25, NULL),
    ('seasons_10', p.seasons >= 10, NULL),
    ('games_10', p.games >= 10, NULL),
    ('manga_10', p.manga >= 10, NULL),
    ('all_rounder', LEAST(p.movies, p.shows, p.games, p.manga) >= 5, NULL),
    ('reviews_10', p.reviews >= 10, NULL),
    ('reviews_50', p.reviews >= 50, NULL),
    ('deep_10', p.deep >= 10, NULL),
    ('void_stare', p.void_stare, p.void_at),
    ('masterpiece', p.masterpiece, p.kino_at),
    ('tough_crowd', p.total >= 25 AND p.avg <= 55, NULL),
    ('golden_heart', p.total >= 25 AND p.avg >= 85, NULL)
  ) AS b(badge, ok, at)
  WHERE b.ok
  UNION ALL
  SELECT s.user_id, b.badge, COALESCE(b.at, NOW())
  FROM social s
  CROSS JOIN LATERAL (VALUES
    ('social_10', s.friends >= 10, NULL::timestamptz),
    ('curator', s.first_list IS NOT NULL, s.first_list),
    ('finisher', s.completed >= 25, NULL)
  ) AS b(badge, ok, at)
  WHERE b.ok
)
INSERT INTO public.user_badges (user_id, badge_id, unlocked_at)
SELECT user_id, badge, at FROM earned
ON CONFLICT DO NOTHING;

-- Showcase up to three unlocked badges on seeded profiles (the rarest first)
UPDATE public.users u SET "showcaseBadges" = b.badges
FROM (
  SELECT user_id, (array_agg(badge_id ORDER BY CASE badge_id
      WHEN 'ratings_500' THEN 1 WHEN 'all_rounder' THEN 2 WHEN 'reviews_50' THEN 3 WHEN 'ratings_250' THEN 4
      WHEN 'golden_heart' THEN 5 WHEN 'tough_crowd' THEN 6 WHEN 'masterpiece' THEN 7 WHEN 'deep_10' THEN 8
      WHEN 'ratings_100' THEN 9 WHEN 'curator' THEN 10 WHEN 'social_10' THEN 11 ELSE 20 END))[1:3] AS badges
  FROM public.user_badges GROUP BY user_id
) b
WHERE b.user_id = u.id AND u.email LIKE '%@example.com';
