"""Everything in the room that is not furniture: you, the state of things, and today's tasks.

Three cleanliness domains, and each one shows up somewhere you would look for it:

  BODY   on you. Bright-eyed at 0; by 3 the hair is sticking up, the eyes are heavy, there
         are smudges and little stink wiggles. Cartoon, not grime — the point is to be
         noticed and fixed, not to be disgusting.
  ROOM   on the floor. A sock and a paper ball at 1; dishes, a laundry pile and dust at 2;
         a pizza box and a cobweb at 3. Cumulative, so getting worse adds things rather than
         swapping the whole picture, and getting better visibly takes them away.
  HEART  beside you, as a little heart with a face that floats at your shoulder. Sparkly at
         0, content at 1, sleepy at 2, squeezed-eyes-and-a-plaster at 3. A companion rather
         than weather over your head, because a cloud above someone reads as a verdict on
         them, and a heart that needs looking after reads as something you can look after.

And the outside: on a day with a mission still to do, you are holding a letter with a little
"!" over it. It is the invitation, in the room, and tapping it opens the mission — so the
missions live in the same picture as the cleaning rather than in a list somewhere below it.

None of it is ever gloomy. Mess in here is a Tamagotchi's mess: noticeable, a bit funny,
and gone the moment someone deals with it. Shame makes people avoid the thing that shames
them (Tangney & Dearing, 2002), and avoidance is the opposite of what this is for.

The character is drawn in one look — skin, hair and top — and the app swaps those three
ramps for the ones the user picked (palette.LOOKS). So every pixel of skin, hair and top
below uses only the a/A/u/e, m/M/N and l/L/j keys, and nothing else in the room does.
"""

FX = {}


def fx(name, rows):
    w = max(len(r) for r in rows)
    FX[name] = {
        "name": name, "w": w, "h": len(rows),
        "ragged": len({len(r) for r in rows}) > 1,
        "rows": [r.ljust(w, ".") for r in rows],
    }


def _patch(rows, marks):
    out = [list(r) for r in rows]
    for (x, y), ch in marks.items():
        out[y][x] = ch
    return ["".join(r) for r in out]


# ----------------------------------------------------------------- you, by Body level
#
# All four share a 20x23 frame with the body in the same place, so swapping state never
# makes the character jump: a blank row on top for a cowlick, a column each side for stink
# wiggles. Big head, big eyes with a highlight up and to the left (the room's one light),
# blush — the proportions that read as cute at twenty pixels tall.

_HAIR = [
    ".......kkkkkk.......",
    ".....kkMMMMMMkk.....",
    "....kMMmmMMMMMMk....",
    "...kMmmMMMMMMMMNk...",
    "...kMmMMMMMMMMMNk...",
    "..kMMMMMMMMMMMMMNk..",
    "..kMMMMMMMMMMMMMNk..",
    "..kNMMMMMMMaaMMMNk..",
    "..kNMMMMaaaaaaaMNk..",
]
_BODY = [
    "....kkaaaaaaaakk....",
    "......kkkkkkkk......",
    ".....kLlllllLLk.....",
    "....kAkLLLLLLkAk....",
    "....kAkLLLLLLkAk....",
    ".....kjjjjjjjjk.....",
    ".....kKKkkkkKKk.....",
    ".....kkk....kkk.....",
]
_FACES = {
    # Fresh: sparkly eyes, open smile.
    0: [
        "..kNMaikaaaaikaaNk..",
        "..kNaakkaaaakkaaNk..",
        "..kNaakkaaaakkaaNk..",
        "...kaeaakPPkaaeak...",
        "...kaaaaakkaaaaak...",
    ],
    # Fine: the same eyes, a small smile.
    1: [
        "..kNMaikaaaaikaaNk..",
        "..kNaakkaaaakkaaNk..",
        "..kNaakkaaaakkaaNk..",
        "...kaeaaakkaaaeak...",
        "...kaaaaaaaaaaaak...",
    ],
    # A bit scruffy: lids half down, a smudge.
    2: [
        "..kNMaaaaaaaaaaaNk..",
        "..kNaakkaaaakkaaNk..",
        "..kNaakkaaaakkaaNk..",
        "...kaeaaaKKaaaeak...",
        "...kaaaaaaaaaFaak...",
    ],
    # Needs a wash: heavy lids, tired shadows, smudges.
    3: [
        "..kNMaaaaaaaaaaaNk..",
        "..kNaaaaaaaaaaaaNk..",
        "..kNaakkaaaakkaaNk..",
        "...kaeuuaaaauueak...",
        "...kaFaaaKKaaaaak...",
    ],
}
_EXTRA = {
    0: {},
    1: {},
    2: {(10, 0): "k", (11, 0): "M"},
    3: {(6, 0): "k", (7, 0): "M", (10, 0): "k", (11, 0): "M",
        (9, 18): "u", (13, 17): "u",
        (2, 15): "g", (1, 16): "G", (2, 17): "g", (1, 18): "G",
        (17, 15): "g", (18, 16): "G", (17, 17): "g", (18, 18): "G"},
}
for _lvl in range(4):
    fx(f"me{_lvl}", _patch(["." * 20] + _HAIR + _FACES[_lvl] + _BODY, _EXTRA[_lvl]))


