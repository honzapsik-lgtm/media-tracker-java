import DiscoverTabs from "@/components/DiscoverTabs";
import TwinTasteRecommendations from "@/components/TwinTasteRecommendations";
import { getRecommendations } from "@/lib/api-client";

export const dynamic = "force-dynamic";
export const revalidate = 0;

export default async function ForYouPage() {
  const recommendations = await getRecommendations().catch(() => null);

  return (
    <main className="min-h-screen bg-gray-950 text-white pb-24 pt-12">
      <div className="max-w-7xl mx-auto px-8">
        <DiscoverTabs active="for-you" />
        {recommendations ? (
          <TwinTasteRecommendations data={recommendations} />
        ) : (
          <div className="text-center py-20 bg-gray-900/50 rounded-2xl border border-gray-800 border-dashed">
            <p className="text-gray-400 text-lg">Couldn&apos;t load your recommendations right now.</p>
          </div>
        )}
      </div>
    </main>
  );
}
