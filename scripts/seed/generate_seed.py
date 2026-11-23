"""
Generates scripts/seed/seed-large.sql: a large, believable community for trying the app.

  * ~300 users from many countries, each with a taste profile (favourite media types and
    genres, generous or harsh), so Taste Twins and recommendations have real signal
  * ~20k ratings, with written reviews and deep (sub-score) reviews
  * watchlists with progress, custom ranked lists (these feed the global List Rank),
    friendships (including with your own account), activity feed, badges, stats caches

Deterministic: the same catalog gives the same data (timestamps are relative to today). Run fetch_catalog.py first.
Usage:
    python scripts/seed/generate_seed.py [--friend-of <username>]
"""
import argparse
import json
import math
import os
import random
import unicodedata
import uuid
from datetime import datetime, timedelta, timezone

HERE = os.path.dirname(__file__)
CATALOG = os.path.join(HERE, "catalog.json")
OUT = os.path.join(HERE, "seed-large.sql")

NUM_USERS = 300
NOW = datetime.now(timezone.utc).replace(microsecond=0)
NS = uuid.UUID("6f1c3a52-6a51-4b8e-9d0e-5eed00000000")

rng = random.Random(20261123)

# ---------------------------------------------------------------- people

COUNTRIES = [
    # (country, weight, regions, first names (m), first names (f), last names)
    ("United States", 18, ["California", "Texas", "New York", "Washington", "Illinois", "Florida", "Oregon", "Colorado", "Massachusetts"],
     ["James", "Michael", "Ethan", "Noah", "Logan", "Ryan", "Tyler", "Jordan", "Caleb", "Marcus", "Dylan", "Austin"],
     ["Emily", "Olivia", "Madison", "Ava", "Hannah", "Chloe", "Abigail", "Taylor", "Brooke", "Kayla", "Megan", "Riley"],
     ["Smith", "Johnson", "Brown", "Miller", "Davis", "Wilson", "Anderson", "Thomas", "Moore", "Jackson", "Martin", "Lee", "Harris", "Clark", "Lewis", "Walker", "Young", "Hall"]),
    ("United Kingdom", 8, ["Greater London", "Manchester", "Scotland", "Wales", "Yorkshire", "Bristol"],
     ["Oliver", "Harry", "George", "Jack", "Charlie", "Alfie", "Callum", "Liam"],
     ["Amelia", "Isla", "Poppy", "Freya", "Sophie", "Grace", "Ella", "Holly"],
     ["Taylor", "Evans", "Roberts", "Wright", "Hughes", "Edwards", "Green", "Hall", "Wood", "Turner", "Cooper", "Morris"]),
    ("Czech Republic", 9, ["Prague", "South Moravian", "Moravian-Silesian", "Pilsen", "Olomouc", "Liberec"],
     ["Jan", "Jakub", "Tomáš", "Lukáš", "Matěj", "Ondřej", "Filip", "Vojtěch", "David", "Adam"],
     ["Tereza", "Eliška", "Anna", "Kateřina", "Lucie", "Barbora", "Klára", "Natálie", "Veronika", "Adéla"],
     ["Novák", "Svoboda", "Dvořák", "Černý", "Procházka", "Kučera", "Veselý", "Horák", "Němec", "Marek", "Pokorný", "Král"]),
    ("Germany", 7, ["Berlin", "Bavaria", "Hamburg", "Saxony", "Hesse", "North Rhine-Westphalia"],
     ["Lukas", "Leon", "Finn", "Jonas", "Paul", "Felix", "Max", "Niklas"],
     ["Lena", "Mia", "Hannah", "Lea", "Laura", "Marie", "Johanna", "Clara"],
     ["Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Wagner", "Becker", "Hoffmann", "Schulz", "Koch"]),
    ("Japan", 8, ["Tokyo", "Osaka", "Kyoto", "Hokkaido", "Fukuoka", "Kanagawa"],
     ["Haruto", "Ren", "Daiki", "Sota", "Yuto", "Kaito", "Takumi", "Riku"],
     ["Yui", "Hina", "Aoi", "Sakura", "Mei", "Rin", "Yuna", "Aiko"],
     ["Sato", "Suzuki", "Takahashi", "Tanaka", "Watanabe", "Ito", "Yamamoto", "Nakamura", "Kobayashi", "Kato"]),
    ("France", 5, ["Île-de-France", "Provence", "Brittany", "Occitanie", "Auvergne-Rhône-Alpes"],
     ["Lucas", "Hugo", "Louis", "Théo", "Arthur", "Jules", "Raphaël"],
     ["Léa", "Manon", "Camille", "Chloé", "Inès", "Jade", "Louise"],
     ["Martin", "Bernard", "Dubois", "Thomas", "Robert", "Petit", "Durand", "Leroy", "Moreau", "Laurent"]),
    ("Poland", 5, ["Mazovia", "Lesser Poland", "Silesia", "Pomerania", "Greater Poland"],
     ["Jakub", "Kacper", "Szymon", "Mateusz", "Filip", "Bartosz", "Wojciech"],
     ["Zuzanna", "Julia", "Maja", "Zofia", "Natalia", "Aleksandra", "Wiktoria"],
     ["Nowak", "Kowalski", "Wiśniewski", "Wójcik", "Kamiński", "Lewandowski", "Zieliński", "Szymański"]),
    ("Slovakia", 3, ["Bratislava", "Košice", "Žilina", "Prešov"],
     ["Martin", "Peter", "Michal", "Samuel", "Patrik", "Marek"],
     ["Simona", "Zuzana", "Petra", "Lenka", "Ivana", "Martina"],
     ["Horváth", "Kováč", "Varga", "Tóth", "Nagy", "Baláž", "Szabó", "Molnár"]),
    ("Brazil", 5, ["São Paulo", "Rio de Janeiro", "Minas Gerais", "Bahia", "Paraná"],
     ["Gabriel", "Lucas", "Matheus", "Pedro", "Rafael", "Gustavo", "Felipe"],
     ["Ana", "Beatriz", "Larissa", "Juliana", "Camila", "Fernanda", "Mariana"],
     ["Silva", "Santos", "Oliveira", "Souza", "Lima", "Pereira", "Costa", "Rodrigues", "Almeida"]),
    ("Canada", 4, ["Ontario", "Quebec", "British Columbia", "Alberta"],
     ["Liam", "Owen", "Nathan", "Connor", "Evan", "Jacob"],
     ["Charlotte", "Emma", "Avery", "Maya", "Zoe", "Sarah"],
     ["Tremblay", "Roy", "Gagnon", "MacDonald", "Campbell", "Wong", "Fraser", "Bouchard"]),
    ("Sweden", 3, ["Stockholm", "Västra Götaland", "Skåne", "Uppsala"],
     ["Erik", "Oscar", "William", "Hugo", "Elias", "Axel"],
     ["Elsa", "Alice", "Maja", "Saga", "Ebba", "Wilma"],
     ["Andersson", "Johansson", "Karlsson", "Nilsson", "Eriksson", "Larsson", "Lindqvist", "Berg"]),
    ("Spain", 4, ["Madrid", "Catalonia", "Andalusia", "Valencia", "Basque Country"],
     ["Pablo", "Alejandro", "Daniel", "Javier", "Adrián", "Sergio"],
     ["Lucía", "María", "Paula", "Carmen", "Sara", "Elena"],
     ["García", "Fernández", "González", "Rodríguez", "López", "Martínez", "Sánchez", "Pérez", "Romero"]),
    ("Italy", 4, ["Lombardy", "Lazio", "Campania", "Tuscany", "Piedmont"],
     ["Marco", "Luca", "Matteo", "Alessandro", "Lorenzo", "Davide"],
     ["Giulia", "Chiara", "Sofia", "Francesca", "Martina", "Alessia"],
     ["Rossi", "Russo", "Ferrari", "Esposito", "Bianchi", "Romano", "Colombo", "Ricci", "Marino"]),
    ("South Korea", 3, ["Seoul", "Busan", "Incheon", "Gyeonggi"],
     ["Minjun", "Seojun", "Jiho", "Hyunwoo", "Dohyun"],
     ["Seoyeon", "Jiwoo", "Haeun", "Minji", "Yuna"],
     ["Kim", "Lee", "Park", "Choi", "Jung", "Kang", "Cho", "Yoon"]),
    ("Australia", 3, ["New South Wales", "Victoria", "Queensland", "Western Australia"],
     ["Jack", "Cooper", "Mason", "Lachlan", "Hamish"],
     ["Isla", "Matilda", "Ruby", "Zara", "Evie"],
     ["Smith", "Kelly", "Ryan", "Murphy", "Walsh", "O'Brien", "Byrne", "Nguyen"]),
    ("Netherlands", 3, ["North Holland", "South Holland", "Utrecht", "Gelderland"],
     ["Daan", "Sem", "Lucas", "Levi", "Bram", "Thijs"],
     ["Emma", "Julia", "Tess", "Sanne", "Fleur", "Noor"],
     ["de Jong", "Jansen", "de Vries", "van Dijk", "Bakker", "Visser", "Smit", "Meijer"]),
    ("Mexico", 3, ["Mexico City", "Jalisco", "Nuevo León", "Puebla"],
     ["Diego", "Santiago", "Emiliano", "Leonardo", "Iker"],
     ["Valentina", "Ximena", "Regina", "Renata", "Fernanda"],
     ["Hernández", "García", "Martínez", "López", "González", "Ramírez", "Flores", "Cruz"]),
    ("Ukraine", 2, ["Kyiv", "Lviv", "Kharkiv", "Odesa"],
     ["Oleksandr", "Dmytro", "Andriy", "Maksym", "Taras"],
     ["Olena", "Iryna", "Kateryna", "Sofiia", "Daryna"],
     ["Shevchenko", "Kovalenko", "Bondarenko", "Tkachenko", "Kravchenko", "Melnyk"]),
    ("Philippines", 2, ["Metro Manila", "Cebu", "Davao"],
     ["Juan", "Miguel", "Paolo", "Carlo", "Rafael"],
     ["Andrea", "Bea", "Kim", "Patricia", "Nicole"],
     ["Santos", "Reyes", "Cruz", "Bautista", "Ocampo", "Garcia", "Mendoza"]),
]

