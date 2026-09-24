-- V3: Fix MangaDex cover URLs for seeded manga in activities, ratings, watchlist, and list items

-- 1. user_activities
UPDATE public.user_activities
SET media_image = 'https://uploads.mangadex.org/covers/f3f59f12-351a-4de7-bd51-696d0764d64e/920a8cba-7c5c-4284-84c6-1c27cd2a3c0a.jpg'
WHERE media_title = 'Jujutsu Kaisen' OR media_id LIKE '%c52b2ce3-7f95-469c-96b0-474fb724fb49%' OR media_id LIKE '%f3f59f12-351a-4de7-bd51-696d0764d64e%';

UPDATE public.user_activities
SET media_image = 'https://uploads.mangadex.org/covers/30196491-8fc2-4961-8886-a58f898b1b3e/1790f17f-9184-4a48-8928-c45de48b778e.jpg'
WHERE media_title = 'Berserk' OR media_id LIKE '%d1a9fdeb-f713-407f-960c-8326b586e6fd%' OR media_id LIKE '%30196491-8fc2-4961-8886-a58f898b1b3e%';

UPDATE public.user_activities
SET media_image = 'https://uploads.mangadex.org/covers/e896c48c-3150-437d-ba57-d8567eb399ae/fa06e4e4-ef2a-477b-bfb6-a2a88793620b.jpg'
WHERE media_title = 'Chainsaw Man' OR media_id LIKE '%a77742b1-befd-49a4-bff5-1ad4e6b0ef7b%' OR media_id LIKE '%e896c48c-3150-437d-ba57-d8567eb399ae%';

UPDATE public.user_activities
SET media_image = 'https://uploads.mangadex.org/covers/a1c7c817-4e59-43b7-9365-09675a149a6f/2f4aca53-64c7-46ac-ae85-3bc9b3169890.png'
WHERE media_title = 'One Piece' OR media_id LIKE '%a1c7c817-4e59-43b7-9365-09675a149a6f%';

-- 2. user_ratings
UPDATE public.user_ratings
SET media_image = 'https://uploads.mangadex.org/covers/f3f59f12-351a-4de7-bd51-696d0764d64e/920a8cba-7c5c-4284-84c6-1c27cd2a3c0a.jpg'
WHERE media_title = 'Jujutsu Kaisen' OR media_id LIKE '%c52b2ce3-7f95-469c-96b0-474fb724fb49%' OR media_id LIKE '%f3f59f12-351a-4de7-bd51-696d0764d64e%';

UPDATE public.user_ratings
SET media_image = 'https://uploads.mangadex.org/covers/30196491-8fc2-4961-8886-a58f898b1b3e/1790f17f-9184-4a48-8928-c45de48b778e.jpg'
WHERE media_title = 'Berserk' OR media_id LIKE '%d1a9fdeb-f713-407f-960c-8326b586e6fd%' OR media_id LIKE '%30196491-8fc2-4961-8886-a58f898b1b3e%';

UPDATE public.user_ratings
SET media_image = 'https://uploads.mangadex.org/covers/e896c48c-3150-437d-ba57-d8567eb399ae/fa06e4e4-ef2a-477b-bfb6-a2a88793620b.jpg'
WHERE media_title = 'Chainsaw Man' OR media_id LIKE '%a77742b1-befd-49a4-bff5-1ad4e6b0ef7b%' OR media_id LIKE '%e896c48c-3150-437d-ba57-d8567eb399ae%';

UPDATE public.user_ratings
SET media_image = 'https://uploads.mangadex.org/covers/a1c7c817-4e59-43b7-9365-09675a149a6f/2f4aca53-64c7-46ac-ae85-3bc9b3169890.png'
WHERE media_title = 'One Piece' OR media_id LIKE '%a1c7c817-4e59-43b7-9365-09675a149a6f%';

-- 3. user_watchlist
UPDATE public.user_watchlist
SET media_image = 'https://uploads.mangadex.org/covers/f3f59f12-351a-4de7-bd51-696d0764d64e/920a8cba-7c5c-4284-84c6-1c27cd2a3c0a.jpg'
WHERE media_title = 'Jujutsu Kaisen' OR media_id LIKE '%c52b2ce3-7f95-469c-96b0-474fb724fb49%' OR media_id LIKE '%f3f59f12-351a-4de7-bd51-696d0764d64e%';

UPDATE public.user_watchlist
SET media_image = 'https://uploads.mangadex.org/covers/30196491-8fc2-4961-8886-a58f898b1b3e/1790f17f-9184-4a48-8928-c45de48b778e.jpg'
WHERE media_title = 'Berserk' OR media_id LIKE '%d1a9fdeb-f713-407f-960c-8326b586e6fd%' OR media_id LIKE '%30196491-8fc2-4961-8886-a58f898b1b3e%';

UPDATE public.user_watchlist
SET media_image = 'https://uploads.mangadex.org/covers/e896c48c-3150-437d-ba57-d8567eb399ae/fa06e4e4-ef2a-477b-bfb6-a2a88793620b.jpg'
WHERE media_title = 'Chainsaw Man' OR media_id LIKE '%a77742b1-befd-49a4-bff5-1ad4e6b0ef7b%' OR media_id LIKE '%e896c48c-3150-437d-ba57-d8567eb399ae%';

UPDATE public.user_watchlist
SET media_image = 'https://uploads.mangadex.org/covers/a1c7c817-4e59-43b7-9365-09675a149a6f/2f4aca53-64c7-46ac-ae85-3bc9b3169890.png'
WHERE media_title = 'One Piece' OR media_id LIKE '%a1c7c817-4e59-43b7-9365-09675a149a6f%';

-- 4. user_list_items
UPDATE public.user_list_items
SET media_image = 'https://uploads.mangadex.org/covers/f3f59f12-351a-4de7-bd51-696d0764d64e/920a8cba-7c5c-4284-84c6-1c27cd2a3c0a.jpg'
WHERE media_title = 'Jujutsu Kaisen' OR media_id LIKE '%c52b2ce3-7f95-469c-96b0-474fb724fb49%' OR media_id LIKE '%f3f59f12-351a-4de7-bd51-696d0764d64e%';

UPDATE public.user_list_items
SET media_image = 'https://uploads.mangadex.org/covers/30196491-8fc2-4961-8886-a58f898b1b3e/1790f17f-9184-4a48-8928-c45de48b778e.jpg'
WHERE media_title = 'Berserk' OR media_id LIKE '%d1a9fdeb-f713-407f-960c-8326b586e6fd%' OR media_id LIKE '%30196491-8fc2-4961-8886-a58f898b1b3e%';

UPDATE public.user_list_items
SET media_image = 'https://uploads.mangadex.org/covers/e896c48c-3150-437d-ba57-d8567eb399ae/fa06e4e4-ef2a-477b-bfb6-a2a88793620b.jpg'
WHERE media_title = 'Chainsaw Man' OR media_id LIKE '%a77742b1-befd-49a4-bff5-1ad4e6b0ef7b%' OR media_id LIKE '%e896c48c-3150-437d-ba57-d8567eb399ae%';

UPDATE public.user_list_items
SET media_image = 'https://uploads.mangadex.org/covers/a1c7c817-4e59-43b7-9365-09675a149a6f/2f4aca53-64c7-46ac-ae85-3bc9b3169890.png'
WHERE media_title = 'One Piece' OR media_id LIKE '%a1c7c817-4e59-43b7-9365-09675a149a6f%';
