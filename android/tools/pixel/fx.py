"""Everything in the room that is not furniture: you, and the state of things.

Three cleanliness domains, and each one shows up somewhere you would look for it:

  BODY   on you. Fresh and shiny at 0; by 3 the hair is up, the eyes are heavy and there
         are stink lines. Cartoon stink lines, not grime — the point is to be noticed and
         fixed, not to be disgusting.
  ROOM   on the floor. A sock and a paper ball at 1; dishes, a laundry pile and dust at 2;
         a pizza box and a cobweb at 3. Cumulative, so getting worse adds things rather than
         swapping the whole picture, and getting better visibly takes them away.
  HEART  above you, as weather. A little floating heart at 0, nothing at 1, a cloud at 2,
         a rain cloud at 3. Weather because it passes, which is the true thing to say about
         a mood, and because a cloud is a mood you can draw without a sad face.

None of it is ever gloomy. Mess in here is a Tamagotchi's mess: noticeable, a bit funny,
and gone the moment someone deals with it. Shame makes people avoid the thing that shames
them (Tangney & Dearing, 2002), and avoidance is the opposite of what this is for.

The character is a bubble — this is DeBubble, and the bubble is the thing that learns to go
outside. Teal rather than a skin tone, so it is nobody in particular and anybody.
"""

FX = {}


def fx(name, rows):
    w = max(len(r) for r in rows)
    FX[name] = {
        "name": name, "w": w, "h": len(rows),
        "ragged": len({len(r) for r in rows}) > 1,
        "rows": [r.ljust(w, ".") for r in rows],
    }


# ----------------------------------------------------------------- you, by Body level
#
# All four share a 20x19 frame with the body in the same place, so swapping state never
# makes the character jump. The extra width is for stink lines, the extra height for hair.

fx("me0", [
    "....................",
    "....................",
    "....................",
    ".......kkkkkk.......",
    ".....kkcccccckk.....",
    "....kciicccccccK....",
    "....kciccccccccK....",
    "...kcccccccccccCK...",
    "...kcckicccckicCK...",
    "...kcckkcccckkcCK...",
    "...kcpccccccccpCK...",
    "...kccckcccckccCK...",
    "...kCccckkkkcccCK...",
    "....kCCcccccccCK....",
    "....kCCCCCCCCCnK....",
    ".....kKnnnnnnKK.....",
    "......kk....kk......",
    "......kn....kn......",
    "......kk....kk......",
])

fx("me1", [
    "....................",
    "....................",
    "....................",
    ".......kkkkkk.......",
    ".....kkcccccckk.....",
    "....kciccccccccK....",
    "....kccccccccccK....",
    "...kcccccccccccCK...",
    "...kcckicccckicCK...",
    "...kcckkcccckkcCK...",
    "...kcpccccccccpCK...",
    "...kcccckcckcccCK...",
    "...kCcccckkccccCK...",
    "....kCCcccccccCK....",
    "....kCCCCCCCCCnK....",
    ".....kKnnnnnnKK.....",
    "......kk....kk......",
    "......kn....kn......",
    "......kk....kk......",
])

fx("me2", [
    "....................",
    "..........n.........",
    "........n.nk........",
    ".......kkkkkk.....g.",
    ".....kkcccccckk..G..",
    "....kccccccccccK..G.",
    "....kccccccccccK.G..",
    "...kcccccccccccCK.g.",
    "...kcccccccccccCK...",
    "...kcckkcccckkcCK...",
    "...kcpccccccccpCK...",
    "...kcccccKKccccCK...",
    "...kCccccccccccCK...",
    "....kCCcccdcccCK....",
    "....kCCCCCCCCCnK....",
    ".....kKnnnnnnKK.....",
    "......kk....kk......",
    "......kn....kn......",
    "......kk....kk......",
])

fx("me3", [
    ".......n...n........",
    "......n.nn.nk.......",
    ".g......n.kn......g.",
    "..G....kkkkkk....G..",
    ".G...kkcccccckk...G.",
    "..G.kccccccccccK.G..",
    ".g..kcccdccccccK..g.",
    "...kcccccccccccCK...",
    "...kcccccccccccCK...",
    "...kcckkcccckkcCK...",
    "...kcccccccccccCK...",
    "...kcccckkkkcccCK...",
    "...kCcckcccckdcCK...",
    "....kCdcccccccCK....",
    "....kCCCCCCCCCnK....",
    ".....kKnnnnnnKK.....",
    "......kk....kk......",
    "......kn....kn......",
    "......kk....kk......",
])


# ----------------------------------------------------------------- Heart, as weather

fx("heart_sun", [
    ".kk.kk.",
    "kppkPPk",
    "kpPPPPk",
    ".kPPPk.",
    "..kPk..",
    "...k...",
])

