'use client';

import React from 'react';
import Link from 'next/link';
import Carousel from './Carousel';
import MediaCardVertical from './MediaCardVertical';

export interface TwinProfile {
  id: string;
  username: string;
  name: string;
  image: string | null;
  matchPercentage: number;
  sharedRatingsCount: number;
}

export interface TwinMediaRecommendation {
  id: string;
  title: string;
  image: string | null;
  type: string;
  releaseDate: string | null;
  communityScore: number | null;
  listRank: number | null;
  twinScore: number;
  twin: TwinProfile;
}

export interface ContentRecommendation {
  sourceMediaId: string;
  sourceTitle: string;
  sourceScore: number;
  recommendations: Array<{
    id: string;
    title: string;
    image: string | null;
    type: string;
    releaseDate: string | null;
    communityScore: number | null;
    listRank: number | null;
  }>;
}

export interface TwinTasteData {
  authenticated: boolean;
  hasRatings: boolean;
  userRatingsCount: number;
  topTwins: TwinProfile[];
  twinRecommendations: TwinMediaRecommendation[];
  contentRecommendation: ContentRecommendation | null;
}

export default function TwinTasteRecommendations({ data }: { data: TwinTasteData | null }) {
  if (!data) return null;

  // Unauthenticated banner
  if (!data.authenticated) {
    return (
      <div className="mb-12 rounded-2xl border border-indigo-900/40 bg-gradient-to-r from-indigo-950/40 via-purple-950/30 to-blue-950/40 p-6 backdrop-blur-md">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <h2 className="text-lg font-black text-white tracking-wide">Twin Taste Match</h2>
              <span className="text-[10px] font-black uppercase px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                Personalized
              </span>
            </div>
            <p className="text-sm text-gray-400">
              Sign in to unlock recommendations from users with matching taste and personalized suggestions based on your highest ratings.
            </p>
          </div>
          <Link
            href="/api/auth/signin"
            className="shrink-0 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-sm px-5 py-2.5 shadow-lg shadow-indigo-600/30 transition-all hover:scale-105 active:scale-95"
          >
            Sign In to Unlock
          </Link>
        </div>
      </div>
    );
  }

  // Authenticated but no ratings yet
  if (!data.hasRatings || data.userRatingsCount < 1) {
    return (
      <div className="mb-12 rounded-2xl border border-blue-900/40 bg-gradient-to-r from-blue-950/40 via-indigo-950/30 to-gray-900/50 p-6 backdrop-blur-md">
        <div className="flex items-center gap-2 mb-2">
          <h2 className="text-lg font-black text-white tracking-wide">Twin Taste Match</h2>
          <span className="text-[10px] font-black uppercase px-2 py-0.5 rounded bg-blue-500/20 text-blue-300 border border-blue-500/30">
            Awaiting Ratings
          </span>
        </div>
        <p className="text-sm text-gray-400 mb-3">
          Rate movies, TV shows, or games to discover your <strong className="text-gray-200">Taste Twins</strong>—other users whose ratings closely align with yours—and unlock their top recommendations!
        </p>
      </div>
    );
  }

  const hasTwins = data.topTwins && data.topTwins.length > 0;
  const hasTwinRecs = data.twinRecommendations && data.twinRecommendations.length > 0;
  const hasContentRec = data.contentRecommendation && data.contentRecommendation.recommendations.length > 0;

  if (!hasTwins && !hasTwinRecs && !hasContentRec) {
    return (
      <div className="mb-12 rounded-2xl border border-gray-800 bg-gray-900/50 p-6">
        <div className="flex items-center gap-2 mb-2">
          <h2 className="text-lg font-black text-white">Twin Taste Match</h2>
        </div>
        <p className="text-sm text-gray-400">
          You currently have {data.userRatingsCount} rating{data.userRatingsCount === 1 ? '' : 's'}. Rate a few more popular titles so we can match you with Taste Twins!
        </p>
      </div>
    );
  }

  return (
    <div className="mb-14 space-y-10">
      {/* Taste Twins Bar */}
      {hasTwins && (
        <div className="rounded-2xl border border-purple-900/30 bg-gradient-to-r from-purple-950/30 via-indigo-950/20 to-gray-900/40 p-5 backdrop-blur-md">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <h2 className="text-lg font-black text-white tracking-wide">Your Taste Twins</h2>
              <span className="text-xs text-gray-400">Users who share your viewing preferences</span>
            </div>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
            {data.topTwins.map((twin) => (
              <Link
                key={twin.id}
                href={`/user/${twin.username}`}
                className="flex items-center gap-3 p-3 rounded-xl bg-gray-900/70 hover:bg-gray-800/90 border border-gray-800/80 hover:border-purple-500/50 transition-all hover:scale-[1.02] group"
              >
                <img
                  src={twin.image || `https://api.dicebear.com/7.x/identicon/svg?seed=${twin.username}`}
                  alt={twin.name || twin.username}
                  className="w-10 h-10 rounded-full object-cover border border-purple-500/40"
                />
                <div className="min-w-0 flex-1">
                  <div className="font-bold text-sm text-white truncate group-hover:text-purple-300 transition-colors">
                    {twin.name || twin.username}
                  </div>
                  <div className="text-xs text-gray-400">@{twin.username}</div>
                </div>
                <div className="text-right shrink-0">
                  <div className="text-xs font-black px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 border border-purple-500/40">
                    {twin.matchPercentage}% match
                  </div>
                  <div className="text-[10px] text-gray-500 mt-0.5">{twin.sharedRatingsCount} shared</div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}

      {/* Recommendations from Taste Twins */}
      {hasTwinRecs && (
        <div>
          <div className="flex items-center gap-2 mb-4">
            <h2 className="text-2xl font-bold tracking-wide text-gray-100">Twin Taste Picks</h2>
            <span className="text-xs font-semibold text-purple-400 px-2 py-0.5 rounded bg-purple-950/60 border border-purple-800/50">
              Highly Rated by Users With Your Taste
            </span>
          </div>
          <Carousel>
            {data.twinRecommendations.map((rec) => (
              <div key={rec.id} className="flex-none w-40 sm:w-48 lg:w-56 snap-start flex flex-col">
                <div className="mb-2 flex items-center justify-between text-[11px] px-1 text-purple-300">
                  <span className="truncate font-semibold">@{rec.twin.username}</span>
                  <span className="font-bold text-emerald-400 shrink-0">★ {rec.twinScore}%</span>
                </div>
                <MediaCardVertical
                  item={{
                    id: rec.id,
                    title: rec.title,
                    type: rec.type,
                    image: rec.image,
                    releaseDate: rec.releaseDate,
                    communityScore: rec.communityScore,
                    listRank: rec.listRank,
                  }}
                />
              </div>
            ))}
          </Carousel>
        </div>
      )}

      {/* Because You Loved [Title] */}
      {hasContentRec && data.contentRecommendation && (
        <div>
          <div className="flex items-center gap-2 mb-4">
            <h2 className="text-2xl font-bold tracking-wide text-gray-100">
              Because You Loved <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-indigo-400">{data.contentRecommendation.sourceTitle}</span>
            </h2>
            <span className="text-xs font-semibold text-blue-400 px-2 py-0.5 rounded bg-blue-950/60 border border-blue-800/50">
              Rated {data.contentRecommendation.sourceScore}%
            </span>
          </div>
          <Carousel>
            {data.contentRecommendation.recommendations.map((item) => (
              <div key={item.id} className="flex-none w-40 sm:w-48 lg:w-56 snap-start">
                <MediaCardVertical
                  item={{
                    id: item.id,
                    title: item.title,
                    type: item.type,
                    image: item.image,
                    releaseDate: item.releaseDate,
                    communityScore: item.communityScore,
                    listRank: item.listRank,
                  }}
                />
              </div>
            ))}
          </Carousel>
        </div>
      )}
    </div>
  );
}
