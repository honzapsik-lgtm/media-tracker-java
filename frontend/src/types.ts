export interface MediaItem {
  id: string; // Ensure this is mostly used as a string
  title: string;
  type: string;
  image: string | null;
  releaseDate: string | null;
  communityScore?: number | null;
  listRank?: number | null;
  totalRatings?: number | null;
  originalLanguage?: string;
  genreIds?: number[];
  genres?: string[];
  keywords?: string[];
  origin?: string;
}

export interface MediaCredit {
  id: string | number;
  name: string;
  role: string;
  image: string | null;
  isCompany?: boolean;
}

export interface GameCharacter {
  id: number;
  name: string;
  description: string | null;
  imageUrl: string | null;
}