# ----------------------------------------------------------------- Heart, as a companion

_HEART = [
    "..kkk...kkk..",
    ".kiPPk.kPPPk.",
    "kiPPPPkPPPPPk",
    "kPPPPPPPPPPPk",
    "kPPPPPPPPPPPk",
    "kPPPPPPPPPPPk",
    ".kPPPPPPPPPk.",
    "..kPPPPPPPk..",
    "...kPPPPPk...",
    "....kPPPk....",
    ".....kPk.....",
    "......k......",
]


def _heart(face, fill="P", line="k"):
    rows = [r.replace("P", fill).replace("k", line) for r in _HEART]
    return _patch(rows, face)


# Glowing: sparkly eyes, blush, a smile.
fx("buddy0", _heart({(3, 3): "i", (4, 3): "k", (3, 4): "k", (4, 4): "k",
                     (8, 3): "i", (9, 3): "k", (8, 4): "k", (9, 4): "k",
                     (2, 5): "p", (10, 5): "p", (5, 5): "k", (7, 5): "k", (6, 6): "k"}))
# Content.
fx("buddy1", _heart({(4, 3): "k", (8, 3): "k", (4, 4): "k", (8, 4): "k", (6, 5): "k"}))
# Sleepy, a shade paler, one bead of sweat.
fx("buddy2", _heart({(3, 4): "k", (4, 4): "k", (8, 4): "k", (9, 4): "k", (6, 6): "K",
                     (12, 1): "S", (12, 2): "s"}, fill="p"))
# Squeezed eyes and a plaster: overwhelmed, not broken.
fx("buddy3", _heart({(3, 3): "k", (4, 4): "k", (3, 5): "k", (9, 3): "k", (8, 4): "k", (9, 5): "k",
                     (5, 6): "k", (6, 6): "K", (7, 6): "k",
                     (8, 1): "i", (9, 1): "w", (9, 2): "w", (10, 2): "i"}, fill="p", line="K"))


# ----------------------------------------------------------------- today's mission, in hand

fx("letter", [
    "kkkkkkkkk",
    "kvwwwwwvk",
    "kWvwwwvWk",
    "kwWvPvWwk",
    "kwwWPWwwk",
    "kkkkkkkkk",
])

fx("bang", [
    ".k.",
    "kYk",
    "kYk",
    "kYk",
    ".k.",
    "kYk",
    ".k.",
])


# ----------------------------------------------------------------- icons for the task list
#
# The same pixels in the list under the room and on the home-screen widget, so a task and
# the part of the room it changes are visibly the same thing.

fx("icon_body", [
    "....kk....",
    "...kcck...",
    "..kcccCk..",
    ".kciccCCk.",
    ".kciccCCk.",
    "kcicccCCnk",
    "kccccCCCnk",
    "kcccCCCnnk",
    ".kcCCCnnk.",
    "..kkkkkk..",
])

