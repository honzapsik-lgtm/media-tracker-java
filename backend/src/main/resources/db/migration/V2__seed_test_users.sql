-- V2: Seed 40 diverse users with rich profiles, ratings, deep reviews, watchlists, custom lists, friendships, and activities

-- 1. Insert 40 Diverse Users
INSERT INTO public.users (id, username, name, email, "realName", "stateRegion", country, "showcaseBadges", role, image, created_at)
VALUES
  ('a1111111-1111-1111-1111-111111111111', 'alec_turner', 'Alec Turner', 'alec.turner@example.com', 'Alexander Turner', 'Greater London', 'United Kingdom', ARRAY['early_adopter', 'critic', 'century_club'], 'user', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150', NOW() - INTERVAL '45 days'),
  ('a2222222-2222-2222-2222-222222222222', 'yuki_m', 'Yuki Miyamoto', 'yuki.m@example.jp', 'Yuki Miyamoto', 'Tokyo', 'Japan', ARRAY['binge_master', 'completionist', 'veteran'], 'user', 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=150', NOW() - INTERVAL '40 days'),
  ('a3333333-3333-3333-3333-333333333333', 'chloe_dev', 'Chloe Price', 'chloe.price@example.com', 'Chloe Elizabeth Price', 'Washington', 'United States', ARRAY['veteran', 'critic'], 'user', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150', NOW() - INTERVAL '35 days'),
  ('a4444444-4444-4444-4444-444444444444', 'matej_k', 'Matěj Kovář', 'matej.kovar@example.cz', 'Matěj Kovář', 'Prague', 'Czech Republic', ARRAY['early_adopter', 'perfectionist'], 'user', 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150', NOW() - INTERVAL '30 days'),
  ('a5555555-5555-5555-5555-555555555555', 'sophie_d', 'Sophie Dupont', 'sophie.dupont@example.fr', 'Sophie Dupont', 'Île-de-France', 'France', ARRAY['critic', 'perfectionist'], 'user', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150', NOW() - INTERVAL '28 days'),
  ('a6666666-6666-6666-6666-666666666666', 'marco_r', 'Marco Rossi', 'marco.rossi@example.it', 'Marco Rossi', 'Lombardy', 'Italy', ARRAY['veteran', 'binge_master'], 'user', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150', NOW() - INTERVAL '25 days'),
  ('a7777777-7777-7777-7777-777777777777', 'hannah_b', 'Hannah Becker', 'hannah.b@example.de', 'Hannah Becker', 'Berlin', 'Germany', ARRAY['critic', 'early_adopter'], 'user', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150', NOW() - INTERVAL '22 days'),
  ('a8888888-8888-8888-8888-888888888888', 'dmitri_v', 'Dmitri Volkov', 'dmitri.v@example.com', 'Dmitri Volkov', 'Mazovia', 'Poland', ARRAY['completionist', 'veteran'], 'user', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150', NOW() - INTERVAL '20 days'),
  ('a9999999-9999-9999-9999-999999999999', 'aiko_t', 'Aiko Tanaka', 'aiko.t@example.jp', 'Aiko Tanaka', 'Osaka', 'Japan', ARRAY['binge_master', 'century_club'], 'user', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150', NOW() - INTERVAL '18 days'),
  ('baaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'lucas_s', 'Lucas Silva', 'lucas.s@example.com.br', 'Lucas Silva', 'São Paulo', 'Brazil', ARRAY['early_adopter'], 'user', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150', NOW() - INTERVAL '16 days'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'emma_w', 'Emma Watson', 'emma.w@example.ca', 'Emma Watson', 'Ontario', 'Canada', ARRAY['critic', 'completionist'], 'user', 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=150', NOW() - INTERVAL '15 days'),
  ('bccccccc-cccc-cccc-cccc-cccccccccccc', 'viktor_s', 'Viktor Steiner', 'viktor.steiner@example.at', 'Viktor Steiner', 'Vienna', 'Austria', ARRAY['veteran'], 'user', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150', NOW() - INTERVAL '14 days'),
  ('bddddddd-dddd-dddd-dddd-dddddddddddd', 'lina_c', 'Lina Chen', 'lina.chen@example.tw', 'Lina Chen', 'Taipei', 'Taiwan', ARRAY['binge_master', 'early_adopter'], 'user', 'https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?w=150', NOW() - INTERVAL '12 days'),
  ('beeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'oliver_w', 'Oliver Wright', 'oliver.w@example.com.au', 'Oliver Wright', 'New South Wales', 'Australia', ARRAY['critic'], 'user', 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=150', NOW() - INTERVAL '10 days'),
  ('bfffffff-ffff-ffff-ffff-ffffffffffff', 'mia_l', 'Mia Lindqvist', 'mia.l@example.se', 'Mia Lindqvist', 'Stockholm', 'Sweden', ARRAY['perfectionist', 'critic'], 'user', 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=150', NOW() - INTERVAL '8 days'),
  ('c1111111-1111-1111-1111-111111111111', 'tariq_m', 'Tariq Mansour', 'tariq.m@example.ae', 'Tariq Mansour', 'Dubai', 'United Arab Emirates', ARRAY['early_adopter', 'completionist'], 'user', 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150', NOW() - INTERVAL '7 days'),
  ('c2222222-2222-2222-2222-222222222222', 'nina_n', 'Nina Nováková', 'nina.novakova@example.cz', 'Nina Nováková', 'South Moravian', 'Czech Republic', ARRAY['binge_master'], 'user', 'https://images.unsplash.com/photo-1548142813-c348350df52b?w=150', NOW() - INTERVAL '6 days'),
  ('c3333333-3333-3333-3333-333333333333', 'sam_p', 'Sam Porter', 'sam.porter@example.com', 'Sam Porter Bridges', 'Texas', 'United States', ARRAY['veteran', 'century_club'], 'user', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150', NOW() - INTERVAL '5 days'),
  ('c4444444-4444-4444-4444-444444444444', 'clara_o', 'Clara Oswald', 'clara.oswald@example.co.uk', 'Clara Oswald', 'Scotland', 'United Kingdom', ARRAY['critic'], 'user', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150', NOW() - INTERVAL '4 days'),
  ('c5555555-5555-5555-5555-555555555555', 'zack_f', 'Zack Fair', 'zack.fair@example.com', 'Zack Fair', 'California', 'United States', ARRAY['completionist', 'veteran'], 'user', 'https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?w=150', NOW() - INTERVAL '2 days'),
  -- Batch 2: 20 Additional Diverse Users
  ('d1111111-1111-1111-1111-111111111111', 'gamer_g', 'Gamer Girl', 'gamer.g@example.com', 'Samantha Rivera', 'California', 'United States', ARRAY['veteran', 'critic'], 'user', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150', NOW() - INTERVAL '25 days'),
  ('d2222222-2222-2222-2222-222222222222', 'pixel_pete', 'Peter Parker', 'peter.p@example.com', 'Peter Parker', 'New York', 'United States', ARRAY['early_adopter'], 'user', 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150', NOW() - INTERVAL '24 days'),
  ('d3333333-3333-3333-3333-333333333333', 'anime_daiki', 'Daiki Takahashi', 'daiki.t@example.jp', 'Daiki Takahashi', 'Kyoto', 'Japan', ARRAY['binge_master', 'century_club'], 'user', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150', NOW() - INTERVAL '23 days'),
  ('d4444444-4444-4444-4444-444444444444', 'eva_green', 'Eva Green', 'eva.green@example.fr', 'Eva Green', 'Paris', 'France', ARRAY['critic', 'perfectionist'], 'user', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150', NOW() - INTERVAL '22 days'),
  ('d5555555-5555-5555-5555-555555555555', 'arthur_m', 'Arthur Morgan', 'arthur.m@example.com', 'Arthur Morgan', 'Colorado', 'United States', ARRAY['veteran', 'completionist'], 'user', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150', NOW() - INTERVAL '21 days'),
  ('d6666666-6666-6666-6666-666666666666', 'geralt_r', 'Geralt of Rivia', 'geralt@example.pl', 'Geralt Rivian', 'Krakow', 'Poland', ARRAY['veteran'], 'user', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150', NOW() - INTERVAL '20 days'),
  ('d7777777-7777-7777-7777-777777777777', 'ellie_w', 'Ellie Williams', 'ellie.w@example.com', 'Ellie Williams', 'Wyoming', 'United States', ARRAY['critic'], 'user', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150', NOW() - INTERVAL '19 days'),
  ('d8888888-8888-8888-8888-888888888888', 'joel_m', 'Joel Miller', 'joel.m@example.com', 'Joel Miller', 'Texas', 'United States', ARRAY['veteran', 'early_adopter'], 'user', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150', NOW() - INTERVAL '18 days'),
  ('d9999999-9999-9999-9999-999999999999', 'cloud_s', 'Cloud Strife', 'cloud.s@example.com', 'Cloud Strife', 'Midgar', 'Japan', ARRAY['completionist', 'veteran'], 'user', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150', NOW() - INTERVAL '17 days'),
  ('e1111111-1111-1111-1111-111111111111', 'tifa_l', 'Tifa Lockhart', 'tifa.l@example.com', 'Tifa Lockhart', 'Nibelheim', 'Japan', ARRAY['early_adopter'], 'user', 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=150', NOW() - INTERVAL '16 days'),
  ('e2222222-2222-2222-2222-222222222222', 'aerith_g', 'Aerith Gainsborough', 'aerith.g@example.com', 'Aerith Gainsborough', 'Sector 5', 'Japan', ARRAY['perfectionist'], 'user', 'https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?w=150', NOW() - INTERVAL '15 days'),
  ('e3333333-3333-3333-3333-333333333333', 'spike_s', 'Spike Spiegel', 'spike.s@example.com', 'Spike Spiegel', 'Bebop', 'Mars', ARRAY['veteran', 'critic'], 'user', 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=150', NOW() - INTERVAL '14 days'),
  ('e4444444-4444-4444-4444-444444444444', 'faye_v', 'Faye Valentine', 'faye.v@example.com', 'Faye Valentine', 'Bebop', 'Earth', ARRAY['critic'], 'user', 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=150', NOW() - INTERVAL '13 days'),
  ('e5555555-5555-5555-5555-555555555555', 'edward_e', 'Edward Elric', 'edward.e@example.com', 'Edward Elric', 'Resembool', 'Amestris', ARRAY['completionist', 'century_club'], 'user', 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150', NOW() - INTERVAL '12 days'),
  ('e6666666-6666-6666-6666-666666666666', 'levi_a', 'Levi Ackerman', 'levi.a@example.com', 'Levi Ackerman', 'Wall Rose', 'Paradis', ARRAY['perfectionist', 'veteran'], 'user', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150', NOW() - INTERVAL '11 days'),
  ('e7777777-7777-7777-7777-777777777777', 'mikasa_a', 'Mikasa Ackerman', 'mikasa.a@example.com', 'Mikasa Ackerman', 'Shiganshina', 'Paradis', ARRAY['veteran'], 'user', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150', NOW() - INTERVAL '10 days'),
  ('e8888888-8888-8888-8888-888888888888', 'eren_y', 'Eren Yeager', 'eren.y@example.com', 'Eren Yeager', 'Shiganshina', 'Paradis', ARRAY['early_adopter'], 'user', 'https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?w=150', NOW() - INTERVAL '9 days'),
  ('e9999999-9999-9999-9999-999999999999', 'gojo_s', 'Satoru Gojo', 'gojo.s@example.jp', 'Satoru Gojo', 'Tokyo', 'Japan', ARRAY['perfectionist', 'critic', 'century_club'], 'user', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150', NOW() - INTERVAL '8 days'),
  ('f1111111-1111-1111-1111-111111111111', 'luffy_m', 'Monkey D. Luffy', 'luffy@example.com', 'Monkey D. Luffy', 'Foosha Village', 'East Blue', ARRAY['binge_master', 'completionist'], 'user', 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150', NOW() - INTERVAL '7 days'),
  ('f2222222-2222-2222-2222-222222222222', 'zoro_r', 'Roronoa Zoro', 'zoro@example.com', 'Roronoa Zoro', 'Shimotsuki Village', 'East Blue', ARRAY['veteran', 'century_club'], 'user', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150', NOW() - INTERVAL '6 days')
ON CONFLICT (id) DO NOTHING;

-- 2. Insert Privacy Settings
INSERT INTO public.user_privacy_settings (id, user_id, profile_visibility, ratings_visibility, watchlist_visibility, activity_visibility, updated_at)
SELECT gen_random_uuid(), u.id, 'PUBLIC'::public."VisibilityLevel", 'PUBLIC'::public."VisibilityLevel", 'PUBLIC'::public."VisibilityLevel", 'PUBLIC'::public."VisibilityLevel", NOW()
FROM public.users u
WHERE u.id >= 'a1111111-1111-1111-1111-111111111111'
ON CONFLICT DO NOTHING;

-- 3. Insert Badges for Seed Users
INSERT INTO public.user_badges (user_id, badge_id, unlocked_at)
SELECT u.id, b, NOW() - (random() * interval '30 days')
FROM public.users u, unnest(u."showcaseBadges") AS b
WHERE u.id >= 'a1111111-1111-1111-1111-111111111111'
ON CONFLICT DO NOTHING;

-- 4. Friendships (Social Graph around admin cloudy: de161b2c-4300-4771-bd83-b50e876f64d8)
INSERT INTO public.friendships (id, sender_id, receiver_id, status, created_at, updated_at)
SELECT seed.id, seed.sender_id::uuid, seed.receiver_id::uuid,
       seed.status::public."FriendshipStatus", seed.created_at, seed.updated_at
FROM (VALUES
  -- Accepted friends with cloudy
  (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 'de161b2c-4300-4771-bd83-b50e876f64d8', 'ACCEPTED', NOW() - INTERVAL '20 days', NOW() - INTERVAL '19 days'),
  (gen_random_uuid(), 'a2222222-2222-2222-2222-222222222222', 'de161b2c-4300-4771-bd83-b50e876f64d8', 'ACCEPTED', NOW() - INTERVAL '18 days', NOW() - INTERVAL '17 days'),
  (gen_random_uuid(), 'a3333333-3333-3333-3333-333333333333', 'de161b2c-4300-4771-bd83-b50e876f64d8', 'ACCEPTED', NOW() - INTERVAL '15 days', NOW() - INTERVAL '14 days'),
  (gen_random_uuid(), 'a4444444-4444-4444-4444-444444444444', 'de161b2c-4300-4771-bd83-b50e876f64d8', 'ACCEPTED', NOW() - INTERVAL '12 days', NOW() - INTERVAL '11 days'),
  (gen_random_uuid(), 'c3333333-3333-3333-3333-333333333333', 'de161b2c-4300-4771-bd83-b50e876f64d8', 'ACCEPTED', NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 days'),
  (gen_random_uuid(), 'c2222222-2222-2222-2222-222222222222', 'de161b2c-4300-4771-bd83-b50e876f64d8', 'ACCEPTED', NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days'),
  (gen_random_uuid(), 'd5555555-5555-5555-5555-555555555555', 'de161b2c-4300-4771-bd83-b50e876f64d8', 'ACCEPTED', NOW() - INTERVAL '7 days', NOW() - INTERVAL '6 days'),
  (gen_random_uuid(), 'd6666666-6666-6666-6666-666666666666', 'de161b2c-4300-4771-bd83-b50e876f64d8', 'ACCEPTED', NOW() - INTERVAL '8 days', NOW() - INTERVAL '7 days'),
  (gen_random_uuid(), 'e9999999-9999-9999-9999-999999999999', 'de161b2c-4300-4771-bd83-b50e876f64d8', 'ACCEPTED', NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days'),
  -- Pending requests sent TO cloudy (incoming)
  (gen_random_uuid(), 'a5555555-5555-5555-5555-555555555555', 'de161b2c-4300-4771-bd83-b50e876f64d8', 'PENDING', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
  (gen_random_uuid(), 'a7777777-7777-7777-7777-777777777777', 'de161b2c-4300-4771-bd83-b50e876f64d8', 'PENDING', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours'),
  (gen_random_uuid(), 'f1111111-1111-1111-1111-111111111111', 'de161b2c-4300-4771-bd83-b50e876f64d8', 'PENDING', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'),
  -- Pending request sent FROM cloudy (outgoing)
  (gen_random_uuid(), 'de161b2c-4300-4771-bd83-b50e876f64d8', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'PENDING', NOW() - INTERVAL '12 hours', NOW() - INTERVAL '12 hours'),
  (gen_random_uuid(), 'de161b2c-4300-4771-bd83-b50e876f64d8', 'd1111111-1111-1111-1111-111111111111', 'PENDING', NOW() - INTERVAL '10 hours', NOW() - INTERVAL '10 hours'),
  -- Cross friendships
  (gen_random_uuid(), 'd1111111-1111-1111-1111-111111111111', 'a3333333-3333-3333-3333-333333333333', 'ACCEPTED', NOW() - INTERVAL '20 days', NOW() - INTERVAL '19 days'),
  (gen_random_uuid(), 'd5555555-5555-5555-5555-555555555555', 'd6666666-6666-6666-6666-666666666666', 'ACCEPTED', NOW() - INTERVAL '18 days', NOW() - INTERVAL '17 days'),
  (gen_random_uuid(), 'd7777777-7777-7777-7777-777777777777', 'd8888888-8888-8888-8888-888888888888', 'ACCEPTED', NOW() - INTERVAL '16 days', NOW() - INTERVAL '15 days'),
  (gen_random_uuid(), 'd9999999-9999-9999-9999-999999999999', 'e1111111-1111-1111-1111-111111111111', 'ACCEPTED', NOW() - INTERVAL '15 days', NOW() - INTERVAL '14 days'),
  (gen_random_uuid(), 'e1111111-1111-1111-1111-111111111111', 'e2222222-2222-2222-2222-222222222222', 'ACCEPTED', NOW() - INTERVAL '14 days', NOW() - INTERVAL '13 days'),
  (gen_random_uuid(), 'e3333333-3333-3333-3333-333333333333', 'e4444444-4444-4444-4444-444444444444', 'ACCEPTED', NOW() - INTERVAL '12 days', NOW() - INTERVAL '11 days'),
  (gen_random_uuid(), 'e6666666-6666-6666-6666-666666666666', 'e7777777-7777-7777-7777-777777777777', 'ACCEPTED', NOW() - INTERVAL '10 days', NOW() - INTERVAL '9 days'),
  (gen_random_uuid(), 'e7777777-7777-7777-7777-777777777777', 'e8888888-8888-8888-8888-888888888888', 'ACCEPTED', NOW() - INTERVAL '8 days', NOW() - INTERVAL '7 days'),
  (gen_random_uuid(), 'f1111111-1111-1111-1111-111111111111', 'f2222222-2222-2222-2222-222222222222', 'ACCEPTED', NOW() - INTERVAL '6 days', NOW() - INTERVAL '5 days')
) AS seed(id, sender_id, receiver_id, status, created_at, updated_at)
WHERE EXISTS (SELECT 1 FROM public.users WHERE id = seed.sender_id::uuid)
  AND EXISTS (SELECT 1 FROM public.users WHERE id = seed.receiver_id::uuid)
ON CONFLICT DO NOTHING;

-- 5. Insert Ratings & Detailed Reviews
INSERT INTO public.user_ratings (id, user_id, media_id, media_title, media_image, score, review_text, username, avatar_url, criteria_scores, is_deep_review, rank_position, created_at, media_release_date)
VALUES
  -- Interstellar
  (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 'tmdb-movie-157336', 'Interstellar', 'https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg', 98, 'An absolute triumph of cinema. Zimmer''s score combined with Hoyte van Hoytema''s cinematography elevates this to an unforgettable emotional space voyage.', 'alec_turner', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150', '{"acting": 95, "pacing": 92, "visuals": 100, "soundtrack": 100, "story": 96}'::jsonb, true, 1, NOW() - INTERVAL '30 days', '2014-11-05'),
  (gen_random_uuid(), 'a4444444-4444-4444-4444-444444444444', 'tmdb-movie-157336', 'Interstellar', 'https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg', 92, 'Visually breathtaking. The docking sequence alone makes this one of Nolan''s best works.', 'matej_k', 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150', '{"acting": 90, "pacing": 88, "visuals": 98, "soundtrack": 95, "story": 90}'::jsonb, true, 2, NOW() - INTERVAL '18 days', '2014-11-05'),
  (gen_random_uuid(), 'a5555555-5555-5555-5555-555555555555', 'tmdb-movie-157336', 'Interstellar', 'https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg', 86, 'Ambitious science fiction, though slightly melodramatic in the final act.', 'sophie_d', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150', '{"acting": 85, "pacing": 80, "visuals": 95, "soundtrack": 90, "story": 82}'::jsonb, true, 4, NOW() - INTERVAL '21 days', '2014-11-05'),
  (gen_random_uuid(), 'c1111111-1111-1111-1111-111111111111', 'tmdb-movie-157336', 'Interstellar', 'https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg', 96, 'Peak science fiction on 4K IMAX. The audio dynamics will shake your entire room.', 'tariq_m', 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150', '{"acting": 92, "pacing": 90, "visuals": 100, "soundtrack": 100, "story": 94}'::jsonb, true, 1, NOW() - INTERVAL '6 days', '2014-11-05'),
  (gen_random_uuid(), 'd2222222-2222-2222-2222-222222222222', 'tmdb-movie-157336', 'Interstellar', 'https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg', 94, 'Made me cry in the theater. Cooper watching the video messages is heartbreaking.', 'pixel_pete', 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150', '{"acting": 95, "pacing": 90, "visuals": 98, "soundtrack": 98, "story": 92}'::jsonb, true, 1, NOW() - INTERVAL '15 days', '2014-11-05'),

  -- Inception
  (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 'tmdb-movie-27205', 'Inception', 'https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg', 95, 'A mind-bending masterclass in editing and cross-cutting tension. Hans Zimmer strikes gold again.', 'alec_turner', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150', '{"acting": 92, "pacing": 96, "visuals": 98, "soundtrack": 98, "story": 95}'::jsonb, true, 2, NOW() - INTERVAL '28 days', '2010-07-15'),
  (gen_random_uuid(), 'a5555555-5555-5555-5555-555555555555', 'tmdb-movie-27205', 'Inception', 'https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg', 90, 'Remarkable heist structure wrapped in dream architecture. Marion Cotillard delivers the emotional core.', 'sophie_d', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150', '{"acting": 92, "pacing": 90, "visuals": 95, "soundtrack": 94, "story": 90}'::jsonb, true, 3, NOW() - INTERVAL '24 days', '2010-07-15'),
  (gen_random_uuid(), 'beeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'tmdb-movie-27205', 'Inception', 'https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg', 94, 'Original concept executed with surgical precision.', 'oliver_w', 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=150', '{"acting": 90, "pacing": 94, "visuals": 96, "soundtrack": 95, "story": 94}'::jsonb, true, 1, NOW() - INTERVAL '8 days', '2010-07-15'),
  (gen_random_uuid(), 'd4444444-4444-4444-4444-444444444444', 'tmdb-movie-27205', 'Inception', 'https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg', 93, 'A modern classic. The rotating hallway fight is legendary practical effect filmmaking.', 'eva_green', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150', '{"acting": 90, "pacing": 95, "visuals": 96, "soundtrack": 94, "story": 92}'::jsonb, true, 2, NOW() - INTERVAL '10 days', '2010-07-15'),

  -- The Dark Knight
  (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 'tmdb-movie-155', 'The Dark Knight', 'https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg', 99, 'Heath Ledger''s Joker is iconic. A crime drama disguised as a comic book movie.', 'alec_turner', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150', '{"acting": 100, "pacing": 98, "visuals": 96, "soundtrack": 98, "story": 98}'::jsonb, true, 1, NOW() - INTERVAL '26 days', '2008-07-16'),
  (gen_random_uuid(), 'a6666666-6666-6666-6666-666666666666', 'tmdb-movie-155', 'The Dark Knight', 'https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg', 97, 'The greatest superhero film ever produced, bar none.', 'marco_r', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150', '{"acting": 100, "pacing": 96, "visuals": 95, "soundtrack": 95, "story": 96}'::jsonb, true, 1, NOW() - INTERVAL '19 days', '2008-07-16'),
  (gen_random_uuid(), 'd2222222-2222-2222-2222-222222222222', 'tmdb-movie-155', 'The Dark Knight', 'https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg', 96, 'Ledger completely disappears into the role. Electrifying script.', 'pixel_pete', 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150', '{"acting": 100, "pacing": 96, "visuals": 94, "soundtrack": 94, "story": 95}'::jsonb, true, 2, NOW() - INTERVAL '12 days', '2008-07-16'),

  -- Dune: Part Two
  (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 'tmdb-movie-693134', 'Dune: Part Two', 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg', 97, 'Modern sci-fi epic at its finest. Denis Villeneuve created our generation''s Empire Strikes Back.', 'alec_turner', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150', '{"acting": 96, "pacing": 94, "visuals": 100, "soundtrack": 98, "story": 96}'::jsonb, true, 3, NOW() - INTERVAL '15 days', '2024-02-27'),
  (gen_random_uuid(), 'c1111111-1111-1111-1111-111111111111', 'tmdb-movie-693134', 'Dune: Part Two', 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg', 99, 'Sound design and cinematography are in a class of their own.', 'tariq_m', 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150', '{"acting": 95, "pacing": 96, "visuals": 100, "soundtrack": 100, "story": 98}'::jsonb, true, 2, NOW() - INTERVAL '5 days', '2024-02-27'),
  (gen_random_uuid(), 'a7777777-7777-7777-7777-777777777777', 'tmdb-movie-693134', 'Dune: Part Two', 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg', 93, 'Austin Butler as Feyd-Rautha is hypnotic and chilling.', 'hannah_b', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150', '{"acting": 94, "pacing": 90, "visuals": 98, "soundtrack": 95, "story": 92}'::jsonb, true, 1, NOW() - INTERVAL '12 days', '2024-02-27'),
  (gen_random_uuid(), 'd8888888-8888-8888-8888-888888888888', 'tmdb-movie-693134', 'Dune: Part Two', 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg', 95, 'The worm-riding scene was worth the price of admission alone.', 'joel_m', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150', '{"acting": 92, "pacing": 94, "visuals": 100, "soundtrack": 96, "story": 94}'::jsonb, true, 1, NOW() - INTERVAL '8 days', '2024-02-27'),

  -- Oppenheimer
  (gen_random_uuid(), 'a4444444-4444-4444-4444-444444444444', 'tmdb-movie-872585', 'Oppenheimer', 'https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg', 96, 'Cillian Murphy gives the performance of a lifetime. The sound design during Trinity is unforgettable.', 'matej_k', 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150', '{"acting": 100, "pacing": 92, "visuals": 95, "soundtrack": 96, "story": 95}'::jsonb, true, 1, NOW() - INTERVAL '20 days', '2023-07-19'),
  (gen_random_uuid(), 'a5555555-5555-5555-5555-555555555555', 'tmdb-movie-872585', 'Oppenheimer', 'https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg', 94, 'A thrilling examination of guilt, hubris, and political maneuvering.', 'sophie_d', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150', '{"acting": 98, "pacing": 90, "visuals": 94, "soundtrack": 96, "story": 92}'::jsonb, true, 2, NOW() - INTERVAL '14 days', '2023-07-19'),
  (gen_random_uuid(), 'd4444444-4444-4444-4444-444444444444', 'tmdb-movie-872585', 'Oppenheimer', 'https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg', 95, 'Intense biographical cinema. Ludwig Göransson''s violin motifs are chilling.', 'eva_green', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150', '{"acting": 98, "pacing": 92, "visuals": 94, "soundtrack": 98, "story": 94}'::jsonb, true, 1, NOW() - INTERVAL '9 days', '2023-07-19'),

  -- Spirited Away
  (gen_random_uuid(), 'a2222222-2222-2222-2222-222222222222', 'tmdb-movie-129', 'Spirited Away', 'https://image.tmdb.org/t/p/w500/39wmItIWsg5sZMyRUHLkWBcuVCM.jpg', 100, 'Hayao Miyazaki''s magnum opus. Pure imagination, breathtaking hand-drawn artistry, and Joe Hisaishi''s magical score.', 'yuki_m', 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=150', '{"acting": 98, "pacing": 98, "visuals": 100, "soundtrack": 100, "story": 100}'::jsonb, true, 1, NOW() - INTERVAL '35 days', '2001-07-20'),
  (gen_random_uuid(), 'c2222222-2222-2222-2222-222222222222', 'tmdb-movie-129', 'Spirited Away', 'https://image.tmdb.org/t/p/w500/39wmItIWsg5sZMyRUHLkWBcuVCM.jpg', 98, 'Every frame feels like a painting full of spirit and warmth.', 'nina_n', 'https://images.unsplash.com/photo-1548142813-c348350df52b?w=150', '{"acting": 95, "pacing": 96, "visuals": 100, "soundtrack": 100, "story": 98}'::jsonb, true, 1, NOW() - INTERVAL '5 days', '2001-07-20'),
  (gen_random_uuid(), 'd3333333-3333-3333-3333-333333333333', 'tmdb-movie-129', 'Spirited Away', 'https://image.tmdb.org/t/p/w500/39wmItIWsg5sZMyRUHLkWBcuVCM.jpg', 99, 'Studio Ghibli at the absolute height of their power.', 'anime_daiki', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150', '{"acting": 96, "pacing": 98, "visuals": 100, "soundtrack": 100, "story": 98}'::jsonb, true, 1, NOW() - INTERVAL '11 days', '2001-07-20'),

  -- Parasite
  (gen_random_uuid(), 'a7777777-7777-7777-7777-777777777777', 'tmdb-movie-496243', 'Parasite', 'https://image.tmdb.org/t/p/w500/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg', 98, 'Flawless social commentary and genre blending. Bong Joon-ho directs with unmatched precision.', 'hannah_b', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150', '{"acting": 98, "pacing": 98, "visuals": 96, "soundtrack": 92, "story": 100}'::jsonb, true, 2, NOW() - INTERVAL '19 days', '2019-05-30'),
  (gen_random_uuid(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'tmdb-movie-496243', 'Parasite', 'https://image.tmdb.org/t/p/w500/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg', 95, 'The tonal shift at the halfway mark is one of the best cinema moments ever.', 'emma_w', 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=150', '{"acting": 96, "pacing": 96, "visuals": 94, "soundtrack": 90, "story": 98}'::jsonb, true, 1, NOW() - INTERVAL '11 days', '2019-05-30'),
  (gen_random_uuid(), 'e4444444-4444-4444-4444-444444444444', 'tmdb-movie-496243', 'Parasite', 'https://image.tmdb.org/t/p/w500/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg', 96, 'Hilarious, dark, and utterly heartbreaking.', 'faye_v', 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=150', '{"acting": 96, "pacing": 96, "visuals": 94, "soundtrack": 92, "story": 98}'::jsonb, true, 1, NOW() - INTERVAL '7 days', '2019-05-30'),

  -- Arcane (TV)
  (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 'tmdb-tv-94605', 'Arcane', 'https://image.tmdb.org/t/p/w500/fqldf2t8ztc9aiwn3k6mlX3tvRT.jpg', 99, 'Fortiche set a new benchmark for animation in history. Rich character arcs, incredible action choreography, and gorgeous painterly textures.', 'alec_turner', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150', '{"acting": 98, "pacing": 98, "visuals": 100, "soundtrack": 98, "story": 98}'::jsonb, true, 1, NOW() - INTERVAL '25 days', '2021-11-06'),
  (gen_random_uuid(), 'a3333333-3333-3333-3333-333333333333', 'tmdb-tv-94605', 'Arcane', 'https://image.tmdb.org/t/p/w500/fqldf2t8ztc9aiwn3k6mlX3tvRT.jpg', 98, 'Jinx and Vi''s dynamic is gut-wrenching and beautifully written.', 'chloe_dev', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150', '{"acting": 96, "pacing": 96, "visuals": 100, "soundtrack": 98, "story": 98}'::jsonb, true, 1, NOW() - INTERVAL '20 days', '2021-11-06'),
  (gen_random_uuid(), 'c4444444-4444-4444-4444-444444444444', 'tmdb-tv-94605', 'Arcane', 'https://image.tmdb.org/t/p/w500/fqldf2t8ztc9aiwn3k6mlX3tvRT.jpg', 96, 'Even if you don''t know League of Legends, this is peak storytelling.', 'clara_o', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150', '{"acting": 94, "pacing": 95, "visuals": 100, "soundtrack": 96, "story": 95}'::jsonb, true, 1, NOW() - INTERVAL '3 days', '2021-11-06'),
  (gen_random_uuid(), 'd1111111-1111-1111-1111-111111111111', 'tmdb-tv-94605', 'Arcane', 'https://image.tmdb.org/t/p/w500/fqldf2t8ztc9aiwn3k6mlX3tvRT.jpg', 97, 'The soundtrack choices in every single sequence give me chills.', 'gamer_g', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150', '{"acting": 96, "pacing": 96, "visuals": 100, "soundtrack": 98, "story": 96}'::jsonb, true, 1, NOW() - INTERVAL '12 days', '2021-11-06'),

  -- Attack on Titan (TV)
  (gen_random_uuid(), 'a2222222-2222-2222-2222-222222222222', 'tmdb-tv-1429', 'Attack on Titan', 'https://image.tmdb.org/t/p/w500/hTP1DtLGFamjfu8WqjnuQdP1n4i.jpg', 97, 'From survival horror to geopolitical tragedy. Isayama''s narrative foreshadowing is unmatched in modern anime.', 'yuki_m', 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=150', '{"acting": 96, "pacing": 94, "visuals": 98, "soundtrack": 100, "story": 98}'::jsonb, true, 2, NOW() - INTERVAL '30 days', '2013-04-07'),
  (gen_random_uuid(), 'a8888888-8888-8888-8888-888888888888', 'tmdb-tv-1429', 'Attack on Titan', 'https://image.tmdb.org/t/p/w500/hTP1DtLGFamjfu8WqjnuQdP1n4i.jpg', 94, 'Sawano''s music gives goosebumps every single episode.', 'dmitri_v', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150', '{"acting": 92, "pacing": 92, "visuals": 96, "soundtrack": 100, "story": 95}'::jsonb, true, 1, NOW() - INTERVAL '15 days', '2013-04-07'),
  (gen_random_uuid(), 'e6666666-6666-6666-6666-666666666666', 'tmdb-tv-1429', 'Attack on Titan', 'https://image.tmdb.org/t/p/w500/hTP1DtLGFamjfu8WqjnuQdP1n4i.jpg', 98, 'Erwin Smith''s speech in Season 3 is the greatest anime moment ever animated.', 'levi_a', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150', '{"acting": 98, "pacing": 96, "visuals": 98, "soundtrack": 100, "story": 96}'::jsonb, true, 1, NOW() - INTERVAL '7 days', '2013-04-07'),

  -- Breaking Bad (TV)
  (gen_random_uuid(), 'a4444444-4444-4444-4444-444444444444', 'tmdb-tv-1396', 'Breaking Bad', 'https://image.tmdb.org/t/p/w500/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg', 99, 'The gold standard of serialized television drama. Bryan Cranston and Aaron Paul deliver timeless acting.', 'matej_k', 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150', '{"acting": 100, "pacing": 98, "visuals": 95, "soundtrack": 94, "story": 100}'::jsonb, true, 3, NOW() - INTERVAL '25 days', '2008-01-20'),
  (gen_random_uuid(), 'bfffffff-ffff-ffff-ffff-ffffffffffff', 'tmdb-tv-1396', 'Breaking Bad', 'https://image.tmdb.org/t/p/w500/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg', 98, 'Ozymandias is the single greatest episode in television history.', 'mia_l', 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=150', '{"acting": 100, "pacing": 98, "visuals": 96, "soundtrack": 92, "story": 100}'::jsonb, true, 1, NOW() - INTERVAL '7 days', '2008-01-20'),
  (gen_random_uuid(), 'd5555555-5555-5555-5555-555555555555', 'tmdb-tv-1396', 'Breaking Bad', 'https://image.tmdb.org/t/p/w500/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg', 97, 'Masterpiece of character transformation and moral descent.', 'arthur_m', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150', '{"acting": 98, "pacing": 96, "visuals": 94, "soundtrack": 92, "story": 98}'::jsonb, true, 1, NOW() - INTERVAL '14 days', '2008-01-20'),

  -- Stranger Things (TV)
  (gen_random_uuid(), 'd1111111-1111-1111-1111-111111111111', 'tmdb-tv-66732', 'Stranger Things', 'https://image.tmdb.org/t/p/w500/49WJfeN0moxb9IPfGn8AIqMGskD.jpg', 90, '80s nostalgia with immense heart and fantastic practical creature effects in Season 1 and 4.', 'gamer_g', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150', '{"acting": 90, "pacing": 88, "visuals": 94, "soundtrack": 96, "story": 88}'::jsonb, true, 2, NOW() - INTERVAL '15 days', '2016-07-15'),
  (gen_random_uuid(), 'd7777777-7777-7777-7777-777777777777', 'tmdb-tv-66732', 'Stranger Things', 'https://image.tmdb.org/t/p/w500/49WJfeN0moxb9IPfGn8AIqMGskD.jpg', 88, 'Running Up That Hill sequence in Season 4 is iconic.', 'ellie_w', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150', '{"acting": 90, "pacing": 85, "visuals": 92, "soundtrack": 96, "story": 86}'::jsonb, true, 2, NOW() - INTERVAL '11 days', '2016-07-15'),

  -- Elden Ring (Game)
  (gen_random_uuid(), 'a3333333-3333-3333-3333-333333333333', 'igdb-game-119133', 'Elden Ring', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co4jni.png', 98, 'FromSoftware mastered open-world discovery. The sheer scale, mystery, and build variety kept me hooked for 200+ hours.', 'chloe_dev', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150', '{"gameplay": 100, "visuals": 95, "soundtrack": 96, "story": 90, "world": 100}'::jsonb, true, 2, NOW() - INTERVAL '22 days', '2022-02-25'),
  (gen_random_uuid(), 'a8888888-8888-8888-8888-888888888888', 'igdb-game-119133', 'Elden Ring', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co4jni.png', 96, 'Boss designs and lore are incredible. Challenging and deeply rewarding.', 'dmitri_v', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150', '{"gameplay": 98, "visuals": 94, "soundtrack": 95, "story": 92, "world": 98}'::jsonb, true, 2, NOW() - INTERVAL '14 days', '2022-02-25'),
  (gen_random_uuid(), 'c3333333-3333-3333-3333-333333333333', 'igdb-game-119133', 'Elden Ring', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co4jni.png', 97, 'A revolutionary dark fantasy adventure. Miyazaki does it again.', 'sam_p', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150', '{"gameplay": 98, "visuals": 96, "soundtrack": 94, "story": 90, "world": 100}'::jsonb, true, 1, NOW() - INTERVAL '4 days', '2022-02-25'),
  (gen_random_uuid(), 'd6666666-6666-6666-6666-666666666666', 'igdb-game-119133', 'Elden Ring', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co4jni.png', 97, 'Uncompromising difficulty with unparalleled reward.', 'geralt_r', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150', '{"gameplay": 100, "visuals": 95, "soundtrack": 94, "story": 90, "world": 98}'::jsonb, true, 1, NOW() - INTERVAL '9 days', '2022-02-25'),

  -- Baldur's Gate 3 (Game)
  (gen_random_uuid(), 'a3333333-3333-3333-3333-333333333333', 'igdb-game-112875', 'Baldur''s Gate 3', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co670h.png', 99, 'The gold standard of cRPGs. Narrative reactivity and companion writing are light years ahead of the industry.', 'chloe_dev', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150', '{"gameplay": 98, "visuals": 96, "soundtrack": 98, "story": 100, "world": 100}'::jsonb, true, 1, NOW() - INTERVAL '28 days', '2023-08-03'),
  (gen_random_uuid(), 'bccccccc-cccc-cccc-cccc-cccccccccccc', 'igdb-game-112875', 'Baldur''s Gate 3', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co670h.png', 97, 'Tactical depth and player freedom like nothing else in the past decade.', 'viktor_s', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150', '{"gameplay": 98, "visuals": 94, "soundtrack": 96, "story": 98, "world": 98}'::jsonb, true, 1, NOW() - INTERVAL '12 days', '2023-08-03'),
  (gen_random_uuid(), 'c5555555-5555-5555-5555-555555555555', 'igdb-game-112875', 'Baldur''s Gate 3', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co670h.png', 98, 'Every choice feels impactful. Shadowheart and Astarion are unforgettable companions.', 'zack_f', 'https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?w=150', '{"gameplay": 96, "visuals": 96, "soundtrack": 98, "story": 100, "world": 98}'::jsonb, true, 1, NOW() - INTERVAL '2 days', '2023-08-03'),
  (gen_random_uuid(), 'e5555555-5555-5555-5555-555555555555', 'igdb-game-112875', 'Baldur''s Gate 3', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co670h.png', 96, 'Act 3 performance is much better now. Incredible turn-based combat.', 'edward_e', 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150', '{"gameplay": 98, "visuals": 94, "soundtrack": 96, "story": 96, "world": 98}'::jsonb, true, 1, NOW() - INTERVAL '6 days', '2023-08-03'),

  -- Red Dead Redemption 2 (Game)
  (gen_random_uuid(), 'd5555555-5555-5555-5555-555555555555', 'igdb-game-119277', 'Red Dead Redemption 2', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co1q1f.png', 99, 'Arthur Morgan is the most human protagonist in video game history. Unbelievable world detail.', 'arthur_m', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150', '{"gameplay": 94, "visuals": 100, "soundtrack": 100, "story": 100, "world": 100}'::jsonb, true, 1, NOW() - INTERVAL '16 days', '2018-10-26'),
  (gen_random_uuid(), 'a6666666-6666-6666-6666-666666666666', 'igdb-game-119277', 'Red Dead Redemption 2', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co1q1f.png', 96, 'A living, breathing western sandbox. Truly timeless.', 'marco_r', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150', '{"gameplay": 92, "visuals": 100, "soundtrack": 98, "story": 98, "world": 100}'::jsonb, true, 2, NOW() - INTERVAL '11 days', '2018-10-26'),

  -- The Witcher 3 (Game)
  (gen_random_uuid(), 'd6666666-6666-6666-6666-666666666666', 'igdb-game-1942', 'The Witcher 3: Wild Hunt', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.png', 99, 'The benchmark for side quest design. Bloody Baron arc is unforgettable.', 'geralt_r', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150', '{"gameplay": 92, "visuals": 98, "soundtrack": 100, "story": 100, "world": 100}'::jsonb, true, 1, NOW() - INTERVAL '15 days', '2015-05-19'),
  (gen_random_uuid(), 'a4444444-4444-4444-4444-444444444444', 'igdb-game-1942', 'The Witcher 3: Wild Hunt', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.png', 95, 'Blood and Wine DLC alone is better than most full price games.', 'matej_k', 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150', '{"gameplay": 90, "visuals": 96, "soundtrack": 98, "story": 98, "world": 98}'::jsonb, true, 2, NOW() - INTERVAL '10 days', '2015-05-19'),

  -- Chainsaw Man (Manga)
  (gen_random_uuid(), 'a2222222-2222-2222-2222-222222222222', 'mangadex-manga-a77742b1-befd-49a4-bff5-1ad4e6b0ef7b', 'Chainsaw Man', 'https://mangadex.org/covers/a77742b1-befd-49a4-bff5-1ad4e6b0ef7b/ca945e4e-4628-4448-a0d3-7d228f4bc426.jpg', 96, 'Fujimoto''s chaotic brilliance on full display. Cinematic paneling, unpredictable twists, and emotional depth.', 'yuki_m', 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=150', '{"art": 95, "pacing": 98, "characters": 96, "story": 95}'::jsonb, true, 3, NOW() - INTERVAL '20 days', '2018-12-03'),
  (gen_random_uuid(), 'a9999999-9999-9999-9999-999999999999', 'mangadex-manga-a77742b1-befd-49a4-bff5-1ad4e6b0ef7b', 'Chainsaw Man', 'https://mangadex.org/covers/a77742b1-befd-49a4-bff5-1ad4e6b0ef7b/ca945e4e-4628-4448-a0d3-7d228f4bc426.jpg', 94, 'Part 1 is a rollercoaster that redefines modern shonen.', 'aiko_t', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150', '{"art": 92, "pacing": 96, "characters": 94, "story": 94}'::jsonb, true, 1, NOW() - INTERVAL '14 days', '2018-12-03'),
  (gen_random_uuid(), 'bddddddd-dddd-dddd-dddd-dddddddddddd', 'mangadex-manga-a77742b1-befd-49a4-bff5-1ad4e6b0ef7b', 'Chainsaw Man', 'https://mangadex.org/covers/a77742b1-befd-49a4-bff5-1ad4e6b0ef7b/ca945e4e-4628-4448-a0d3-7d228f4bc426.jpg', 92, 'Raw and unapologetic. Makima is one of the most chilling characters in modern manga.', 'lina_c', 'https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?w=150', '{"art": 90, "pacing": 94, "characters": 96, "story": 92}'::jsonb, true, 1, NOW() - INTERVAL '10 days', '2018-12-03'),

  -- Berserk (Manga)
  (gen_random_uuid(), 'a8888888-8888-8888-8888-888888888888', 'mangadex-manga-d1a9fdeb-f713-407f-960c-8326b586e6fd', 'Berserk', 'https://mangadex.org/covers/d1a9fdeb-f713-407f-960c-8326b586e6fd/34e0a719-74d3-4a16-89ce-f6b0b5e907d4.jpg', 100, 'Kentaro Miura''s artwork is unparalleled. The Golden Age arc remains the greatest dark fantasy story ever told.', 'dmitri_v', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150', '{"art": 100, "pacing": 98, "characters": 100, "story": 100}'::jsonb, true, 1, NOW() - INTERVAL '18 days', '1989-08-25'),
  (gen_random_uuid(), 'a2222222-2222-2222-2222-222222222222', 'mangadex-manga-d1a9fdeb-f713-407f-960c-8326b586e6fd', 'Berserk', 'https://mangadex.org/covers/d1a9fdeb-f713-407f-960c-8326b586e6fd/34e0a719-74d3-4a16-89ce-f6b0b5e907d4.jpg', 99, 'Every double-page spread belongs in an art gallery.', 'yuki_m', 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=150', '{"art": 100, "pacing": 96, "characters": 100, "story": 100}'::jsonb, true, 1, NOW() - INTERVAL '25 days', '1989-08-25'),
  (gen_random_uuid(), 'f2222222-2222-2222-2222-222222222222', 'mangadex-manga-d1a9fdeb-f713-407f-960c-8326b586e6fd', 'Berserk', 'https://mangadex.org/covers/d1a9fdeb-f713-407f-960c-8326b586e6fd/34e0a719-74d3-4a16-89ce-f6b0b5e907d4.jpg', 98, 'The sheer endurance of Guts is inspiring and tragic.', 'zoro_r', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150', '{"art": 100, "pacing": 95, "characters": 100, "story": 98}'::jsonb, true, 1, NOW() - INTERVAL '5 days', '1989-08-25'),

  -- Jujutsu Kaisen (Manga)
  (gen_random_uuid(), 'e9999999-9999-9999-9999-999999999999', 'mangadex-manga-c52b2ce3-7f95-469c-96b0-474fb724fb49', 'Jujutsu Kaisen', 'https://mangadex.org/covers/c52b2ce3-7f95-469c-96b0-474fb724fb49/92c81da5-671e-450f-90e8-0955353597ba.jpg', 94, 'Shibuya Incident arc is one of the most intense continuous battles in manga history.', 'gojo_s', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150', '{"art": 92, "pacing": 96, "characters": 96, "story": 92}'::jsonb, true, 1, NOW() - INTERVAL '8 days', '2018-03-05'),
  (gen_random_uuid(), 'a9999999-9999-9999-9999-999999999999', 'mangadex-manga-c52b2ce3-7f95-469c-96b0-474fb724fb49', 'Jujutsu Kaisen', 'https://mangadex.org/covers/c52b2ce3-7f95-469c-96b0-474fb724fb49/92c81da5-671e-450f-90e8-0955353597ba.jpg', 91, 'Cursed technique power system is deep and inventive.', 'aiko_t', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150', '{"art": 90, "pacing": 92, "characters": 94, "story": 90}'::jsonb, true, 2, NOW() - INTERVAL '13 days', '2018-03-05'),

  -- One Piece (Manga)
  (gen_random_uuid(), 'f1111111-1111-1111-1111-111111111111', 'mangadex-manga-a1c7c817-4e59-43b7-9365-09675a149a6f', 'One Piece', 'https://mangadex.org/covers/a1c7c817-4e59-43b7-9365-09675a149a6f/b2a75960-e4b2-4d23-9562-7e78119eb893.jpg', 100, 'Over 1100 chapters and Oda still delivers peak worldbuilding and emotional payoffs.', 'luffy_m', 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150', '{"art": 95, "pacing": 92, "characters": 100, "story": 100}'::jsonb, true, 1, NOW() - INTERVAL '7 days', '1997-07-22'),
  (gen_random_uuid(), 'a2222222-2222-2222-2222-222222222222', 'mangadex-manga-a1c7c817-4e59-43b7-9365-09675a149a6f', 'One Piece', 'https://mangadex.org/covers/a1c7c817-4e59-43b7-9365-09675a149a6f/b2a75960-e4b2-4d23-9562-7e78119eb893.jpg', 98, 'The Enies Lobby and Marineford arcs are legendary.', 'yuki_m', 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=150', '{"art": 94, "pacing": 90, "characters": 100, "story": 100}'::jsonb, true, 4, NOW() - INTERVAL '16 days', '1997-07-22'),

  -- Madame Web (Negative review example)
  (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 'tmdb-movie-634492', 'Madame Web', 'https://image.tmdb.org/t/p/w500/rULWuutDcN5NvtiZi4xZa3neNIL.jpg', 24, 'Disjointed dialogue, wooden line deliveries, and nonsensical editing. Hard to sit through.', 'alec_turner', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150', '{"acting": 25, "pacing": 20, "visuals": 40, "soundtrack": 35, "story": 15}'::jsonb, true, NULL, NOW() - INTERVAL '10 days', '2024-02-14')
ON CONFLICT DO NOTHING;

-- 6. Insert User Watchlist Entries
INSERT INTO public.user_watchlist (id, user_id, media_id, media_title, media_image, status, "chaptersRead", "episodesWatched", "volumesRead", media_type, "hoursPlayed", platform, "watchCount", added_at, started_at, finished_at)
VALUES
  -- Completed items
  (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 'tmdb-movie-157336', 'Interstellar', 'https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg', 'COMPLETED', 0, 1, 0, 'MOVIE', 2.8, 'Blu-ray', 3, NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days'),
  (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 'tmdb-tv-94605', 'Arcane', 'https://image.tmdb.org/t/p/w500/fqldf2t8ztc9aiwn3k6mlX3tvRT.jpg', 'COMPLETED', 0, 9, 0, 'SHOW', 6.0, 'Netflix', 2, NOW() - INTERVAL '25 days', NOW() - INTERVAL '27 days', NOW() - INTERVAL '25 days'),
  (gen_random_uuid(), 'a2222222-2222-2222-2222-222222222222', 'tmdb-tv-1429', 'Attack on Titan', 'https://image.tmdb.org/t/p/w500/hTP1DtLGFamjfu8WqjnuQdP1n4i.jpg', 'COMPLETED', 0, 89, 0, 'SHOW', 35.0, 'Crunchyroll', 1, NOW() - INTERVAL '40 days', NOW() - INTERVAL '60 days', NOW() - INTERVAL '30 days'),
  (gen_random_uuid(), 'a3333333-3333-3333-3333-333333333333', 'igdb-game-119133', 'Elden Ring', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co4jni.png', 'COMPLETED', 0, 0, 0, 'GAME', 185.5, 'PC (Steam)', 2, NOW() - INTERVAL '35 days', NOW() - INTERVAL '70 days', NOW() - INTERVAL '22 days'),
  (gen_random_uuid(), 'a3333333-3333-3333-3333-333333333333', 'igdb-game-112875', 'Baldur''s Gate 3', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co670h.png', 'COMPLETED', 0, 0, 0, 'GAME', 210.0, 'PC (Steam)', 1, NOW() - INTERVAL '28 days', NOW() - INTERVAL '60 days', NOW() - INTERVAL '20 days'),
  (gen_random_uuid(), 'a8888888-8888-8888-8888-888888888888', 'mangadex-manga-d1a9fdeb-f713-407f-960c-8326b586e6fd', 'Berserk', 'https://mangadex.org/covers/d1a9fdeb-f713-407f-960c-8326b586e6fd/34e0a719-74d3-4a16-89ce-f6b0b5e907d4.jpg', 'COMPLETED', 364, 0, 41, 'MANGA', 0.0, 'Paperback', 1, NOW() - INTERVAL '30 days', NOW() - INTERVAL '100 days', NOW() - INTERVAL '18 days'),
  (gen_random_uuid(), 'd5555555-5555-5555-5555-555555555555', 'igdb-game-119277', 'Red Dead Redemption 2', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co1q1f.png', 'COMPLETED', 0, 0, 0, 'GAME', 120.0, 'PlayStation 5', 1, NOW() - INTERVAL '20 days', NOW() - INTERVAL '40 days', NOW() - INTERVAL '16 days'),
  (gen_random_uuid(), 'd6666666-6666-6666-6666-666666666666', 'igdb-game-1942', 'The Witcher 3: Wild Hunt', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.png', 'COMPLETED', 0, 0, 0, 'GAME', 150.0, 'PC', 2, NOW() - INTERVAL '25 days', NOW() - INTERVAL '50 days', NOW() - INTERVAL '15 days'),
  (gen_random_uuid(), 'f1111111-1111-1111-1111-111111111111', 'mangadex-manga-a1c7c817-4e59-43b7-9365-09675a149a6f', 'One Piece', 'https://mangadex.org/covers/a1c7c817-4e59-43b7-9365-09675a149a6f/b2a75960-e4b2-4d23-9562-7e78119eb893.jpg', 'COMPLETED', 1100, 0, 108, 'MANGA', 0.0, 'Shonen Jump', 1, NOW() - INTERVAL '10 days', NOW() - INTERVAL '365 days', NOW() - INTERVAL '7 days'),

  -- In Progress items
  (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 'tmdb-tv-100088', 'The Last of Us', 'https://image.tmdb.org/t/p/w500/uKvVjK19uQytBkdnqw7BFXuhbta.jpg', 'IN_PROGRESS', 0, 7, 0, 'SHOW', 5.5, 'Max', 0, NOW() - INTERVAL '10 days', NOW() - INTERVAL '5 days', NULL),
  (gen_random_uuid(), 'a3333333-3333-3333-3333-333333333333', 'igdb-game-1877', 'Cyberpunk 2077', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co2mjs.png', 'IN_PROGRESS', 0, 0, 0, 'GAME', 48.0, 'PC (Steam)', 0, NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days', NULL),
  (gen_random_uuid(), 'a2222222-2222-2222-2222-222222222222', 'mangadex-manga-a77742b1-befd-49a4-bff5-1ad4e6b0ef7b', 'Chainsaw Man', 'https://mangadex.org/covers/a77742b1-befd-49a4-bff5-1ad4e6b0ef7b/ca945e4e-4628-4448-a0d3-7d228f4bc426.jpg', 'IN_PROGRESS', 152, 0, 16, 'MANGA', 0.0, 'MangaPlus', 0, NOW() - INTERVAL '20 days', NOW() - INTERVAL '19 days', NULL),
  (gen_random_uuid(), 'c3333333-3333-3333-3333-333333333333', 'igdb-game-19560', 'God of War', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co1tmu.png', 'IN_PROGRESS', 0, 0, 0, 'GAME', 16.5, 'PlayStation 5', 0, NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days', NULL),
  (gen_random_uuid(), 'd7777777-7777-7777-7777-777777777777', 'tmdb-tv-66732', 'Stranger Things', 'https://image.tmdb.org/t/p/w500/49WJfeN0moxb9IPfGn8AIqMGskD.jpg', 'IN_PROGRESS', 0, 24, 0, 'SHOW', 20.0, 'Netflix', 0, NOW() - INTERVAL '12 days', NOW() - INTERVAL '10 days', NULL),
  (gen_random_uuid(), 'e9999999-9999-9999-9999-999999999999', 'mangadex-manga-c52b2ce3-7f95-469c-96b0-474fb724fb49', 'Jujutsu Kaisen', 'https://mangadex.org/covers/c52b2ce3-7f95-469c-96b0-474fb724fb49/92c81da5-671e-450f-90e8-0955353597ba.jpg', 'IN_PROGRESS', 250, 0, 27, 'MANGA', 0.0, 'Shonen Jump', 0, NOW() - INTERVAL '15 days', NOW() - INTERVAL '14 days', NULL),

  -- Planning items
  (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 'tmdb-movie-693134', 'Dune: Part Two', 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg', 'PLANNING', 0, 0, 0, 'MOVIE', 0.0, NULL, 0, NOW() - INTERVAL '5 days', NULL, NULL),
  (gen_random_uuid(), 'a2222222-2222-2222-2222-222222222222', 'tmdb-tv-85937', 'Demon Slayer', 'https://image.tmdb.org/t/p/w500/xUfRZu2mi8jH6SzQEJGP6tjBuYj.jpg', 'PLANNING', 0, 0, 0, 'SHOW', 0.0, 'Crunchyroll', 0, NOW() - INTERVAL '4 days', NULL, NULL),
  (gen_random_uuid(), 'a4444444-4444-4444-4444-444444444444', 'tmdb-tv-76331', 'Succession', 'https://image.tmdb.org/t/p/w500/7T8d9bTfEwN3p3c5yN9h6L7QdJm.jpg', 'PLANNING', 0, 0, 0, 'SHOW', 0.0, 'Max', 0, NOW() - INTERVAL '3 days', NULL, NULL),
  (gen_random_uuid(), 'c5555555-5555-5555-5555-555555555555', 'igdb-game-134706', 'Final Fantasy VII Rebirth', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co7dkh.png', 'PLANNING', 0, 0, 0, 'GAME', 0.0, 'PlayStation 5', 0, NOW() - INTERVAL '2 days', NULL, NULL)
ON CONFLICT DO NOTHING;

-- 7. Insert Custom Lists & Ranked Items
DO $$
DECLARE
  list_scifi_id uuid := gen_random_uuid();
  list_soulslike_id uuid := gen_random_uuid();
  list_anime_id uuid := gen_random_uuid();
  list_jrpg_id uuid := gen_random_uuid();
BEGIN
  -- List 1: Sci-Fi by alec_turner
  INSERT INTO public.user_lists (id, user_id, title, description, media_type, created_at, updated_at)
  VALUES (list_scifi_id, 'a1111111-1111-1111-1111-111111111111', 'Ultimate Sci-Fi Masterpieces', 'The defining science fiction cinema of the 21st century.', 'MOVIE', NOW() - INTERVAL '25 days', NOW() - INTERVAL '15 days');

  INSERT INTO public.user_list_items (id, list_id, media_id, media_title, media_image, rank_position)
  VALUES
    (gen_random_uuid(), list_scifi_id, 'tmdb-movie-157336', 'Interstellar', 'https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg', 1),
    (gen_random_uuid(), list_scifi_id, 'tmdb-movie-693134', 'Dune: Part Two', 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg', 2),
    (gen_random_uuid(), list_scifi_id, 'tmdb-movie-27205', 'Inception', 'https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg', 3);

  -- List 2: Soulslike by chloe_dev
  INSERT INTO public.user_lists (id, user_id, title, description, media_type, created_at, updated_at)
  VALUES (list_soulslike_id, 'a3333333-3333-3333-3333-333333333333', 'Top Tier Action RPGs', 'Challenging worlds and immaculate combat mechanics.', 'GAME', NOW() - INTERVAL '20 days', NOW() - INTERVAL '10 days');

  INSERT INTO public.user_list_items (id, list_id, media_id, media_title, media_image, rank_position)
  VALUES
    (gen_random_uuid(), list_soulslike_id, 'igdb-game-112875', 'Baldur''s Gate 3', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co670h.png', 1),
    (gen_random_uuid(), list_soulslike_id, 'igdb-game-119133', 'Elden Ring', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co4jni.png', 2),
    (gen_random_uuid(), list_soulslike_id, 'igdb-game-1877', 'Cyberpunk 2077', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co2mjs.png', 3);

  -- List 3: Peak Anime by yuki_m
  INSERT INTO public.user_lists (id, user_id, title, description, media_type, created_at, updated_at)
  VALUES (list_anime_id, 'a2222222-2222-2222-2222-222222222222', 'Modern Anime Landmarks', 'Series and films that moved the medium forward.', 'SHOW', NOW() - INTERVAL '18 days', NOW() - INTERVAL '8 days');

  INSERT INTO public.user_list_items (id, list_id, media_id, media_title, media_image, rank_position)
  VALUES
    (gen_random_uuid(), list_anime_id, 'tmdb-tv-1429', 'Attack on Titan', 'https://image.tmdb.org/t/p/w500/hTP1DtLGFamjfu8WqjnuQdP1n4i.jpg', 1),
    (gen_random_uuid(), list_anime_id, 'tmdb-tv-94605', 'Arcane', 'https://image.tmdb.org/t/p/w500/fqldf2t8ztc9aiwn3k6mlX3tvRT.jpg', 2);

  -- List 4: Greatest JRPGs by cloud_s
  INSERT INTO public.user_lists (id, user_id, title, description, media_type, created_at, updated_at)
  VALUES (list_jrpg_id, 'd9999999-9999-9999-9999-999999999999', 'Essential JRPGs of All Time', 'Games with unmatched heart, music, and party dynamics.', 'GAME', NOW() - INTERVAL '14 days', NOW() - INTERVAL '7 days');

  INSERT INTO public.user_list_items (id, list_id, media_id, media_title, media_image, rank_position)
  VALUES
    (gen_random_uuid(), list_jrpg_id, 'igdb-game-112875', 'Baldur''s Gate 3', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co670h.png', 1),
    (gen_random_uuid(), list_jrpg_id, 'igdb-game-134706', 'Final Fantasy VII Rebirth', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co7dkh.png', 2);
END $$;

-- 8. Insert Activities (Populating the Friends Activity Feed)
INSERT INTO public.user_activities (id, user_id, type, media_id, media_title, media_image, media_type, data, created_at)
VALUES
  (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 'RATED_MEDIA', 'tmdb-movie-693134', 'Dune: Part Two', 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg', 'MOVIE', '{"score": 97}'::jsonb, NOW() - INTERVAL '2 hours'),
  (gen_random_uuid(), 'a3333333-3333-3333-3333-333333333333', 'WATCHLIST_STATUS', 'igdb-game-1877', 'Cyberpunk 2077', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co2mjs.png', 'GAME', '{"status": "IN_PROGRESS", "hoursPlayed": 48.0}'::jsonb, NOW() - INTERVAL '5 hours'),
  (gen_random_uuid(), 'a2222222-2222-2222-2222-222222222222', 'RATED_MEDIA', 'tmdb-tv-1429', 'Attack on Titan', 'https://image.tmdb.org/t/p/w500/hTP1DtLGFamjfu8WqjnuQdP1n4i.jpg', 'SHOW', '{"score": 97}'::jsonb, NOW() - INTERVAL '9 hours'),
  (gen_random_uuid(), 'c3333333-3333-3333-3333-333333333333', 'RATED_MEDIA', 'igdb-game-119133', 'Elden Ring', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co4jni.png', 'GAME', '{"score": 97}'::jsonb, NOW() - INTERVAL '14 hours'),
  (gen_random_uuid(), 'a4444444-4444-4444-4444-444444444444', 'RATED_MEDIA', 'tmdb-movie-872585', 'Oppenheimer', 'https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg', 'MOVIE', '{"score": 96}'::jsonb, NOW() - INTERVAL '18 hours'),
  (gen_random_uuid(), 'c2222222-2222-2222-2222-222222222222', 'RATED_MEDIA', 'tmdb-movie-129', 'Spirited Away', 'https://image.tmdb.org/t/p/w500/39wmItIWsg5sZMyRUHLkWBcuVCM.jpg', 'MOVIE', '{"score": 98}'::jsonb, NOW() - INTERVAL '1 day'),
  (gen_random_uuid(), 'c5555555-5555-5555-5555-555555555555', 'RATED_MEDIA', 'igdb-game-112875', 'Baldur''s Gate 3', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co670h.png', 'GAME', '{"score": 98}'::jsonb, NOW() - INTERVAL '2 days'),
  (gen_random_uuid(), 'a8888888-8888-8888-8888-888888888888', 'RATED_MEDIA', 'mangadex-manga-d1a9fdeb-f713-407f-960c-8326b586e6fd', 'Berserk', 'https://mangadex.org/covers/d1a9fdeb-f713-407f-960c-8326b586e6fd/34e0a719-74d3-4a16-89ce-f6b0b5e907d4.jpg', 'MANGA', '{"score": 100}'::jsonb, NOW() - INTERVAL '2 days'),
  (gen_random_uuid(), 'd5555555-5555-5555-5555-555555555555', 'RATED_MEDIA', 'igdb-game-119277', 'Red Dead Redemption 2', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co1q1f.png', 'GAME', '{"score": 99}'::jsonb, NOW() - INTERVAL '3 days'),
  (gen_random_uuid(), 'd6666666-6666-6666-6666-666666666666', 'RATED_MEDIA', 'igdb-game-1942', 'The Witcher 3: Wild Hunt', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.png', 'GAME', '{"score": 99}'::jsonb, NOW() - INTERVAL '3 days'),
  (gen_random_uuid(), 'e9999999-9999-9999-9999-999999999999', 'RATED_MEDIA', 'mangadex-manga-c52b2ce3-7f95-469c-96b0-474fb724fb49', 'Jujutsu Kaisen', 'https://mangadex.org/covers/c52b2ce3-7f95-469c-96b0-474fb724fb49/92c81da5-671e-450f-90e8-0955353597ba.jpg', 'MANGA', '{"score": 94}'::jsonb, NOW() - INTERVAL '4 days'),
  (gen_random_uuid(), 'f1111111-1111-1111-1111-111111111111', 'RATED_MEDIA', 'mangadex-manga-a1c7c817-4e59-43b7-9365-09675a149a6f', 'One Piece', 'https://mangadex.org/covers/a1c7c817-4e59-43b7-9365-09675a149a6f/b2a75960-e4b2-4d23-9562-7e78119eb893.jpg', 'MANGA', '{"score": 100}'::jsonb, NOW() - INTERVAL '4 days')
ON CONFLICT DO NOTHING;

-- 9. Refresh media_stats for all rated items
INSERT INTO public.media_stats (id, community_average, total_ratings, media_type)
SELECT
  r.media_id,
  ROUND(AVG(r.score)),
  COUNT(*),
  CASE
    WHEN r.media_id LIKE 'tmdb-movie-%' THEN 'MOVIE'::public."MediaType"
    WHEN r.media_id LIKE 'tmdb-tv-%' THEN 'SHOW'::public."MediaType"
    WHEN r.media_id LIKE 'igdb-game-%' OR r.media_id LIKE 'rawg-game-%' THEN 'GAME'::public."MediaType"
    WHEN r.media_id LIKE 'mangadex-manga-%' OR r.media_id LIKE 'anilist-manga-%' THEN 'MANGA'::public."MediaType"
    ELSE 'OTHER'::public."MediaType"
  END
FROM public.user_ratings r
GROUP BY r.media_id
ON CONFLICT (id) DO UPDATE SET
  community_average = EXCLUDED.community_average,
  total_ratings = EXCLUDED.total_ratings,
  media_type = EXCLUDED.media_type;

-- 10. Compute UserStatsCache JSON for seed users
INSERT INTO public."UserStatsCache" (user_id, media_type, stats_json, updated_at)
SELECT
  u.id,
  m.m_type,
  json_build_object(
    'total_count', COALESCE(stat.total_count, 0),
    'average_score', COALESCE(stat.avg_score, 0),
    'highest_score', COALESCE(stat.max_score, 0),
    'lowest_score', COALESCE(stat.min_score, 0),
    'status_counts', json_build_object(
      'completed', COALESCE(ws.completed, 0),
      'watching', COALESCE(ws.watching, 0),
      'plan_to_watch', COALESCE(ws.planning, 0),
      'dropped', COALESCE(ws.dropped, 0)
    )
  ),
  NOW()
FROM public.users u
CROSS JOIN (VALUES ('MOVIE'::public."MediaType"), ('SHOW'::public."MediaType"), ('GAME'::public."MediaType"), ('MANGA'::public."MediaType")) AS m(m_type)
LEFT JOIN LATERAL (
  SELECT
    COUNT(*) as total_count,
    ROUND(AVG(score)) as avg_score,
    MAX(score) as max_score,
    MIN(score) as min_score
  FROM public.user_ratings r
  WHERE r.user_id = u.id AND (
    (m.m_type = 'MOVIE' AND r.media_id LIKE 'tmdb-movie-%') OR
    (m.m_type = 'SHOW' AND r.media_id LIKE 'tmdb-tv-%') OR
    (m.m_type = 'GAME' AND (r.media_id LIKE 'igdb-game-%' OR r.media_id LIKE 'rawg-game-%')) OR
    (m.m_type = 'MANGA' AND r.media_id LIKE 'mangadex-manga-%')
  )
) stat ON true
LEFT JOIN LATERAL (
  SELECT
    COUNT(*) FILTER (WHERE status = 'COMPLETED') as completed,
    COUNT(*) FILTER (WHERE status = 'IN_PROGRESS') as watching,
    COUNT(*) FILTER (WHERE status = 'PLANNING') as planning,
    COUNT(*) FILTER (WHERE status = 'DROPPED') as dropped
  FROM public.user_watchlist w
  WHERE w.user_id = u.id AND w.media_type = m.m_type
) ws ON true
WHERE u.id >= 'a1111111-1111-1111-1111-111111111111'
ON CONFLICT (user_id, media_type) DO UPDATE SET
  stats_json = EXCLUDED.stats_json,
  updated_at = EXCLUDED.updated_at;