HANDLE_WORDS = {
    "movie": ["cinephile", "reel", "frames", "popcorn", "noir", "director", "kino", "filmfan", "celluloid", "screen"],
    "show": ["binge", "couchpotato", "episodes", "seriesaddict", "tvfan", "showrunner", "nextepisode", "bingewatch"],
    "game": ["gamer", "pixel", "respawn", "loot", "speedrun", "noscope", "controller", "8bit", "backlog", "gg", "souls", "quest"],
    "manga": ["otaku", "manga", "shonen", "seinen", "panel", "ink", "chapter", "senpai", "weeb", "tankobon"],
    "any": ["night", "neon", "lazy", "cosmic", "retro", "silent", "lucky", "wild", "frost", "echo", "nova", "storm"],
}

ARCHETYPES = {
    # weights for movie / show / game / manga, how many ratings (median), review chance, deep chance
    "cinephile": dict(w=(0.75, 0.15, 0.05, 0.05), n=80, review=0.22, deep=0.30),
    "binger": dict(w=(0.25, 0.65, 0.05, 0.05), n=60, review=0.10, deep=0.15),
    "gamer": dict(w=(0.10, 0.08, 0.77, 0.05), n=55, review=0.14, deep=0.25),
    "otaku": dict(w=(0.08, 0.27, 0.10, 0.55), n=70, review=0.10, deep=0.18),
    "omnivore": dict(w=(0.35, 0.30, 0.20, 0.15), n=110, review=0.12, deep=0.20),
    "casual": dict(w=(0.45, 0.35, 0.12, 0.08), n=18, review=0.04, deep=0.05),
    "critic": dict(w=(0.45, 0.25, 0.20, 0.10), n=160, review=0.45, deep=0.55),
}
ARCHETYPE_WEIGHTS = {"cinephile": 14, "binger": 16, "gamer": 18, "otaku": 13, "omnivore": 14, "casual": 18, "critic": 5}

