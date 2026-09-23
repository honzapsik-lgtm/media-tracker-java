import * as api from "@/lib/api-client";
import SearchResultsTabs from "@/components/SearchResultsTabs";
import Link from "next/link";
import { MediaItem } from "@/types";

export default async function SearchPage({
  searchParams,
}: {
  searchParams: Promise<{ q?: string }>;
}) {
  const resolvedParams = await searchParams;
  const query = resolvedParams.q || "";

  let combinedResults: MediaItem[] = [];
  if (query.trim()) {
    try {
      const results = await api.searchMedia(query.trim());
      combinedResults = (results || []).map((item: any) => ({
        id: item.id || "",
        title: item.title || "Untitled",
        type: item.type || "other",
        image: item.image || null,
        releaseDate: item.releaseDate || null,
        communityScore: typeof item.globalScore === "number" ? item.globalScore : null,
        listRank: null,
      }));
    } catch (e) {
      console.error("[SearchPage] Failed to fetch search results from Spring Boot:", e);
    }
  }

  const userResults: any[] = [];

  return (
    <main className="min-h-screen bg-gray-950 text-white p-8">
      <div className="max-w-7xl mx-auto">
        <div className="mb-8">
          <Link href="/" className="text-blue-400 hover:text-blue-300 font-semibold flex w-fit items-center gap-2 transition-colors">
            ← Back Home
          </Link>
        </div>

        <h1 className="text-2xl font-bold mb-6">
          Search Results for: <span className="text-blue-400">&ldquo;{query}&rdquo;</span>
        </h1>

        {combinedResults.length === 0 && userResults.length === 0 && query ? (
          <p className="text-gray-400">No results found.</p>
        ) : (
          <SearchResultsTabs results={combinedResults} userResults={userResults} />
        )}
      </div>
    </main>
  );
}