fx("heart_cloud", [
    "....kkkk......",
    "..kkqqqqkkk...",
    ".kqqqqqqqqqkk.",
    "kqqqqqqqqqqqQk",
    "kQqqqqqqqqqQQk",
    ".kQQQQQQQQQQk.",
    "..kkkkkkkkkk..",
])

fx("heart_storm", [
    "....kkkk........",
    "..kkQQQQkkkk....",
    ".kQQQQQQQQQQkk..",
    "kQQQQQQQQQQQQQk.",
    "kxQQQQQQQQQQQxxk",
    ".kxxxxxxxxxxxxk.",
    "..kkkkkkYkkkkk..",
    "..b...b.Yy..b...",
    ".b...b...Y.b....",
    "......b..y...b..",
])


# ----------------------------------------------------------------- Room, as mess

fx("sock", [
    ".kkk....",
    "kiPik...",
    "kiiiikk.",
    ".kiiiPik",
    "..kkkkk.",
])

fx("paper", [
    ".kkk.",
    "kiwik",
    "kwivk",
    ".kkk.",
])

fx("dishes", [
    "......kkkk.",
    ".....kccckk",
    "kkkkkkcccck",
    "kiiiiikcckk",
    "kkkkkkkkkk.",
    "kiiiiiiik..",
    "kkkkkkkkk..",
])

fx("laundry", [
    ".....kkkk......",
    "...kkPPPPkk....",
    "..kPPppPPPkkk..",
    ".kCCkPPPkYYYYk.",
    "kCccCkkkYyyYYYk",
    "kCCCCCkYYYYYYok",
    ".kkkkkkkkkkkkk.",
])

fx("dust", [
    "..k.k..",
    ".kqQqk.",
    "kqQqQQk",
    ".kkkkk.",
])

fx("pizza", [
    "kkkkkkkkkkkkkk",
    "kFfffffffffffk",
    "kfYotYYoYtYYfk",
    "kfYYYoYYYYoYfk",
    "kkkkkkkkkkkkkk",
])

fx("peel", [
    "..k.....",
    ".kYk..k.",
    "kYyYkkYk",
    ".kkkkkk.",
])

fx("cobweb", [
    "kKKKKKKKK",
    "K.K...K..",
    "K..K.K...",
    "K.KKK....",
    "K.K.K....",
    "KK...K...",
    "K........",
    "K........",
    "K........",
])

fx("sparkle", [
    "..y..",
    "..i..",
    "yiiiy",
    "..i..",
    "..y..",
])


# ----------------------------------------------------------------- where it all goes
#
# Mess sits at fixed places so the room always gets messy in the same way, which is what
# makes a cleaned room recognisably *your* room cleaned. Each entry: (fx, x, baseline, level)
# — shown at that Room level and above. Positions avoid the default item spots enough that
# a furnished room still shows its mess, because hiding it behind a beanbag would defeat
# the point of the mirror.

ROOM_MESS = [
    ("sock",    34, 92, 1),
    ("paper",  116, 86, 1),
    ("dishes",  92, 82, 2),
    ("laundry", 54, 99, 2),
    ("dust",   140, 76, 2),
    ("dust",    70, 74, 2),
    ("pizza",  122, 99, 3),
    ("cobweb",   0,  9, 3),
    ("paper",   20, 97, 3),
    ("peel",   104, 93, 3),
    ("sock",   144, 90, 3),
]

# Where the room sparkles when it is spotless.
ROOM_SPARKLES = [(28, 20), (138, 54), (84, 66), (12, 70), (150, 34)]

ME_SPOT = (72, 95)        # (x, baseline) for the character by default
WEATHER_DROP = 2          # the weather's bottom edge sits this many rows into the 20x19 frame:
                          # in the empty headroom over a fresh head, over the hair of a scruffy one
MAX_DIRT = 3


def check():
    from palette import PALETTE
    problems = []
    for n, s in FX.items():
        if s["ragged"]:
            problems.append(f"{n}: ragged rows ({sorted({len(r) for r in s['rows']})})")
        bad = {ch for r in s["rows"] for ch in r} - set(PALETTE)
        if bad:
            problems.append(f"{n}: unknown palette keys {bad}")
    sizes = {(FX[f"me{i}"]["w"], FX[f"me{i}"]["h"]) for i in range(MAX_DIRT + 1)}
    if len(sizes) != 1:
        problems.append(f"the four 'me' frames differ in size: {sizes}")
    for name, *_ in ROOM_MESS:
        if name not in FX:
            problems.append(f"ROOM_MESS names unknown fx {name}")
    return problems


if __name__ == "__main__":
    print(check() or "ok")