TYPES = ("movie", "show", "game", "manga")
DB_TYPE = {"movie": "MOVIE", "show": "SHOW", "game": "GAME", "manga": "MANGA"}

CRITERIA = {
    "movie": ["narrative", "cinematography", "sound", "acting"],
    "show": ["narrative", "cinematography", "sound", "acting", "ending"],
    "game": ["narrative", "gameplay", "visuals", "performance", "audio"],
    "manga": ["narrative", "artStyle", "characters", "development"],
}

# ---------------------------------------------------------------- review text

OPENERS = {
    "rave": ["An absolute masterpiece.", "This one is going straight into my all-time favourites.", "I did not expect it to hit this hard.",
             "Flawless from start to finish.", "Easily one of the best things I've {consumed} in years.", "Wow. Just wow.",
             "I keep thinking about this days later.", "Pure perfection.", "This is why I love {medium}.", "Believe the hype."],
    "good": ["Really solid.", "Very good, with a couple of rough edges.", "Had a great time with this.", "Better than I expected.",
             "A strong entry, even if it's not perfect.", "Genuinely enjoyable.", "Pleasantly surprised.", "Good stuff overall.",
             "Worth your time.", "I liked this a lot more than my friends did."],
    "mixed": ["Mixed feelings on this one.", "Some great moments, some real slogs.", "It's fine. Just fine.", "I wanted to love it.",
              "Decent, but it never quite clicks.", "Started strong, lost me halfway.", "Not bad, not great.",
              "I can see why people like it, but it wasn't for me.", "Average at best.", "A bit overrated if you ask me."],
    "bad": ["Honestly a chore to get through.", "I really don't get the hype.", "Disappointing.", "This was rough.",
            "Couldn't wait for it to end.", "A big letdown.", "I gave it every chance.", "Not for me at all.",
            "One of the weakest things I've {consumed} this year.", "Skip it."],
}
DETAILS = {
    "movie": {
        "pos": ["The cinematography is stunning, every frame could be a wallpaper.", "The score carries so many scenes.",
                "The lead performance is incredible.", "The pacing is tight and it never drags.", "The final act is unforgettable.",
                "The dialogue is sharp and often really funny.", "The world-building is so rich.", "Practical effects still look amazing.",
                "The editing in the action scenes is top tier.", "It's the kind of movie that rewards a rewatch."],
        "neg": ["The third act falls apart.", "It's at least thirty minutes too long.", "The characters are paper thin.",
                "The CGI looks dated already.", "The plot holes are hard to ignore.", "The tone is all over the place.",
                "The script relies on clichés.", "The ending feels rushed.", "It's beautiful but emotionally hollow.",
                "Way too much exposition."],
    },
    "show": {
        "pos": ["The character arcs are handled beautifully.", "Every episode ends on a hook, I binged it in a weekend.",
                "The writing gets better every season.", "The cast has amazing chemistry.", "The finale actually sticks the landing.",
                "The soundtrack is fantastic.", "The slow burn really pays off.", "The world feels lived in.",
                "Some episodes are genuinely perfect television.", "It handles heavy themes with real care."],
        "neg": ["The middle seasons drag.", "The ending ruined it for me.", "Too many filler episodes.", "It lost its way after the first season.",
                "The side plots go nowhere.", "The characters keep making baffling decisions.", "It should have ended a season earlier.",
                "The pacing is glacial.", "The budget cuts are very visible later on.", "The writers clearly had no plan."],
    },
    "game": {
        "pos": ["The gameplay loop is incredibly addictive.", "The combat feels tight and rewarding.", "The art direction is gorgeous.",
                "The soundtrack is a banger.", "The level design is genius.", "The story kept me hooked until the credits.",
                "Exploring this world is a joy.", "Boss fights are the highlight.", "So much content, I lost track of time.",
                "Performance was smooth the whole way through."],
        "neg": ["Performance is a mess, constant frame drops.", "The grind gets old fast.", "The story is forgettable.",
                "Too many bugs at launch.", "The controls feel clunky.", "Way too much padding.", "Microtransactions everywhere.",
                "The difficulty spikes are unfair.", "The open world feels empty.", "The ending is a letdown."],
    },
    "manga": {
        "pos": ["The art is breathtaking, some panels are pure art.", "The character development is incredible.", "The fights are drawn beautifully.",
                "The plot twists caught me completely off guard.", "The world-building is so detailed.", "I couldn't stop reading.",
                "The emotional moments hit hard.", "The villain is one of the best written ever.", "The pacing is perfect.",
                "Every arc is better than the last."],
        "neg": ["The pacing is all over the place.", "Too many filler chapters.", "The art gets sloppy later on.", "The ending felt rushed.",
                "The power system stops making sense.", "Too much fan service.", "The side characters get forgotten.",
                "The tournament arc dragged forever.", "The hiatuses killed my interest.", "The romance subplot goes nowhere."],
    },
}
CLOSERS = {
    "rave": ["Must {verb}.", "10/10 would {verb} again.", "Go {verb} it right now.", "Instant classic.", "No notes.", ""],
    "good": ["Recommended.", "Would {verb} again.", "Definitely worth it.", "Looking forward to more.", "", ""],
    "mixed": ["Maybe give it a shot if you're a fan of the genre.", "Wait for a sale." , "Your mileage may vary.", "", ""],
    "bad": ["Can't recommend it.", "Life's too short.", "Hard pass.", "Glad it's over.", ""],
}
VERB = {"movie": "watch", "show": "watch", "game": "play", "manga": "read"}
CONSUMED = {"movie": "watched", "show": "watched", "game": "played", "manga": "read"}
MEDIUM = {"movie": "cinema", "show": "TV", "game": "games", "manga": "manga"}