fx("icon_room", [
    "..........kk",
    ".........kFk",
    "........kFk.",
    ".......kFk..",
    "......kFk...",
    "..kkk.kFk...",
    ".kYYYkFk....",
    "kYYYYYk.....",
    "kYoYYYYk....",
    "kYoYoYYk....",
    ".kYoYoYk....",
    "..kkkkk.....",
])

fx("icon_heart", FX["buddy1"]["rows"])

fx("icon_out", [
    "kkkkkkkkkkkkk",
    "kvwwwwwwwwwvk",
    "kWvwwwwwwwvWk",
    "kwWvwwwwwvWwk",
    "kwwWvwPwvWwwk",
    "kwwwWPPPWwwwk",
    "kwwwwwPwwwwwk",
    "kkkkkkkkkkkkk",
])

fx("icon_goal", [
    ".kk........",
    "kKkkkkkk...",
    "kKkTTTTTkk.",
    "kKkTtTTTTTk",
    "kKkTTTTTTk.",
    "kKkTTTkkk..",
    "kKkkkk.....",
    "kKk........",
    "kKk........",
    "kKk........",
    "kKk........",
    ".k.........",
])

fx("icon_done", [
    ".kkkkkkkkk.",
    "kGGGGGGGGGk",
    "kGGGGGGGiGk",
    "kGGGGGGiiGk",
    "kGiGGGiiGGk",
    "kGiiGiiGGGk",
    "kGGiiiGGGGk",
    "kGGGiGGGGGk",
    "khGGGGGGGhk",
    "khhhhhhhhhk",
    ".kkkkkkkkk.",
])

fx("icon_todo", [
    ".KKKKKKKKK.",
    "Kiiiiiiiiik",
    "Kiwwwwwwwik",
    "Kiwwwwwwwik",
    "Kiwwwwwwwik",
    "Kiwwwwwwwik",
    "Kiwwwwwwwik",
    "Kiwwwwwwwik",
    "KiWWWWWWWik",
    "KWWWWWWWWWK",
    ".KKKKKKKKK.",
])


# ----------------------------------------------------------------- hats, worn over you
#
# Each one is drawn in your 20-wide frame from its top row down, so it sits on your head
# whichever state you are in. They come with levels, the way the old avatar's wardrobe did:
# a level now means something you can see on yourself in your own room. No look keys, so
# your hat stays its own colour whatever you picked for your hair and top.

fx("hat_beanie", [
    ".........kk.........",
    "........kyyk........",
    ".......kkkkkk.......",
    ".....kkTTTTTTkk.....",
    "....kTtTTTTTTTTk....",
    "...ktTTTTTTTTTTTk...",
    "..krrrrrrrrrrrrrrk..",
    "..krTrTrTrTrTrTrrk..",
])

fx("hat_flower", [
    "....................",
    "....................",
    ".............kkk....",
    "............kpPpk...",
    "...........kpPyPpk..",
    "............kpPpk...",
    ".............kgk....",
    "............kgk.....",
])

fx("hat_cap", [
    "....................",
    ".......kkkkkk.......",
    ".....kkCCCCCCkk.....",
    "....kCcCCCCCCCCk....",
    "...kCcCCiCCCCCCCk...",
    "...kCCCCCCCCCCCCkkkk",
    "..knnnnnnnnnnnnnnnnk",
])

fx("hat_bucket", [
    "....................",
    ".......kkkkkk.......",
    ".....kkYYYYYYkk.....",
    "....kYyYYYYYYYYk....",
    "....kYyYYYYYYYYk....",
    "..kkoooooooooooookk.",
    ".kYYYYYYYYYYYYYYYYk.",
    "..kkkkkkkkkkkkkkkk..",
])

fx("hat_phones", [
    "....................",
    ".......kkkkkk.......",
    ".....kkKKKKKKkk.....",
    "....kKk......kKk....",
    "...kKk........kKk...",
    "...kKk........kKk...",
    "..kKk..........kKk..",
    "..kKk..........kKk..",
    ".kPPk..........kPPk.",
    "kPpPPk........kPpPPk",
    "kPPPPk........kPPPPk",
    ".kPPk..........kPPk.",
    "..kk............kk..",
])

