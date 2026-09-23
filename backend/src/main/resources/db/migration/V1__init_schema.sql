--
-- PostgreSQL database dump
--


-- Dumped from database version 15.18
-- Dumped by pg_dump version 15.18

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

-- *not* creating schema, since initdb creates it


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS '';


--
-- Name: ActivityType; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."ActivityType" AS ENUM (
    'RATED_MEDIA',
    'WATCHLIST_STATUS',
    'EPISODES_WATCHED',
    'CHAPTERS_READ',
    'VOLUMES_READ',
    'HOURS_PLAYED',
    'FAVORITED',
    'CUSTOM'
);


--
-- Name: FriendshipStatus; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."FriendshipStatus" AS ENUM (
    'PENDING',
    'ACCEPTED',
    'DECLINED',
    'BLOCKED'
);


--
-- Name: MediaType; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."MediaType" AS ENUM (
    'SHOW',
    'MOVIE',
    'GAME',
    'MANGA',
    'OTHER',
    'SEASON',
    'EPISODE'
);


--
-- Name: VisibilityLevel; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."VisibilityLevel" AS ENUM (
    'PUBLIC',
    'FRIENDS_ONLY',
    'PRIVATE'
);


--
-- Name: WatchlistStatus; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public."WatchlistStatus" AS ENUM (
    'PLANNING',
    'IN_PROGRESS',
    'COMPLETED',
    'ON_HOLD',
    'DROPPED'
);


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: Account; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Account" (
    id text NOT NULL,
    "userId" uuid NOT NULL,
    type text NOT NULL,
    provider text NOT NULL,
    "providerAccountId" text NOT NULL,
    refresh_token text,
    access_token text,
    expires_at integer,
    token_type text,
    scope text,
    id_token text,
    session_state text
);


--
-- Name: ApiCache; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."ApiCache" (
    id text NOT NULL,
    provider text NOT NULL,
    data jsonb NOT NULL,
    created_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at timestamp(3) without time zone NOT NULL
);


--
-- Name: BackgroundJob; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BackgroundJob" (
    id text NOT NULL,
    type text NOT NULL,
    payload jsonb NOT NULL,
    status text DEFAULT 'pending'::text NOT NULL,
    dedupe_key text,
    attempts integer DEFAULT 0 NOT NULL,
    max_attempts integer DEFAULT 3 NOT NULL,
    run_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    locked_at timestamp(3) without time zone,
    locked_by text,
    last_error text,
    created_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp(3) without time zone NOT NULL,
    processed_at timestamp(3) without time zone
);


--
-- Name: Session; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Session" (
    id text NOT NULL,
    "sessionToken" text NOT NULL,
    "userId" uuid NOT NULL,
    expires timestamp(3) without time zone NOT NULL
);


