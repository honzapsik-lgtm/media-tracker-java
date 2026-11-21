import Link from "next/link";

/** Switches between the filterable catalog (/discover) and personalized picks (/discover/for-you). */
export default function DiscoverTabs({ active }: { active: "browse" | "for-you" }) {
  const tab = (key: "browse" | "for-you", href: string, label: string, hint: string) => {
    const isActive = active === key;
    return (
      <Link
        href={href}
        aria-current={isActive ? "page" : undefined}
        className={`flex flex-col px-5 py-2.5 rounded-xl border transition-colors ${
          isActive
            ? "bg-blue-600 border-blue-500 text-white shadow-lg shadow-blue-600/20"
            : "bg-gray-900 border-gray-800 text-gray-300 hover:text-white hover:border-gray-600"
        }`}
      >
        <span className="text-sm font-black">{label}</span>
        <span className={`text-[11px] font-semibold ${isActive ? "text-blue-100" : "text-gray-500"}`}>{hint}</span>
      </Link>
    );
  };

  return (
    <div className="flex flex-wrap items-end justify-between gap-4 mb-8">
      <h1 className="text-4xl font-black">Discover</h1>
      <nav className="flex gap-2" aria-label="Discover sections">
        {tab("browse", "/discover", "Browse", "Filter by genre & keyword")}
        {tab("for-you", "/discover/for-you", "For You", "Based on your ratings")}
      </nav>
    </div>
  );
}