fx("hat_crown", [
    "....................",
    ".....k...k...k......",
    "....kyk.kyk.kyk.....",
    "....kYYkYYYkYYk.....",
    "....kYiYYYYYYiYk....",
    "....kYYYYPYYYYYk....",
    "....kooooooooook....",
    ".....kkkkkkkkkk.....",
])

# In unlock order, with the level each arrives at. Index 0 is no hat.
HATS = [("hat_beanie", 2, "Beanie"), ("hat_flower", 3, "Flower clip"), ("hat_cap", 4, "Cap"),
        ("hat_bucket", 6, "Bucket hat"), ("hat_phones", 8, "Headphones"), ("hat_crown", 12, "Crown")]


# ----------------------------------------------------------------- the world outside
#
# The three ladders as three roads out of your bubble, each with its own sign: a pin for
# somewhere new, a trainer for something new, two speech bubbles for someone new.

fx("icon_access", [
    "...kkkkk...",
    "..kSSSSSk..",
    ".kSsiSSSbk.",
    "kSsiiiSSSbk",
    "kSSiiiSSSbk",
    "kSSSiSSSSbk",
    ".kSSSSSSbk.",
    ".kSSSSSSbk.",
    "..kSSSSbk..",
    "..kSSSbbk..",
    "...kSbbk...",
    "....kbk....",
    ".....k.....",
])

fx("icon_activity", [
    "...kkkk........",
    "..kGGGGk.......",
    "..kGiGGGk......",
    "..kGGiGGGkk....",
    "..kGiGGiGGGkk..",
    ".kGGGGGGGGGGGk.",
    ".kGGGggggGGGGGk",
    "kGGGgGGGGGGGGhk",
    "kiiiiiiiiiiiiik",
    "kKKKKKKKKKKKKKk",
    ".kkkkkkkkkkkkk.",
])

fx("icon_social", [
    ".kkkkkkkk......",
    "kppppppppk.....",
    "kpkkpkkppk.....",
    "kpppppppPkkkkk.",
    ".kkpPPPPkcccck.",
    "..kpk.kkcCcCck.",
    "..kk..kccccccCk",
    "......kcCCCCCk.",
    ".......kkkCnk..",
    ".........knk...",
    "..........k....",
])

# Courage. Every time something did not go to plan and you said so, a spark goes in the jar.
# Friction used to be armour plates on a different avatar in a different style; it is the
# same idea — evidence you went to the edge — kept somewhere you can see it filling up.

fx("icon_spark", [
    ".....k.....",
    "....kYk....",
    "....kYk....",
    "...kYyYk...",
    ".kkYyiyYkk.",
    "kYYyiiiyYYk",
    ".kkYyiyYkk.",
    "...kYyYk...",
    "....kYk....",
    "....kYk....",
    ".....k.....",
])

_JAR = [
    "...kkkkkkk...",
    "..kTttttTTk..",
    "..kkkkkkkkk..",
    "...kiqqqqk...",
    "..kiqqqqqqk..",
    ".kiqqqqqqqqk.",
    ".kiqqqqqqqqk.",
    ".kiqqqqqqqqk.",
    ".kiqqqqqqqqk.",
    ".kiqqqqqqqqk.",
    ".kqqqqqqqqQk.",
    ".kqqqqqqqQQk.",
    "..kQQQQQQQk..",
    "...kkkkkkk...",
]


def _star(cx, cy):
    return {(cx, cy - 1): "Y", (cx - 1, cy): "Y", (cx, cy): "i", (cx + 1, cy): "Y", (cx, cy + 1): "Y"}


_SPARKS = [_star(5, 10), _star(8, 9), _star(4, 7), _star(8, 5)]
for _n in range(5):
    _marks = {}
    for _s in _SPARKS[:_n]:
        _marks.update(_s)
    if _n == 4:
        _marks.update({(6, 4): "y", (3, 11): "y", (9, 11): "y"})
    fx(f"jar{_n}", _patch(_JAR, _marks))

