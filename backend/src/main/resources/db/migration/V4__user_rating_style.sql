-- Per-user display preference for scores. Scores stay stored as 0-100;
-- this only controls how they are shown (percent, 5 stars, or 10 stars).
ALTER TABLE public.users
    ADD COLUMN rating_style VARCHAR(16) NOT NULL DEFAULT 'PERCENT';

ALTER TABLE public.users
    ADD CONSTRAINT users_rating_style_check
    CHECK (rating_style IN ('PERCENT', 'STARS_5', 'STARS_10'));
