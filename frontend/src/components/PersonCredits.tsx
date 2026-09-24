import Link from "next/link";
import { UnifiedCredit } from "@/types/person";

interface PersonCreditsProps {
  initialCredits: { cast: UnifiedCredit[]; crew: UnifiedCredit[] };
  personSlug: string;
}

export default function PersonCredits({ initialCredits: credits }: PersonCreditsProps) {
  // Grouping credits by decade/year
  const groupCredits = (items: UnifiedCredit[]) => {
    const sorted = [...items].sort((a, b) => (b.releaseYear || 0) - (a.releaseYear || 0));
    return sorted.reduce((acc, c) => {
      const year = c.releaseYear || "Upcoming";
      if (!acc[year]) acc[year] = [];
      acc[year].push(c);
      return acc;
    }, {} as Record<string, UnifiedCredit[]>);
  };

  const sortYearsDesc = (a: string, b: string) => {
    if (a === "Upcoming") return -1;
    if (b === "Upcoming") return 1;
    return Number(b) - Number(a);
  };

  const castGroups = groupCredits(credits.cast);
  const crewGroups = groupCredits(credits.crew);

  const hasCredits = credits.cast.length > 0 || credits.crew.length > 0;

  if (!hasCredits) {
    return (
      <section>
        <div className="flex items-center gap-4 mb-10">
          <h2 className="text-3xl font-black text-white">Credits</h2>
          <div className="h-px flex-1 bg-gradient-to-r from-gray-800 to-transparent"></div>
        </div>
        <div className="text-gray-500 text-lg font-medium p-8 bg-gray-900/20 rounded-2xl border border-gray-800/50 text-center">
          No credits found.
        </div>
      </section>
    );
  }

  return (
    <section>
      <div className="flex items-center gap-4 mb-10">
        <h2 className="text-3xl font-black text-white">Credits</h2>
        <div className="h-px flex-1 bg-gradient-to-r from-gray-800 to-transparent"></div>
      </div>


      <div className="flex flex-col gap-16 w-full">
        {/* CAST */}
        {credits.cast.length > 0 && (
          <div className="w-full">
            <h3 className="text-2xl font-bold mb-6 flex items-center gap-3">
              <span className="bg-blue-500/10 text-blue-400 px-4 py-1.5 rounded-full text-sm tracking-widest uppercase border border-blue-500/20">Cast</span>
              <span className="text-gray-500 text-base">{credits.cast.length} Roles</span>
            </h3>
            
            <div className="space-y-6">
              {Object.entries(castGroups).sort(([a], [b]) => sortYearsDesc(a, b)).map(([year, yearCredits]) => (
                <div key={`cast-${year}`} className="space-y-3">
                  <div className="flex items-center gap-4 pt-4 pb-2">
                    <span className="text-gray-400 font-extrabold text-sm tracking-wider uppercase shrink-0">{year}</span>
                    <div className="h-[2px] flex-1 bg-gray-900"></div>
                  </div>
                  
                  <div className="space-y-3">
                    {yearCredits.map((c, i) => {
                      const isRoleResolving = c.role?.startsWith("[RESOLVING_ROLE]");
                      const displayRole = isRoleResolving 
                        ? (c.role.includes(":") ? c.role.split(":")[1] : "Developer") 
                        : c.role;

                      const cardContent = (
                        <>
                          {c.isVoiceRole && c.characterImage ? (
                            <img src={c.characterImage} alt={c.role} className="w-10 h-10 rounded-full object-cover border border-gray-700 shrink-0" />
                          ) : (
                            <div className="w-10 h-10 rounded-full bg-gray-800 border border-gray-700 shrink-0 flex items-center justify-center text-gray-500 text-xs font-bold">
                              {c.mediaType.slice(0, 2)}
                            </div>
                          )}
                          
                          <div className="flex-1 min-w-0 flex flex-col sm:flex-row sm:items-baseline gap-1 sm:gap-2">
                            <h4 className="font-bold text-base text-gray-200 truncate group-hover:text-white">{c.title}</h4>
                            <div className="flex items-center gap-2 shrink-0">
                              <span className="text-gray-600 text-xs hidden sm:inline">•</span>
                              {c.isVoiceRole && <span className="text-[9px] font-black uppercase bg-purple-500/10 text-purple-400 border border-purple-500/20 px-1.5 py-0.5 rounded shrink-0">Voice</span>}
                              
                              <span className="text-sm text-gray-400 truncate">{displayRole}</span>
                            </div>
                          </div>
                          
                          <span className="text-[10px] font-black uppercase tracking-wider px-2 py-1 rounded bg-gray-950 text-gray-500 border border-gray-800 shrink-0">
                            {c.mediaType}
                          </span>
                        </>
                      );

                      const className = "group bg-gray-900/40 hover:bg-gray-800/60 py-2.5 px-4 rounded-xl border border-gray-800/50 hover:border-blue-500/30 transition-colors flex items-center gap-4 w-full cursor-pointer";

                      if (c.mediaId) {
                        return (
                          <Link key={i} href={`/media/${c.mediaId}`} className={className}>
                            {cardContent}
                          </Link>
                        );
                      }

                      return (
                        <div key={i} className={className}>
                          {cardContent}
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* CREW */}
        {credits.crew.length > 0 && (
          <div className="w-full">
            <h3 className="text-2xl font-bold mb-6 flex items-center gap-3">
              <span className="bg-emerald-500/10 text-emerald-400 px-4 py-1.5 rounded-full text-sm tracking-widest uppercase border border-emerald-500/20">Crew</span>
              <span className="text-gray-500 text-base">{credits.crew.length} Credits</span>
            </h3>
            
            <div className="space-y-6">
              {Object.entries(crewGroups).sort(([a], [b]) => sortYearsDesc(a, b)).map(([year, yearCredits]) => (
                <div key={`crew-${year}`} className="space-y-3">
                  <div className="flex items-center gap-4 pt-4 pb-2">
                    <span className="text-gray-400 font-extrabold text-sm tracking-wider uppercase shrink-0">{year}</span>
                    <div className="h-[2px] flex-1 bg-gray-900"></div>
                  </div>
                  
                  <div className="space-y-3">
                    {yearCredits.map((c, i) => {
                      const isRoleResolving = c.role?.startsWith("[RESOLVING_ROLE]");
                      const displayRole = isRoleResolving 
                        ? (c.role.includes(":") ? c.role.split(":")[1] : "Developer") 
                        : c.role;

                      const cardContent = (
                        <>
                          <div className="flex-1 min-w-0 flex flex-col sm:flex-row sm:items-baseline gap-1 sm:gap-2">
                            <h4 className="font-bold text-base text-gray-200 truncate group-hover:text-white">{c.title}</h4>
                            <span className="text-gray-600 text-xs hidden sm:inline">•</span>
                            
                            <p className="text-sm text-gray-400 truncate">{displayRole}</p>
                          </div>
                          
                          <span className="text-[10px] font-black uppercase tracking-wider px-2 py-1 rounded bg-gray-950 text-gray-500 border border-gray-800 shrink-0">
                            {c.mediaType}
                          </span>
                        </>
                      );

                      const className = "group bg-gray-900/40 hover:bg-gray-800/60 py-2.5 px-4 rounded-xl border border-gray-800/50 hover:border-blue-500/30 transition-colors flex items-center gap-4 w-full cursor-pointer";

                      if (c.mediaId) {
                        return (
                          <Link key={i} href={`/media/${c.mediaId}`} className={className}>
                            {cardContent}
                          </Link>
                        );
                      }

                      return (
                        <div key={i} className={className}>
                          {cardContent}
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