# A reminder.
fx("icon_bell", [
    ".....k.....",
    "....kok....",
    "...kYYYk...",
    "..kyiYYok..",
    "..kyYYYok..",
    ".kyiYYYYok.",
    ".kyYYYYYok.",
    ".kyYYYYYok.",
    "kyYYYYYYYok",
    "kkkkkkkkkkk",
    "....kook...",
    ".....kk....",
])

# A postcard home: what you write after a mission, and every note in your journal.
fx("icon_postcard", [
    "kkkkkkkkkkkkkk",
    "kiiiiiiivkkkkk",
    "kiKKKKiivkPpPk",
    "kiiiiiiivkpPpk",
    "kiKKKKKivkPpPk",
    "kiiiiiiivkkkkk",
    "kiKKKiiiviiiik",
    "kiiiiiiivKKKik",
    "kkkkkkkkkkkkkk",
])

# A book, for Learn.
fx("icon_book", [
    ".kkkkkkkkkk.",
    "kCCCCCCCCCik",
    "kCcCCCCCCCik",
    "kCcCkkkkCCik",
    "kCcCkiikCCik",
    "kCcCkkkkCCik",
    "kCcCCCCCCCik",
    "kCcCCCCCCCik",
    "kCcCCCCCCCik",
    "knnnnnnnnnik",
    "kkkkkkkkkkik",
    ".kiiiiiiiiik",
    "..kkkkkkkkk.",
])

# ----------------------------------------------------------------- where a journey goes
#
# Each goal is a destination at the end of a thirty-stop trail.

fx("goal_interesting", [
    ".kk.........kk.",
    "kWWkkkkkkkkkWWk",
    "kWvwwwwwwwwwWvk",
    "kWvwgwwwwrwrWvk",
    "kWvwGwwwwwrwWvk",
    "kWvwwTwwwrwrWvk",
    "kWvwwwTwwwwwWvk",
    "kWvwwwwTTwwwWvk",
    "kWvwwwwwwTwwWvk",
    "kWWkkkkkkkkkWWk",
    ".kk.........kk.",
])

fx("goal_friends", [
    ".......k........",
    "......kYk.......",
    ".....kYyk..k....",
    "....kYyYk.kYk...",
    "...kYyiyYkkYk...",
    "...kYyiiyYYyYk..",
    "..kYyiiiiyYyYk..",
    "..kYYyiiyYYYok..",
    "..koYYyyYYYook..",
    ".kTkkoooooookTk.",
    "kTtTTkkkkkkTTtTk",
    "krrTtTTTTTTtTrrk",
    ".kkrrrrrrrrrrkk.",
    "..kkkkkkkkkkkk..",
])

fx("goal_partner", [
    ".kkk.kkk.......",
    "kPpPkPPPk......",
    "kpPPPPPPk......",
    "kPPPPPPPkkk.kk.",
    ".kPPPPPkkppkppk",
    "..kPPPk.kpppppk",
    "...kPk...kpppk.",
    "....k.....kpk..",
    "...........k...",
])

fx("goal_intimacy", [
    "....k....",
    "...kYk...",
    "...kyk...",
    "..kYiYk..",
    "...kok...",
    "..kkkkk..",
    ".kiwwwvk.",
    ".kiwwwvk.",
    ".kiwwwvk.",
    ".kiwwwvk.",
    ".kiwwwvk.",
    "kkkkkkkkk",
    "kTTTTTTTk",
    ".kkkkkkk.",
])

fx("goal_craft", [
    "....kkkkkkk....",
    "..kkfffffffkk..",
    ".kfYYffPPffffk.",
    "kfYyYfPpPffffFk",
    "kffYffPPffkkfFk",
    "kfCCfffffk..kFk",
    "kfCcCffGGfkkFFk",
    ".kfCffGgGfFFFk.",
    "..kkFFFGFFFkk..",
    "....kkkkkkk....",
])