LIST_TITLES = {
    "movie": ["My Top {n} Films", "All-Time Favourite Movies", "Comfort Movies", "Best Sci-Fi Ever Made", "Films That Broke Me",
              "Movies Everyone Should See", "Rewatchable Forever", "Underrated Gems", "Perfect Movie Night Picks", "Best of the 2010s"],
    "show": ["Top {n} TV Shows", "Shows Worth Your Time", "Best Binges", "Shows That Stuck the Landing", "Comfort Series",
             "Peak Television", "Anime I'd Rewatch Anytime", "My Watchlist Hall of Fame"],
    "game": ["My Top {n} Games", "Games That Defined Me", "Best RPGs", "Hardest Games I've Beaten", "Cozy Games", "GOTY Picks",
             "Games With The Best Stories", "Couch Co-op Favourites"],
    "manga": ["Top {n} Manga", "Manga Everyone Should Read", "Best Art in Manga", "Peak Fiction", "Seinen Favourites",
              "Manga That Made Me Cry", "Best Shounen"],
}
LIST_DESCS = ["Ranked, not sorted. Fight me.", "Updated whenever I finish something great.", "My personal canon.", "",
              "Recommendations for friends.", "Strictly my opinion.", ""]

PLATFORMS = ["PC", "PC", "PC", "PS5", "PS5", "PS4", "Switch", "Switch", "Xbox Series X", "Steam Deck"]


def uid(name: str) -> str:
    return str(uuid.uuid5(NS, name))


def q(v) -> str:
    """SQL literal."""
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "TRUE" if v else "FALSE"
    if isinstance(v, (int, float)):
        return repr(v)
    if isinstance(v, datetime):
        return "'" + v.isoformat() + "'"
    return "'" + str(v).replace("'", "''") + "'"


def strip_accents(s: str) -> str:
    """ASCII-only handle part: "Matěj Dvořák" -> "matejdvorak"."""
    s = s.replace("ł", "l").replace("Ł", "L").replace("ß", "ss").replace("ø", "o").replace("æ", "ae")
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode()
    return "".join(ch for ch in s if ch.isalnum())


def snap(score: float, style: str) -> int:
    score = max(0, min(100, score))
    if style == "STARS_5":
        return int(round(score / 20) * 20)
    if style == "STARS_10":
        return int(round(score / 10) * 10)
    return int(round(score))


def rand_time(start: datetime, end: datetime, recent_bias: float = 2.0) -> datetime:
    """Random moment between start and end, skewed towards end (activity grows over time)."""
    if end <= start:
        return end
    span = (end - start).total_seconds()
    t = rng.random() ** (1 / recent_bias)
    return start + timedelta(seconds=span * t)


