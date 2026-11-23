"""
Builds scripts/seed/catalog.json: real movies, shows, games and manga (with genres) for the large seed.

Uses the running backend's discover API, so every title has a working media page.
Usage (backend must be running on localhost:8080):
    python scripts/seed/fetch_catalog.py
The gateway key is read from the media_backend container (never printed).
"""
import json
import os
import subprocess
import sys
import time
import urllib.parse
import urllib.request

BASE = "http://localhost:8080/api"
OUT = os.path.join(os.path.dirname(__file__), "catalog.json")

GENRES = {
    "movie": ["action", "adventure", "animation", "comedy", "crime", "documentary", "drama", "family", "fantasy",
              "history", "horror", "mystery", "romance", "scifi", "thriller", "war"],
    "show": ["actionadventure", "animation", "comedy", "crime", "documentary", "drama", "family", "mystery",
             "reality", "scififantasy", "warpolitics"],
    "game": ["action", "adventure", "rpg", "shooter", "strategy", "simulation", "puzzle", "racing", "sports",
             "fighting", "platformer", "indie"],
    "manga": ["action", "adventure", "comedy", "drama", "fantasy", "horror", "mystery", "psychological", "romance",
              "scifi", "slice", "sports", "supernatural"],
}
GENRE_PAGES = {"movie": 2, "show": 2, "game": 2, "manga": 1}
GENERAL_PAGES = {"movie": 10, "show": 8, "game": 8, "manga": 6}


def gateway_key() -> str:
    key = os.environ.get("INTERNAL_GATEWAY_SECRET")
    if key:
        return key
    return subprocess.check_output(["docker", "exec", "media_backend", "printenv", "INTERNAL_GATEWAY_SECRET"], text=True).strip()


KEY = gateway_key()


def get(path: str):
    req = urllib.request.Request(BASE + path, headers={"X-Internal-Gateway-Key": KEY})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=60) as res:
                return json.loads(res.read())
        except Exception as e:  # noqa: BLE001 - retry any network hiccup
            if attempt == 2:
                print(f"  ! {path}: {e}", file=sys.stderr)
                return None
            time.sleep(2)


def discover(media_type: str, sort: str, page: int, genre: str | None = None):
    q = {"type": media_type, "sort": sort, "page": page}
    if genre:
        q["genre"] = genre
    return get("/discover?" + urllib.parse.urlencode(q)) or []


def main():
    catalog: dict[str, dict] = {}

    def add(item, media_type, genre=None):
        if not item.get("id") or not item.get("title") or not item.get("image"):
            return
        entry = catalog.setdefault(item["id"], {
            "id": item["id"], "title": item["title"], "image": item["image"], "type": media_type,
            "releaseDate": item.get("releaseDate"), "globalScore": item.get("globalScore"), "genres": [],
            "popularity": 0,
        })
        if genre and genre not in entry["genres"]:
            entry["genres"].append(genre)
        entry["popularity"] += 1

    for media_type in GENRES:
        print(f"{media_type}: general pages")
        for sort in ("all_time", "popular"):
            for page in range(1, GENERAL_PAGES[media_type] + 1):
                for it in discover(media_type, sort, page):
                    add(it, media_type)
        for genre in GENRES[media_type]:
            print(f"{media_type}: {genre}")
            for page in range(1, GENRE_PAGES[media_type] + 1):
                for it in discover(media_type, "all_time", page, genre):
                    add(it, media_type, genre)

    # AniList manga ids resolve to a canonical MangaDex id on the media page; store that one so
    # ratings, stats and pages all agree.
    resolved: dict[str, dict] = {}
    for mid, entry in list(catalog.items()):
        if entry["type"] != "manga" or not mid.startswith("anilist-manga-"):
            resolved[mid] = entry
            continue
        details = get("/media/" + mid)
        time.sleep(0.25)
        if not details or not details.get("id") or not details.get("image"):
            continue
        canon = details["id"]
        entry = {**entry, "id": canon, "title": details.get("title") or entry["title"], "image": details["image"]}
        if canon in resolved:
            resolved[canon]["genres"] = sorted(set(resolved[canon]["genres"]) | set(entry["genres"]))
        else:
            resolved[canon] = entry

    items = sorted(resolved.values(), key=lambda e: (e["type"], -e["popularity"], e["title"]))
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(items, f, ensure_ascii=False, indent=1)
    counts = {t: sum(1 for i in items if i["type"] == t) for t in GENRES}
    print("wrote", OUT, counts)


if __name__ == "__main__":
    main()
