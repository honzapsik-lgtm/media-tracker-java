import * as api from "@/lib/api-client";
import SearchBar from "@/components/SearchBar";
import MediaRow from "@/components/MediaRow";
import FriendActivityFeed from "@/components/FriendActivityFeed";

export default async function Home() {
  const [movies, shows, games] = await Promise.all([
    api.discoverMedia("movie", "", "", "popular", 1).catch(() => []),
    api.discoverMedia("show", "", "", "popular", 1).catch(() => []),
    api.discoverMedia("game", "", "", "popular", 1).catch(() => []),
  ]);

  const mapToMediaItem = (items: any[], defaultType: string) =>
    (items || []).map((item: any) => ({
      id: item.id,
      title: item.title,
      type: item.type || defaultType,
      image: item.image,
      releaseDate: item.releaseDate,
      communityScore: typeof item.globalScore === "number" ? item.globalScore : null,
      listRank: null,
    }));

  const enhancedMovies = mapToMediaItem(movies, "movie");
  const enhancedShows = mapToMediaItem(shows, "show");
  const enhancedGames = mapToMediaItem(games, "game");

  return (
    <main className="min-h-screen bg-gray-950 text-white p-8">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16 pt-8">
          <h1 className="text-6xl font-extrabold leading-tight pb-2 mb-6 text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-purple-500 to-pink-500 tracking-tight">
            Media Tracker Hub
          </h1>
          <p className="text-gray-400 mb-8 max-w-2xl mx-auto text-lg">
            Your centralized hub for tracking the latest and greatest across movies, TV shows, games, and manga.
          </p>
        </div>

        <FriendActivityFeed />

        <MediaRow title="Trending Movies" items={enhancedMovies} />
        <MediaRow title="Trending TV Shows" items={enhancedShows} />
        <MediaRow title="Trending Games" items={enhancedGames} />
      </div>
    </main>
  );
}