# ---------------------------------------------------------------- build

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--friend-of", default="cloudy", help="existing account that gets friends and friend requests")
    args = ap.parse_args()

    with open(CATALOG, encoding="utf-8") as f:
        catalog = json.load(f)
    by_type = {t: [c for c in catalog if c["type"] == t] for t in TYPES}
    genres_by_type = {t: sorted({g for c in by_type[t] for g in c["genres"]}) for t in TYPES}
    # popularity weight: earlier (more popular) items are rated far more often
    for t in TYPES:
        for i, c in enumerate(by_type[t]):
            c["_pop"] = 1.0 / math.pow(i + 8, 0.85)
            c["_quality"] = c.get("globalScore") or rng.randint(62, 80)

    # ---- users
    users = []
    used_names = set()
    country_weights = [c[1] for c in COUNTRIES]
    archetype_names = list(ARCHETYPE_WEIGHTS)
    for i in range(NUM_USERS):
        country, _, regions, males, females, lasts = rng.choices(COUNTRIES, weights=country_weights)[0]
        female = rng.random() < 0.46
        first = rng.choice(females if female else males)
        last = rng.choice(lasts)
        arche = rng.choices(archetype_names, weights=[ARCHETYPE_WEIGHTS[a] for a in archetype_names])[0]
        fav_type = TYPES[max(range(4), key=lambda k: ARCHETYPES[arche]["w"][k])]

        for _ in range(50):
            pattern = rng.random()
            f_, l_ = strip_accents(first).lower(), strip_accents(last).lower()
            if pattern < 0.30:
                name = f"{f_}_{l_[0]}"
            elif pattern < 0.50:
                name = f"{f_}{l_}"
            elif pattern < 0.65:
                name = f"{f_}.{l_}"
            elif pattern < 0.85:
                name = f"{rng.choice(HANDLE_WORDS[fav_type])}_{f_}"
            else:
                name = f"{rng.choice(HANDLE_WORDS['any'])}{rng.choice(HANDLE_WORDS[fav_type])}"
            if rng.random() < 0.3:
                name += str(rng.choice([rng.randint(1, 99), rng.randint(1985, 2006)]))
            name = name[:24]
            if name not in used_names:
                used_names.add(name)
                break

        created = NOW - timedelta(days=rng.uniform(3, 420))
        # taste: per-genre affinity, a personal bias (generous/harsh) and noise level
        affinity = {}
        for t in TYPES:
            gs = genres_by_type[t]
            if not gs:
                continue
            for g in rng.sample(gs, min(len(gs), rng.randint(2, 4))):
                affinity[(t, g)] = rng.uniform(7, 18)
            for g in rng.sample(gs, min(len(gs), rng.randint(1, 2))):
                affinity.setdefault((t, g), -rng.uniform(8, 20))
        style = rng.choices(["PERCENT", "STARS_10", "STARS_5"], weights=[70, 18, 12])[0]
        portrait = f"https://randomuser.me/api/portraits/{'women' if female else 'men'}/{rng.randint(0, 99)}.jpg"
        users.append(dict(
            id=uid("user:" + name), username=name, name=f"{first} {last}", real=f"{first} {last}", country=country,
            region=rng.choice(regions), image=None if rng.random() < 0.12 else portrait, created=created, arche=arche,
            affinity=affinity, bias=rng.gauss(0, 7) + (6 if arche == "casual" else 0) - (5 if arche == "critic" else 0),
            noise=rng.uniform(5, 11), style=style, round5=rng.random() < 0.35,
        ))

    # ---- ratings
    ratings = []          # dicts
    rated = {}            # user_id -> {media_id: rating}
    for u in users:
        a = ARCHETYPES[u["arche"]]
        n = int(min(650, max(3, rng.lognormvariate(math.log(a["n"]), 0.75))))
        rated[u["id"]] = {}
        type_counts = [0, 0, 0, 0]
        for _ in range(n):
            type_counts[rng.choices(range(4), weights=a["w"])[0]] += 1
        for ti, t in enumerate(TYPES):
            pool = by_type[t]
            if not pool or not type_counts[ti]:
                continue
            weights = []
            for c in pool:
                aff = max((u["affinity"].get((t, g), 0) for g in c["genres"]), default=0) if c["genres"] else 0
                weights.append(c["_pop"] * math.exp(aff / 12))
            k = min(type_counts[ti], len(pool))
            chosen = set()
            while len(chosen) < k:
                for c in rng.choices(pool, weights=weights, k=k - len(chosen)):
                    chosen.add(c["id"])
            for mid in sorted(chosen):
                c = next(x for x in pool if x["id"] == mid)
                aff = sum(u["affinity"].get((t, g), 0) for g in c["genres"]) / max(1, len(c["genres"])) if c["genres"] else 0
                raw = c["_quality"] + 4 + u["bias"] + aff * 0.9 + rng.gauss(0, u["noise"])
                if rng.random() < 0.03:
                    raw += rng.choice([-35, 25])  # the occasional hot take
                if u["style"] == "PERCENT" and u["round5"]:
                    raw = round(raw / 5) * 5
                score = snap(raw, u["style"])
                score = max(score, 0 if u["style"] != "PERCENT" else 3)
                created = rand_time(u["created"], NOW - timedelta(minutes=rng.randint(5, 600)), recent_bias=1.6)
                r = dict(id=uid(f"rating:{u['id']}:{mid}"), user=u, item=c, score=score, created=created, review=None, criteria=None)
                if rng.random() < a["review"]:
                    r["review"] = make_review(c, score)
                if rng.random() < a["deep"]:
                    r["criteria"] = {k_: snap(score + rng.gauss(0, 9), u["style"]) for k_ in CRITERIA[t]}
                ratings.append(r)
                rated[u["id"]][mid] = r

    # ---- watchlists
    watch = []
    for u in users:
        mine = rated[u["id"]]
        for mid, r in mine.items():
            if rng.random() < 0.45:
                watch.append(watch_row(u, r["item"], "COMPLETED", r["created"] - timedelta(days=rng.uniform(0, 20)), r["created"]))
        a = ARCHETYPES[u["arche"]]
        for status, lo, hi in (("IN_PROGRESS", 1, 6), ("PLANNING", 3, 25), ("ON_HOLD", 0, 3), ("DROPPED", 0, 3)):
            for _ in range(rng.randint(lo, hi)):
                t = TYPES[rng.choices(range(4), weights=a["w"])[0]]
                pool = by_type[t]
                if not pool:
                    continue
                c = rng.choices(pool, weights=[x["_pop"] for x in pool])[0]
                if c["id"] in mine or any(w["user"] is u and w["item"]["id"] == c["id"] for w in watch[-60:]):
                    continue
                added = rand_time(u["created"], NOW - timedelta(hours=1), 1.5)
                watch.append(watch_row(u, c, status, added, None))

    # ---- custom lists (feed the global List Rank)
    lists = []
    for u in users:
        if rng.random() > (0.65 if u["arche"] in ("critic", "cinephile", "omnivore") else 0.3):
            continue
        for t in rng.sample(TYPES, rng.randint(1, 3)):
            top = sorted((r for r in rated[u["id"]].values() if r["item"]["type"] == t), key=lambda r: -r["score"])
            if len(top) < 5:
                continue
            size = min(len(top), rng.choice([5, 5, 10, 10, 10, 15, 20, 25]))
            # personal ordering among their favourites: mostly by score, with ties broken by gut feeling
            picks = sorted(top[: size + 3], key=lambda r: -(r["score"] + rng.gauss(0, 4)))[:size]
            created = rand_time(u["created"], NOW - timedelta(hours=2), 1.3)
            title = rng.choice(LIST_TITLES[t]).format(n=size)
            lists.append(dict(id=uid(f"list:{u['id']}:{t}:{title}"), user=u, type=t, title=title,
                              desc=rng.choice(LIST_DESCS) or None, created=created, items=picks))

    # ---- friendships
    pairs = {}
    user_by_country = {}
    for u in users:
        user_by_country.setdefault(u["country"], []).append(u)
    for u in users:
        k = rng.randint(1, 14) if u["arche"] != "casual" else rng.randint(0, 4)
        for _ in range(k):
            other = rng.choice(user_by_country[u["country"]]) if rng.random() < 0.45 else rng.choice(users)
            if other is u:
                continue
            key = tuple(sorted((u["id"], other["id"])))
            if key in pairs:
                continue
            status = rng.choices(["ACCEPTED", "PENDING", "DECLINED"], weights=[88, 9, 3])[0]
            created = rand_time(max(u["created"], other["created"]), NOW - timedelta(hours=1), 1.2)
            pairs[key] = (u["id"], other["id"], status, created)

    # ---- activities (feed): every rating + progress and status updates
    activities = []
    for r in ratings:
        c = r["item"]
        activities.append((r["user"]["id"], "RATED_MEDIA", c, {"score": r["score"]}, r["created"]))
    progress_log = []
    for w in watch:
        u, c = w["user"], w["item"]
        if w["status"] != "COMPLETED" and rng.random() < 0.6:
            activities.append((u["id"], "WATCHLIST_STATUS", c, {"status": w["status"]}, w["added"]))
        if w["status"] == "IN_PROGRESS":
            when = rand_time(NOW - timedelta(days=21), NOW - timedelta(minutes=10), 1.8)
            t = c["type"]
            if t == "show":
                activities.append((u["id"], "EPISODES_WATCHED", c, {"episode": w["episodes"]}, when))
                progress_log.append((u["id"], c["id"], f"+{rng.randint(1, 4)} episodes", when))
            elif t == "manga":
                activities.append((u["id"], "CHAPTERS_READ", c, {"chapter": w["chapters"]}, when))
                progress_log.append((u["id"], c["id"], f"+{rng.randint(1, 15)} chapters", when))
            elif t == "game":
                activities.append((u["id"], "HOURS_PLAYED", c, {"hours": w["hours"]}, when))
                progress_log.append((u["id"], c["id"], f"+{rng.randint(1, 6)} hours", when))

    write_sql(args.friend_of, users, ratings, watch, lists, pairs, activities, progress_log)
    print(f"users={len(users)} ratings={len(ratings)} reviews={sum(1 for r in ratings if r['review'])} "
          f"deep={sum(1 for r in ratings if r['criteria'])} watchlist={len(watch)} lists={len(lists)} "
          f"friendships={len(pairs)} activities={len(activities)}")