# ----------------------------------------------------------------- the tab bar

fx("tab_home", [
    "......kk......",
    ".....kTTk.....",
    "....kTttTk....",
    "...kTtTTTTk...",
    "..kTtTTTTTTk..",
    ".kTTTTTTTTTTk.",
    "kkkkkkkkkkkkkk",
    ".kwwwwwwwwwwk.",
    ".kwkkkwwkkkwk.",
    ".kwkYkwwkSkwk.",
    ".kwkYkwwkkkwk.",
    ".kwkYkwwwwwvk.",
    ".kkkkkkkkkkkk.",
])

fx("tab_out", [
    "......kk......",
    ".kkkkkkFkkkk..",
    ".kYYYYYYYYYYk.",
    ".kyYYYYYYYYYYk",
    ".kYYYYYYYYYYk.",
    ".kkkkkkFkkkk..",
    "..kkkkkFkkkkk.",
    ".kCCCCCCCCCCk.",
    "kCcCCCCCCCCCk.",
    ".kCCCCCCCCCCk.",
    "..kkkkkFkkkkk.",
    "......kFk.....",
    "....kgkFkgk...",
    "...kgGGGGGgk..",
])

fx("tab_journal", [
    ".kkkkkkkkkk...",
    ".kTttttttTkk..",
    ".kTwwwwwwTkik.",
    ".kTwKKKKwTkik.",
    ".kTwwwwwwTkik.",
    ".kTwKKKKwTkik.",
    ".kTwwwwwwTkik.",
    ".kTwKKKwwTkik.",
    ".kTwwwwwwTkik.",
    ".kTTTTTTTTkik.",
    ".kkkkkkkkkkkk.",
    "....kPk.......",
    "....kPk.......",
    ".....k........",
])

fx("tab_library", [
    "..kkkkkkkkk...",
    "..kCCCCCCCck..",
    "..kCiiiiiiCk..",
    "..knnnnnnnnk..",
    ".kkkkkkkkkkkk.",
    ".kPPPPPPPPPPk.",
    ".kPiiiiiiiiPk.",
    ".kPPPPPPPPPPk.",
    "..kkkkkkkkkkk.",
    "..kYYYYYYYYYk.",
    "..kYiiiiiiiYk.",
    "..koooooooook.",
    "..kkkkkkkkkkk.",
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

ME_SPOT = (70, 96)        # (x, baseline) for the character by default
ME_W, ME_H = 20, 23

# The heart buddy floats at your shoulder: its left edge BUDDY_DX right of yours, its bottom
# BUDDY_DY above your feet. It bobs a beat behind you. If that would leave the room it goes
# on your other side, and the letter swaps with it.
BUDDY_DX, BUDDY_DY = 19, 11

# Today's letter, held at your other hand: its left edge LETTER_DX from yours (negative is
# to your left), its bottom LETTER_DY above your feet. The "!" floats BANG_GAP over it.
LETTER_DX, LETTER_DY = -5, 3
BANG_GAP = 2

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
    if sizes != {(ME_W, ME_H)}:
        problems.append(f"the four 'me' frames must all be {ME_W}x{ME_H}: {sizes}")
    if len({(FX[f"buddy{i}"]["w"], FX[f"buddy{i}"]["h"]) for i in range(MAX_DIRT + 1)}) != 1:
        problems.append("the four buddy frames differ in size")
    # The look swap is by exact colour, so the look keys must be the only skin, hair and top
    # in the character, and must appear nowhere else in the room.
    from palette import LOOK_KEYS
    look = set("".join(LOOK_KEYS.values()))
    for n, s in FX.items():
        keys = {ch for r in s["rows"] for ch in r}
        if not n.startswith("me") and keys & look:
            problems.append(f"{n} uses a look colour {sorted(keys & look)}; only 'me' may")
    for name, *_ in ROOM_MESS:
        if name not in FX:
            problems.append(f"ROOM_MESS names unknown fx {name}")
    return problems


if __name__ == "__main__":
    print(check() or "ok")