--
-- Name: SystemLog; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."SystemLog" (
    id text NOT NULL,
    level text NOT NULL,
    event text NOT NULL,
    message text,
    "requestId" text,
    "userId" text,
    "mediaId" text,
    "mediaType" text,
    "jobId" text,
    "durationMs" integer,
    metadata jsonb,
    "errorName" text,
    "errorMessage" text,
    "errorStack" text,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: UserStatsCache; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."UserStatsCache" (
    user_id uuid NOT NULL,
    stats_json jsonb NOT NULL,
    updated_at timestamp(3) without time zone NOT NULL,
    media_type public."MediaType" NOT NULL
);


--
-- Name: VerificationToken; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."VerificationToken" (
    identifier text NOT NULL,
    token text NOT NULL,
    expires timestamp(3) without time zone NOT NULL
);



--
-- Name: activity_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.activity_log (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    media_id text NOT NULL,
    increment text NOT NULL,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: companies; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.companies (
    id text NOT NULL,
    "tmdbId" integer,
    "anilistId" integer,
    "igdbId" integer,
    "tmdbNetworkId" integer,
    name text NOT NULL,
    description text,
    "logoUrl" text,
    country text,
    "mergedWorks" jsonb,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: episodes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.episodes (
    id text NOT NULL,
    "anilistId" integer,
    "tmdbId" integer,
    "episodeData" jsonb,
    "mediaId" text NOT NULL
);


--
-- Name: friendships; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.friendships (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    sender_id uuid NOT NULL,
    receiver_id uuid NOT NULL,
    status public."FriendshipStatus" DEFAULT 'PENDING'::public."FriendshipStatus" NOT NULL,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: global_rankings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.global_rankings (
    media_id text NOT NULL,
    media_type public."MediaType" NOT NULL,
    elo_score double precision DEFAULT 1200.0 NOT NULL,
    rank integer
);


--
-- Name: media; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.media (
    id text NOT NULL,
    "anilistId" integer,
    title text,
    type public."MediaType" NOT NULL,
    "isMainStoryline" boolean DEFAULT false NOT NULL,
    "releaseDate" text,
    "tmdbId" integer,
    "igdbId" integer,
    "mangadexId" text,
    "malId" integer,
    "themeData" jsonb,
    "watchData" jsonb,
    "episodeData" jsonb,
    "relatedMediaId" text,
    "staffData" jsonb,
    "castData" jsonb,
    "studioData" jsonb,
    "franchiseSyncedAt" timestamp(3) without time zone
);


--
-- Name: media_stats; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.media_stats (
    id text NOT NULL,
    community_average numeric DEFAULT 0,
    total_ratings integer DEFAULT 0,
    media_type public."MediaType" NOT NULL
);


--
-- Name: people; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.people (
    id text NOT NULL,
    "tmdbId" integer,
    "anilistId" integer,
    "igdbId" integer,
    "malId" integer,
    name text NOT NULL,
    "nativeName" text,
    biography text,
    "profileImage" text,
    "birthDate" text,
    "deathDate" text,
    "knownForDepartment" text,
    "mergedCredits" jsonb,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "rawgId" integer,
    "mangadexId" text
);


--
-- Name: seasons; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.seasons (
    id text NOT NULL,
    "anilistId" integer,
    "tmdbId" integer,
    "episodeData" jsonb,
    "mediaId" text NOT NULL,
    "releaseDate" text,
    "staffData" jsonb,
    "castData" jsonb,
    "studioData" jsonb,
    "themeData" jsonb
);


--
-- Name: user_activities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_activities (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    type public."ActivityType" NOT NULL,
    media_id text,
    media_title text,
    media_image text,
    media_type public."MediaType",
    data jsonb DEFAULT '{}'::jsonb,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: user_badges; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_badges (
    user_id uuid NOT NULL,
    badge_id text NOT NULL,
    unlocked_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: user_friend_preferences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_friend_preferences (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    friend_id uuid NOT NULL,
    hide_activity boolean DEFAULT false NOT NULL,
    hide_ratings boolean DEFAULT false NOT NULL,
    updated_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: user_list_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_list_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    list_id uuid NOT NULL,
    media_id text NOT NULL,
    media_title text,
    media_image text,
    rank_position integer NOT NULL
);


--
-- Name: user_lists; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_lists (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    title text NOT NULL,
    description text,
    media_type public."MediaType" NOT NULL,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: user_privacy_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_privacy_settings (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    profile_visibility public."VisibilityLevel" DEFAULT 'PUBLIC'::public."VisibilityLevel" NOT NULL,
    ratings_visibility public."VisibilityLevel" DEFAULT 'PUBLIC'::public."VisibilityLevel" NOT NULL,
    watchlist_visibility public."VisibilityLevel" DEFAULT 'PUBLIC'::public."VisibilityLevel" NOT NULL,
    activity_visibility public."VisibilityLevel" DEFAULT 'PUBLIC'::public."VisibilityLevel" NOT NULL,
    updated_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: user_ratings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_ratings (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    media_id text NOT NULL,
    media_title text,
    media_image text,
    score integer NOT NULL,
    review_text text,
    username text,
    avatar_url text,
    criteria_scores jsonb DEFAULT '{}'::jsonb,
    is_deep_review boolean DEFAULT false,
    rank_position integer,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    media_release_date text
);


--
-- Name: user_watchlist; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_watchlist (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    media_id text NOT NULL,
    media_title text,
    media_image text,
    status public."WatchlistStatus" DEFAULT 'PLANNING'::public."WatchlistStatus" NOT NULL,
    added_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP,
    "chaptersRead" integer DEFAULT 0 NOT NULL,
    "episodesWatched" integer DEFAULT 0 NOT NULL,
    "volumesRead" integer DEFAULT 0 NOT NULL,
    media_type public."MediaType",
    started_at timestamp(6) with time zone,
    finished_at timestamp(6) with time zone,
    is_rewatching boolean DEFAULT false NOT NULL,
    is_rereading boolean DEFAULT false NOT NULL,
    "hoursPlayed" double precision DEFAULT 0.0 NOT NULL,
    platform text,
    "watchCount" integer DEFAULT 0 NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name text,
    email text,
    "emailVerified" timestamp(3) without time zone,
    image text,
    created_at timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP,
    "realName" text,
    "stateRegion" text,
    country text,
    "showcaseBadges" text[] DEFAULT ARRAY[]::text[],
    role text DEFAULT 'user'::text NOT NULL,
    username text
);


--
-- Name: Account Account_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Account"
    ADD CONSTRAINT "Account_pkey" PRIMARY KEY (id);


--
-- Name: ApiCache ApiCache_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."ApiCache"
    ADD CONSTRAINT "ApiCache_pkey" PRIMARY KEY (id);


--
-- Name: BackgroundJob BackgroundJob_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BackgroundJob"
    ADD CONSTRAINT "BackgroundJob_pkey" PRIMARY KEY (id);


--
-- Name: Session Session_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Session"
    ADD CONSTRAINT "Session_pkey" PRIMARY KEY (id);


--
-- Name: SystemLog SystemLog_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."SystemLog"
    ADD CONSTRAINT "SystemLog_pkey" PRIMARY KEY (id);



--
-- Name: activity_log activity_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.activity_log
    ADD CONSTRAINT activity_log_pkey PRIMARY KEY (id);


--
-- Name: companies companies_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT companies_pkey PRIMARY KEY (id);


--
-- Name: episodes episodes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.episodes
    ADD CONSTRAINT episodes_pkey PRIMARY KEY (id);


--
-- Name: friendships friendships_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.friendships
    ADD CONSTRAINT friendships_pkey PRIMARY KEY (id);


--
-- Name: global_rankings global_rankings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.global_rankings
    ADD CONSTRAINT global_rankings_pkey PRIMARY KEY (media_id);


--
-- Name: media media_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.media
    ADD CONSTRAINT media_pkey PRIMARY KEY (id);


--
-- Name: media_stats media_stats_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.media_stats
    ADD CONSTRAINT media_stats_pkey PRIMARY KEY (id);


--
-- Name: people people_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.people
    ADD CONSTRAINT people_pkey PRIMARY KEY (id);


--
-- Name: seasons seasons_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.seasons
    ADD CONSTRAINT seasons_pkey PRIMARY KEY (id);


--
-- Name: user_activities user_activities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_activities
    ADD CONSTRAINT user_activities_pkey PRIMARY KEY (id);


--
-- Name: user_badges user_badges_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_badges
    ADD CONSTRAINT user_badges_pkey PRIMARY KEY (user_id, badge_id);


--
-- Name: user_friend_preferences user_friend_preferences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_friend_preferences
    ADD CONSTRAINT user_friend_preferences_pkey PRIMARY KEY (id);


--
-- Name: user_list_items user_list_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_list_items
    ADD CONSTRAINT user_list_items_pkey PRIMARY KEY (id);


--
-- Name: user_lists user_lists_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_lists
    ADD CONSTRAINT user_lists_pkey PRIMARY KEY (id);


--
-- Name: user_privacy_settings user_privacy_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_privacy_settings
    ADD CONSTRAINT user_privacy_settings_pkey PRIMARY KEY (id);


--
-- Name: user_ratings user_ratings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_ratings
    ADD CONSTRAINT user_ratings_pkey PRIMARY KEY (id);


--
-- Name: user_watchlist user_watchlist_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_watchlist
    ADD CONSTRAINT user_watchlist_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: Account_provider_providerAccountId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "Account_provider_providerAccountId_key" ON public."Account" USING btree (provider, "providerAccountId");


--
-- Name: BackgroundJob_created_at_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "BackgroundJob_created_at_idx" ON public."BackgroundJob" USING btree (created_at);


--
-- Name: BackgroundJob_dedupe_key_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "BackgroundJob_dedupe_key_idx" ON public."BackgroundJob" USING btree (dedupe_key);


--
-- Name: BackgroundJob_locked_at_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "BackgroundJob_locked_at_idx" ON public."BackgroundJob" USING btree (locked_at);


--
-- Name: BackgroundJob_status_run_at_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "BackgroundJob_status_run_at_idx" ON public."BackgroundJob" USING btree (status, run_at);


--
-- Name: BackgroundJob_type_status_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "BackgroundJob_type_status_idx" ON public."BackgroundJob" USING btree (type, status);


--
-- Name: Session_sessionToken_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "Session_sessionToken_key" ON public."Session" USING btree ("sessionToken");


--
-- Name: SystemLog_createdAt_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "SystemLog_createdAt_idx" ON public."SystemLog" USING btree ("createdAt");


--
-- Name: SystemLog_event_createdAt_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "SystemLog_event_createdAt_idx" ON public."SystemLog" USING btree (event, "createdAt");


--
-- Name: SystemLog_jobId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "SystemLog_jobId_idx" ON public."SystemLog" USING btree ("jobId");


--
-- Name: SystemLog_level_createdAt_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "SystemLog_level_createdAt_idx" ON public."SystemLog" USING btree (level, "createdAt");


--
-- Name: SystemLog_mediaId_createdAt_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "SystemLog_mediaId_createdAt_idx" ON public."SystemLog" USING btree ("mediaId", "createdAt");


--
-- Name: SystemLog_requestId_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "SystemLog_requestId_idx" ON public."SystemLog" USING btree ("requestId");


--
-- Name: SystemLog_userId_createdAt_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX "SystemLog_userId_createdAt_idx" ON public."SystemLog" USING btree ("userId", "createdAt");


--
-- Name: UserStatsCache_user_id_media_type_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "UserStatsCache_user_id_media_type_key" ON public."UserStatsCache" USING btree (user_id, media_type);


--
-- Name: VerificationToken_identifier_token_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "VerificationToken_identifier_token_key" ON public."VerificationToken" USING btree (identifier, token);


--
-- Name: VerificationToken_token_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "VerificationToken_token_key" ON public."VerificationToken" USING btree (token);


--
-- Name: activity_log_user_id_created_at_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX activity_log_user_id_created_at_idx ON public.activity_log USING btree (user_id, created_at);


--
-- Name: background_job_dedupe_active_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX background_job_dedupe_active_idx ON public."BackgroundJob" USING btree (dedupe_key) WHERE ((dedupe_key IS NOT NULL) AND (status = ANY (ARRAY['pending'::text, 'processing'::text])));


--
-- Name: companies_anilistId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "companies_anilistId_key" ON public.companies USING btree ("anilistId");


--
-- Name: companies_igdbId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "companies_igdbId_key" ON public.companies USING btree ("igdbId");


--
-- Name: companies_tmdbId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "companies_tmdbId_key" ON public.companies USING btree ("tmdbId");


--
-- Name: companies_tmdbNetworkId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "companies_tmdbNetworkId_key" ON public.companies USING btree ("tmdbNetworkId");


--
-- Name: episodes_anilistId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "episodes_anilistId_key" ON public.episodes USING btree ("anilistId");


--
-- Name: episodes_tmdbId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "episodes_tmdbId_key" ON public.episodes USING btree ("tmdbId");


--
-- Name: friendships_receiver_id_status_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX friendships_receiver_id_status_idx ON public.friendships USING btree (receiver_id, status);


--
-- Name: friendships_sender_id_receiver_id_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX friendships_sender_id_receiver_id_key ON public.friendships USING btree (sender_id, receiver_id);


--
-- Name: friendships_sender_id_status_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX friendships_sender_id_status_idx ON public.friendships USING btree (sender_id, status);


--
-- Name: global_rankings_media_type_elo_score_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX global_rankings_media_type_elo_score_idx ON public.global_rankings USING btree (media_type, elo_score DESC);


--
-- Name: media_anilistId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "media_anilistId_key" ON public.media USING btree ("anilistId");


--
-- Name: media_igdbId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "media_igdbId_key" ON public.media USING btree ("igdbId");


--
-- Name: media_malId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "media_malId_key" ON public.media USING btree ("malId");


--
-- Name: media_mangadexId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "media_mangadexId_key" ON public.media USING btree ("mangadexId");


--
-- Name: media_tmdbId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "media_tmdbId_key" ON public.media USING btree ("tmdbId");


--
-- Name: people_anilistId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "people_anilistId_key" ON public.people USING btree ("anilistId");


--
-- Name: people_igdbId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "people_igdbId_key" ON public.people USING btree ("igdbId");


--
-- Name: people_malId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "people_malId_key" ON public.people USING btree ("malId");


--
-- Name: people_mangadexId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "people_mangadexId_key" ON public.people USING btree ("mangadexId");


--
-- Name: people_rawgId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "people_rawgId_key" ON public.people USING btree ("rawgId");


--
-- Name: people_tmdbId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "people_tmdbId_key" ON public.people USING btree ("tmdbId");


--
-- Name: seasons_anilistId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "seasons_anilistId_key" ON public.seasons USING btree ("anilistId");


--
-- Name: seasons_tmdbId_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "seasons_tmdbId_key" ON public.seasons USING btree ("tmdbId");


--
-- Name: user_activities_media_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_activities_media_id_idx ON public.user_activities USING btree (media_id);


--
-- Name: user_activities_user_id_created_at_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_activities_user_id_created_at_idx ON public.user_activities USING btree (user_id, created_at DESC);


--
-- Name: user_friend_preferences_user_id_friend_id_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX user_friend_preferences_user_id_friend_id_key ON public.user_friend_preferences USING btree (user_id, friend_id);


--
-- Name: user_friend_preferences_user_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_friend_preferences_user_id_idx ON public.user_friend_preferences USING btree (user_id);


--
-- Name: user_list_items_list_id_media_id_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX user_list_items_list_id_media_id_key ON public.user_list_items USING btree (list_id, media_id);


--
-- Name: user_list_items_list_id_rank_position_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_list_items_list_id_rank_position_idx ON public.user_list_items USING btree (list_id, rank_position);


--
-- Name: user_lists_user_id_media_type_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX user_lists_user_id_media_type_idx ON public.user_lists USING btree (user_id, media_type);


--
-- Name: user_privacy_settings_user_id_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX user_privacy_settings_user_id_key ON public.user_privacy_settings USING btree (user_id);


--
-- Name: user_ratings_user_id_media_id_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX user_ratings_user_id_media_id_key ON public.user_ratings USING btree (user_id, media_id);


--
-- Name: user_watchlist_user_id_media_id_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX user_watchlist_user_id_media_id_key ON public.user_watchlist USING btree (user_id, media_id);


--
-- Name: users_email_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX users_email_key ON public.users USING btree (email);


--
-- Name: users_username_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX users_username_key ON public.users USING btree (username);


--
-- Name: Account Account_userId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Account"
    ADD CONSTRAINT "Account_userId_fkey" FOREIGN KEY ("userId") REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: Session Session_userId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Session"
    ADD CONSTRAINT "Session_userId_fkey" FOREIGN KEY ("userId") REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: UserStatsCache UserStatsCache_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."UserStatsCache"
    ADD CONSTRAINT "UserStatsCache_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: activity_log activity_log_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.activity_log
    ADD CONSTRAINT activity_log_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: episodes episodes_mediaId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.episodes
    ADD CONSTRAINT "episodes_mediaId_fkey" FOREIGN KEY ("mediaId") REFERENCES public.media(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: friendships friendships_receiver_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.friendships
    ADD CONSTRAINT friendships_receiver_id_fkey FOREIGN KEY (receiver_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: friendships friendships_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.friendships
    ADD CONSTRAINT friendships_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: media media_relatedMediaId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.media
    ADD CONSTRAINT "media_relatedMediaId_fkey" FOREIGN KEY ("relatedMediaId") REFERENCES public.media(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: seasons seasons_mediaId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.seasons
    ADD CONSTRAINT "seasons_mediaId_fkey" FOREIGN KEY ("mediaId") REFERENCES public.media(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_activities user_activities_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_activities
    ADD CONSTRAINT user_activities_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_badges user_badges_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_badges
    ADD CONSTRAINT user_badges_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_friend_preferences user_friend_preferences_friend_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_friend_preferences
    ADD CONSTRAINT user_friend_preferences_friend_id_fkey FOREIGN KEY (friend_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_friend_preferences user_friend_preferences_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_friend_preferences
    ADD CONSTRAINT user_friend_preferences_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_list_items user_list_items_list_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_list_items
    ADD CONSTRAINT user_list_items_list_id_fkey FOREIGN KEY (list_id) REFERENCES public.user_lists(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_lists user_lists_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_lists
    ADD CONSTRAINT user_lists_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_privacy_settings user_privacy_settings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_privacy_settings
    ADD CONSTRAINT user_privacy_settings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: user_ratings user_ratings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_ratings
    ADD CONSTRAINT user_ratings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_watchlist user_watchlist_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_watchlist
    ADD CONSTRAINT user_watchlist_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: -
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;


--
-- PostgreSQL database dump complete
--