def make_review(c, score):
    t = c["type"]
    band = "rave" if score >= 90 else "good" if score >= 70 else "mixed" if score >= 45 else "bad"
    fill = lambda s: s.format(consumed=CONSUMED[t], medium=MEDIUM[t], verb=VERB[t])  # noqa: E731
    parts = [fill(rng.choice(OPENERS[band]))]
    pos, neg = DETAILS[t]["pos"], DETAILS[t]["neg"]
    if band == "rave":
        parts += rng.sample(pos, rng.randint(1, 2))
    elif band == "good":
        parts += rng.sample(pos, 1) + (rng.sample(neg, 1) if rng.random() < 0.5 else [])
    elif band == "mixed":
        parts += rng.sample(pos, 1) + rng.sample(neg, 1)
    else:
        parts += rng.sample(neg, rng.randint(1, 2))
    closer = fill(rng.choice(CLOSERS[band]))
    if closer:
        parts.append(closer)
    if rng.random() < 0.15:
        parts = parts[:1] + parts[-1:]  # some people keep it short
    return " ".join(parts)


def watch_row(u, c, status, added, finished):
    t = c["type"]
    row = dict(user=u, item=c, status=status, added=added, started=None, finished=finished,
               episodes=0, chapters=0, volumes=0, hours=0.0, platform=None, watch_count=0)
    if status in ("IN_PROGRESS", "COMPLETED", "ON_HOLD", "DROPPED"):
        row["started"] = added
    done = status == "COMPLETED"
    if t == "show":
        row["episodes"] = rng.randint(20, 90) if done else rng.randint(1, 30)
    elif t == "manga":
        row["chapters"] = rng.randint(60, 400) if done else rng.randint(1, 150)
        row["volumes"] = row["chapters"] // 9
    elif t == "game":
        row["hours"] = round(rng.uniform(15, 140) if done else rng.uniform(1, 40), 1)
        row["platform"] = rng.choice(PLATFORMS)
    elif t == "movie":
        row["watch_count"] = rng.choices([1, 2, 3, 5], weights=[70, 18, 8, 4])[0] if done else 0
    return row


def batched(rows, size=400):
    for i in range(0, len(rows), size):
        yield rows[i:i + size]


