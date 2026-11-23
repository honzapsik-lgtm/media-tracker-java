import {
  Archive, BookOpen, Clapperboard, Compass, Crown, Flag, Gamepad2, Gavel, Heart, Layers, Library,
  ListOrdered, Microscope, Newspaper, PenLine, Skull, Sparkles, Star, Trophy, Tv, Users,
  type LucideIcon,
} from "lucide-react";

export interface BadgeDef {
  id: string;
  title: string;
  desc: string;
  Icon: LucideIcon;
}

/**
 * Every achievement. The unlock rules live in the backend (StatsService.badgeProgress) and in
 * scripts/seed/generate_seed.py; keep ids and thresholds in sync with both.
 */
export const BADGE_DICTIONARY: BadgeDef[] = [
  { id: "ratings_10", title: "Initiate", desc: "Rate 10 titles.", Icon: Sparkles },
  { id: "ratings_50", title: "Critic", desc: "Rate 50 titles.", Icon: Star },
  { id: "ratings_100", title: "The Completionist", desc: "Rate 100 titles.", Icon: Trophy },
  { id: "ratings_250", title: "Archivist", desc: "Rate 250 titles.", Icon: Archive },
  { id: "ratings_500", title: "Living Library", desc: "Rate 500 titles.", Icon: Library },
  { id: "movies_25", title: "Film Buff", desc: "Rate 25 movies.", Icon: Clapperboard },
  { id: "shows_25", title: "Binge Watcher", desc: "Rate 25 TV shows.", Icon: Tv },
  { id: "seasons_10", title: "Season by Season", desc: "Rate 10 individual seasons.", Icon: Layers },
  { id: "games_10", title: "Controller Freak", desc: "Rate 10 games.", Icon: Gamepad2 },
  { id: "manga_10", title: "Ink & Paper", desc: "Rate 10 manga.", Icon: BookOpen },
  { id: "all_rounder", title: "Renaissance Critic", desc: "Rate 5 movies, 5 shows, 5 games and 5 manga.", Icon: Compass },
  { id: "reviews_10", title: "Wordsmith", desc: "Write 10 reviews.", Icon: PenLine },
  { id: "reviews_50", title: "Columnist", desc: "Write 50 reviews.", Icon: Newspaper },
  { id: "deep_10", title: "Deep Diver", desc: "Give 10 advanced ratings with sub-scores.", Icon: Microscope },
  { id: "void_stare", title: "The Void Stare", desc: "Give a score of 20% or lower.", Icon: Skull },
  { id: "masterpiece", title: "True Kino", desc: "Give a perfect 100% score.", Icon: Crown },
  { id: "tough_crowd", title: "Tough Crowd", desc: "Keep an average of 55% or lower over 25+ ratings.", Icon: Gavel },
  { id: "golden_heart", title: "Golden Heart", desc: "Keep an average of 85% or higher over 25+ ratings.", Icon: Heart },
  { id: "social_10", title: "Social Butterfly", desc: "Have 10 friends.", Icon: Users },
  { id: "curator", title: "Curator", desc: "Create a ranked list.", Icon: ListOrdered },
  { id: "finisher", title: "Finisher", desc: "Complete 25 titles on your lists.", Icon: Flag },
];

export function findBadge(id: string | null | undefined): BadgeDef | undefined {
  return BADGE_DICTIONARY.find((b) => b.id === id);
}

/** The badge's line icon; renders nothing for unknown ids. */
export function BadgeIcon({ id, className = "w-5 h-5" }: { id: string; className?: string }) {
  const badge = findBadge(id);
  if (!badge) return null;
  const { Icon } = badge;
  return <Icon className={className} strokeWidth={1.75} aria-hidden="true" />;
}
