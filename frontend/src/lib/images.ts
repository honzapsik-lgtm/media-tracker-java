const MANGADEX_COVER_PREFIX = "https://uploads.mangadex.org/covers/";

/**
 * Returns an <img>-safe URL for a media cover.
 *
 * MangaDex answers cover requests from browsers with a bot challenge page instead of the image,
 * so its covers are served through our own proxy (/api/covers/mangadex/...). Other URLs pass through.
 */
export function coverSrc(url: string): string;
export function coverSrc(url: string | null | undefined): string | undefined;
export function coverSrc(url: string | null | undefined): string | undefined {
  if (!url) return undefined;
  if (url.startsWith(MANGADEX_COVER_PREFIX)) {
    let path = url.slice(MANGADEX_COVER_PREFIX.length);
    // Originals can be several MB; MangaDex serves a 512px thumbnail at "<file>.512.jpg".
    if (!/\.(256|512)\.jpg$/.test(path)) path += ".512.jpg";
    return `/api/covers/mangadex/${path}`;
  }
  return url;
}