def write_sql(friend_of, users, ratings, watch, lists, pairs, activities, progress_log):
    out = []
    w = out.append
    w("-- Large seed for Media Tracker. Generated by scripts/seed/generate_seed.py; do not edit by hand.")
    w("-- Run after scripts/seed/wipe.sql. Every seeded user has an id derived from its username (uuid5).")
    w("BEGIN;")
    w("SET LOCAL synchronous_commit = off;")

    w("\n-- Users")
    for chunk in batched(users):
        w('INSERT INTO public.users (id, username, name, email, "realName", "stateRegion", country, role, image, rating_style, created_at) VALUES')
        w(",\n".join(f"({q(u['id'])}, {q(u['username'])}, {q(u['name'])}, {q(u['username'] + '@example.com')}, {q(u['real'])}, "
                     f"{q(u['region'])}, {q(u['country'])}, 'user', {q(u['image'])}, {q(u['style'])}, {q(u['created'])})" for u in chunk)
          + "\nON CONFLICT (id) DO NOTHING;")

    w("\n-- Privacy: mostly public, a few friends-only or private")
    rows = []
    for u in users:
        roll = rng.random()
        prof = "PRIVATE" if roll < 0.03 else "FRIENDS_ONLY" if roll < 0.08 else "PUBLIC"
        rat = "FRIENDS_ONLY" if rng.random() < 0.08 else prof
        rows.append(f"({q(uid('privacy:' + u['id']))}, {q(u['id'])}, '{prof}', '{rat}', '{prof}', '{prof}', {q(u['created'])})")
    w('INSERT INTO public.user_privacy_settings (id, user_id, profile_visibility, ratings_visibility, watchlist_visibility, activity_visibility, updated_at) VALUES')
    w(",\n".join(rows) + ";")

    w("\n-- Ratings, written reviews and deep reviews")
    for chunk in batched(ratings, 500):
        w("INSERT INTO public.user_ratings (id, user_id, media_id, media_title, media_image, score, review_text, username, avatar_url, criteria_scores, is_deep_review, created_at, media_release_date) VALUES")
        w(",\n".join(
            f"({q(r['id'])}, {q(r['user']['id'])}, {q(r['item']['id'])}, {q(r['item']['title'])}, {q(r['item']['image'])}, {r['score']}, "
            f"{q(r['review'])}, {q(r['user']['username'])}, {q(r['user']['image'])}, {q(json.dumps(r['criteria'] or {}))}::jsonb, "
            f"{q(bool(r['criteria']))}, {q(r['created'])}, {q(r['item'].get('releaseDate'))})" for r in chunk) + ";")

    w("\n-- Watchlists")
    seen = set()
    wrows = []
    for x in watch:
        key = (x["user"]["id"], x["item"]["id"])
        if key in seen:
            continue
        seen.add(key)
        wrows.append(x)
    for chunk in batched(wrows, 500):
        w('INSERT INTO public.user_watchlist (id, user_id, media_id, media_title, media_image, status, "chaptersRead", "episodesWatched", "volumesRead", media_type, "hoursPlayed", platform, "watchCount", added_at, started_at, finished_at) VALUES')
        w(",\n".join(
            f"({q(uid('watch:' + x['user']['id'] + ':' + x['item']['id']))}, {q(x['user']['id'])}, {q(x['item']['id'])}, {q(x['item']['title'])}, "
            f"{q(x['item']['image'])}, '{x['status']}', {x['chapters']}, {x['episodes']}, {x['volumes']}, '{DB_TYPE[x['item']['type']]}', "
            f"{x['hours']}, {q(x['platform'])}, {x['watch_count']}, {q(x['added'])}, {q(x['started'])}, {q(x['finished'])})" for x in chunk) + ";")

    w("\n-- Custom ranked lists")
    for chunk in batched(lists, 200):
        w("INSERT INTO public.user_lists (id, user_id, title, description, media_type, created_at, updated_at) VALUES")
        w(",\n".join(f"({q(l['id'])}, {q(l['user']['id'])}, {q(l['title'])}, {q(l['desc'])}, '{DB_TYPE[l['type']]}', {q(l['created'])}, {q(l['created'])})" for l in chunk) + ";")
    items = [(l, i, r) for l in lists for i, r in enumerate(l["items"], start=1)]
    for chunk in batched(items, 500):
        w("INSERT INTO public.user_list_items (id, list_id, media_id, media_title, media_image, rank_position) VALUES")
        w(",\n".join(f"({q(uid('item:' + l['id'] + ':' + str(i)))}, {q(l['id'])}, {q(r['item']['id'])}, {q(r['item']['title'])}, {q(r['item']['image'])}, {i})" for l, i, r in chunk) + ";")

    w("\n-- Friendships between seeded users")
    prow = list(pairs.values())
    for chunk in batched(prow, 500):
        w("INSERT INTO public.friendships (id, sender_id, receiver_id, status, created_at, updated_at) VALUES")
        w(",\n".join(f"({q(uid('friend:' + a + ':' + b))}, {q(a)}, {q(b)}, '{s}', {q(c)}, {q(c)})" for a, b, s, c in chunk) + ";")

    # Friends for the real account: the most active users, so the friend feed is busy.
    active = sorted(users, key=lambda u: -sum(1 for r in ratings if r["user"] is u and r["created"] > NOW - timedelta(days=30)))
    accepted, incoming, outgoing = active[:28], active[28:34], active[34:37]
    w(f"\n-- Friends and requests for the existing account '{friend_of}' (skipped if it does not exist)")
    vals = []
    for u in accepted:
        vals.append(f"({q(u['id'])}, 'ACCEPTED', {q(rand_time(NOW - timedelta(days=60), NOW - timedelta(days=1)))}, false)")
    for u in incoming:
        vals.append(f"({q(u['id'])}, 'PENDING', {q(rand_time(NOW - timedelta(days=5), NOW - timedelta(hours=1)))}, false)")
    for u in outgoing:
        vals.append(f"({q(u['id'])}, 'PENDING', {q(rand_time(NOW - timedelta(days=5), NOW - timedelta(hours=1)))}, true)")
    w(f"""INSERT INTO public.friendships (id, sender_id, receiver_id, status, created_at, updated_at)
SELECT gen_random_uuid(),
       CASE WHEN v.outgoing THEN me.id ELSE v.other::uuid END,
       CASE WHEN v.outgoing THEN v.other::uuid ELSE me.id END,
       v.status::public."FriendshipStatus", v.created_at::timestamptz, v.created_at::timestamptz
FROM public.users me,
     (VALUES {", ".join(vals)}) AS v(other, status, created_at, outgoing)
WHERE me.username = {q(friend_of)};""")

    w("\n-- Activity feed")
    for chunk in batched(activities, 500):
        w("INSERT INTO public.user_activities (id, user_id, type, media_id, media_title, media_image, media_type, data, created_at) VALUES")
        w(",\n".join(f"(gen_random_uuid(), {q(uid_)}, '{typ}', {q(c['id'])}, {q(c['title'])}, {q(c['image'])}, '{DB_TYPE[c['type']]}', {q(json.dumps(data))}::jsonb, {q(when)})"
                     for uid_, typ, c, data, when in chunk) + ";")
    for chunk in batched(progress_log, 500):
        w("INSERT INTO public.activity_log (id, user_id, media_id, increment, created_at) VALUES")
        w(",\n".join(f"(gen_random_uuid(), {q(a)}, {q(m)}, {q(inc)}, {q(when)})" for a, m, inc, when in chunk) + ";")

    with open(os.path.join(HERE, "recompute-badges.sql"), encoding="utf-8") as f:
        badges_sql = f.read()
    w(DERIVED_SQL.replace("-- @@BADGES@@", badges_sql))
    w("COMMIT;")
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(out) + "\n")
    print("wrote", OUT, f"{os.path.getsize(OUT) / 1e6:.1f} MB")


