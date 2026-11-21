import { discoverMedia } from "@/app/actions";
import DiscoverFilters from "@/components/DiscoverFilters";
import DiscoverResults from "@/components/DiscoverResults";
import DiscoverTabs from "@/components/DiscoverTabs";

// Force Next.js to always fetch fresh Database scores instead of caching the page
export const dynamic = "force-dynamic";
export const revalidate = 0;

export default async function DiscoverPage({
  searchParams,
}: {
  searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
}) {
  const params = await searchParams;
  const first = (v: string | string[] | undefined) => (Array.isArray(v) ? v[0] : v) || "";
  const all = (v: string | string[] | undefined) => (Array.isArray(v) ? v : v ? [v] : []);
  const type = first(params.type) || "movie";
  const genre = all(params.genre).flatMap((g) => g.split(",")).filter(Boolean).join(",");
  const keywords = all(params.keyword).filter(Boolean);
  const year = first(params.year);
  const sort = first(params.sort) || "popular";

  const results = await discoverMedia(type, genre, year, sort, keywords);

  // Remount the grid whenever the filters change so paging starts over.
  const filterKey = JSON.stringify([type, genre, keywords, year, sort]);

  return (
    <main className="min-h-screen bg-gray-950 text-white pb-24 pt-12">
      <div className="max-w-7xl mx-auto px-8">
        <DiscoverTabs active="browse" />

        <DiscoverFilters />

        <DiscoverResults
          key={filterKey}
          initialItems={results}
          type={type}
          genre={genre}
          year={year}
          sort={sort}
          keywords={keywords}
        />
      </div>
    </main>
  );
}