# Everything the backend normally derives on save: community stats, per-user stats cache, badges.
DERIVED_SQL = r"""
-- Community scores (same rules as StatsService: average rounded to a whole percent)
INSERT INTO public.media_stats (id, community_average, total_ratings, media_type)
SELECT r.media_id, ROUND(AVG(r.score)), COUNT(*),
  CASE
    WHEN r.media_id LIKE 'tmdb-movie-%' THEN 'MOVIE'::public."MediaType"
    WHEN r.media_id LIKE 'tmdb-tv-%' THEN 'SHOW'::public."MediaType"
    WHEN r.media_id LIKE 'igdb-game-%' THEN 'GAME'::public."MediaType"
    WHEN r.media_id LIKE 'mangadex-manga-%' OR r.media_id LIKE 'anilist-manga-%' THEN 'MANGA'::public."MediaType"
    ELSE 'OTHER'::public."MediaType"
  END
FROM public.user_ratings r
GROUP BY r.media_id
ON CONFLICT (id) DO UPDATE SET community_average = EXCLUDED.community_average,
  total_ratings = EXCLUDED.total_ratings, media_type = EXCLUDED.media_type;

-- Per-user statistics cache (same shape as StatsService.updateUserStatsCache)
INSERT INTO public."UserStatsCache" (user_id, media_type, stats_json, updated_at)
SELECT u.id, m.t,
  json_build_object(
    'total_count', COALESCE(s.total, 0),
    'average_score', COALESCE(s.avg, 0),
    'highest_score', COALESCE(s.hi, 0),
    'lowest_score', COALESCE(s.lo, 0),
    'score_distribution', (
      SELECT json_object_agg(b::text, COALESCE(d.cnt, 0) ORDER BY b)
      FROM generate_series(1, 10) b
      LEFT JOIN (
        SELECT LEAST(10, GREATEST(1, CEIL(r.score / 10.0)))::int AS bucket, COUNT(*) AS cnt
        FROM public.user_ratings r
        WHERE r.user_id = u.id AND (
          (m.t = 'MOVIE' AND r.media_id LIKE 'tmdb-movie-%') OR (m.t = 'SHOW' AND r.media_id LIKE 'tmdb-tv-%') OR
          (m.t = 'GAME' AND r.media_id LIKE 'igdb-game-%') OR (m.t = 'MANGA' AND (r.media_id LIKE 'mangadex-manga-%' OR r.media_id LIKE 'anilist-manga-%')))
        GROUP BY 1
      ) d ON d.bucket = b
    ),
    'status_counts', json_build_object(
      'completed', COALESCE(ws.completed, 0), 'watching', COALESCE(ws.watching, 0),
      'plan_to_watch', COALESCE(ws.planning, 0), 'dropped', COALESCE(ws.dropped, 0))
  ), NOW()
FROM public.users u
CROSS JOIN (VALUES ('MOVIE'::public."MediaType"), ('SHOW'::public."MediaType"), ('GAME'::public."MediaType"), ('MANGA'::public."MediaType")) AS m(t)
LEFT JOIN LATERAL (
  SELECT COUNT(*) AS total, ROUND(AVG(score)) AS avg, MAX(score) AS hi, MIN(score) AS lo
  FROM public.user_ratings r
  WHERE r.user_id = u.id AND (
    (m.t = 'MOVIE' AND r.media_id LIKE 'tmdb-movie-%') OR (m.t = 'SHOW' AND r.media_id LIKE 'tmdb-tv-%') OR
    (m.t = 'GAME' AND r.media_id LIKE 'igdb-game-%') OR (m.t = 'MANGA' AND (r.media_id LIKE 'mangadex-manga-%' OR r.media_id LIKE 'anilist-manga-%')))
) s ON true
LEFT JOIN LATERAL (
  SELECT COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed, COUNT(*) FILTER (WHERE status = 'IN_PROGRESS') AS watching,
         COUNT(*) FILTER (WHERE status = 'PLANNING') AS planning, COUNT(*) FILTER (WHERE status = 'DROPPED') AS dropped
  FROM public.user_watchlist w WHERE w.user_id = u.id AND w.media_type = m.t
) ws ON true
ON CONFLICT (user_id, media_type) DO UPDATE SET stats_json = EXCLUDED.stats_json, updated_at = EXCLUDED.updated_at;

-- @@BADGES@@

-- Ask the worker to compute the global List Rank from the new lists
INSERT INTO public."BackgroundJob" (id, type, payload, status, dedupe_key, attempts, max_attempts, run_at, created_at, updated_at)
VALUES ('seed-recalculate-ranks', 'recalculate_ranks', '{}'::jsonb, 'pending', 'seed_recalculate_ranks', 0, 3, NOW(), NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET status = 'pending', attempts = 0, run_at = NOW(), updated_at = NOW();
"""

if __name__ == "__main__":
    main()
