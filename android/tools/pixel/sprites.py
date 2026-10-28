"""The things you put in the room.

Hand-authored, one character per pixel, keys from palette.py. Organic things (plants, the
cat, fabric) are typed out as grids because every pixel is a decision at this size; furniture
is built from the same primitives the room uses, because a bed is rectangles and typing 576
characters to say so produces typos, not craft.

Every item belongs to a FAMILY, and the family is what earns it:

  BODY    looking after yourself       a bed, water, trainers, a mirror...
  ROOM    looking after your space     a lamp, a rug, a laundry basket...
  HEART   looking after how you feel   a plant, the cat, a journal desk...
  WORLD   missions outside the bubble  a map, a backpack, a bike...

So what arrives in the room is always related to what you just did. You start with none of
it. `first` marks the one item each family gives on its first day, chosen because it changes
the empty room the most; after that, which item arrives is a surprise.

`spot` is where an item lands when it arrives, as (x, baseline) in room pixels. On the floor
the baseline IS depth, so a bigger number is nearer the viewer. `flat` things (a rug, a mat)
lie on the floor and are drawn underneath everything that stands.

Light comes from the upper left in all of them. No pure black anywhere.
"""

import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from room import Canvas  # noqa: E402

FAMILIES = ("BODY", "ROOM", "HEART", "WORLD")

SPRITES = {}


def sprite(name, label, family, anchor, spot, rows, first=False, flat=False):
    assert family in FAMILIES, name
    assert anchor in ("floor", "wall"), name
    w = max(len(r) for r in rows)
    SPRITES[name] = {
        "name": name,
        "label": label,
        "family": family,
        "anchor": anchor,          # "floor" | "wall"
        "spot": spot,              # (x, baseline) where it lands when earned
        "first": first,
        "flat": flat,
        "w": w,
        "h": len(rows),
        # Ragged rows are padded on the right, which shifts nothing; `check()` still flags
        # them, because a ragged row in an authored grid is nearly always a typo.
        "ragged": len({len(r) for r in rows}) > 1,
        "rows": [r.ljust(w, ".") for r in rows],
    }


def circle(c, cx, cy, r, ch):
    for y in range(cy - r, cy + r + 1):
        for x in range(cx - r, cx + r + 1):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r + r * 0.6:
                c.set(x, y, ch)


def ring(c, cx, cy, r, ch):
    for y in range(cy - r - 1, cy + r + 2):
        for x in range(cx - r - 1, cx + r + 2):
            d = (x - cx) ** 2 + (y - cy) ** 2
            if r * r - r <= d <= r * r + r:
                c.set(x, y, ch)


def line(c, x0, y0, x1, y1, ch):
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        c.set(x0, y0, ch)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy; x0 += sx
        if e2 <= dx:
            err += dx; y0 += sy


# ----------------------------------------------------------------- floor things

sprite("plant", "Monstera", "HEART", "floor", (56, 72), [
    ".....hh...hh....",
    "....hGGh.hGGh...",
    "...hGggGhGggGh..",
    "...hGgGGGGGgGh..",
    "....hGGGhGGGh...",
    "...hGGh.h.hGGh..",
    "..hGggGh.hGggGh.",
    "..hGgGGh.hGGgGh.",
    "...hGGGh.hGGGh..",
    ".....hGh.hGh....",
    "......hGGGh.....",
    ".......hGh......",
    ".......hGh......",
    ".......hGh......",
    "....kkkkkkkk....",
    "....ktttttTk....",
    "....ktttTTTk....",
    ".....kTTTrk.....",
    ".....kTTrrk.....",
    "......kkkk......",
], first=True)

sprite("lamp", "Floor lamp", "ROOM", "floor", (40, 70), [
    "....kkkkkk....",
    "...kyyyyyyk...",
    "..kyyyyyyyyk..",
    "..kyyyyyyyyk..",
    "..kyyyYYYYYk..",
    "...kYYYYYYk...",
    "....kkKkkk....",
    "......Kk......",
    "......Kk......",
    "......Kk......",
    "......Kk......",
    "......Kk......",
    "......Kk......",
    "......Kk......",
    "......Kk......",
    "......Kk......",
    "......Kk......",
    "......Kk......",
    "......Kk......",
    "......Kk......",
    "....kkKKkk....",
    "...kKKKKKKk...",
    "..kKKKKKKKKk..",
    "...kkkkkkkk...",
], first=True)

sprite("rug", "Round rug", "ROOM", "floor", (52, 92), [
    "......kkkkkkkkkkkk......",
    "...kkkTTTTTTTTTTTTkkk...",
    ".kkTTTtttttttttttttTTkk.",
    "kTTtttccccccccccccttTTTk",
    "kTttccccCCCCCCcccccctTTk",
    "kTttccCCCCCCCCCCcccctTTk",
    "kTTtttccccccccccccttTTTk",
    ".kkTTTtttttttttttttTTkk.",
    "...kkkTTTTTTTTTTTTkkk...",
    "......kkkkkkkkkkkk......",
], flat=True)

sprite("cat", "Sleeping cat", "HEART", "floor", (56, 89), [
    ".....kk.......kk....",
    "....kpfk.....kfpk...",
    "...kffffkkkkkffffk..",
    "..kfffffffffffffffk.",
    ".kffkkfffffffkkffffk",
    ".kfffffffPffffffffFk",
    ".kFfffffffffffffffFk",
    ".kFFFFFFFFFFFFFFFFFk",
    "..kFFFFFFFFFFFFFFFk.",
    "..kdFFFFFFFFFFFFdFk.",
    "...kddddddddddddkk..",
    "....kkkkkkkkkkkk....",
])

sprite("beanbag", "Beanbag", "ROOM", "floor", (108, 88), [
    ".....kkkkkk.....",
    "...kkCCCCCCkk...",
    "..kCCccccccCCk..",
    ".kCCcccccccccCk.",
    "kCCccccccccccCCk",
    "kCcccccccccccCCk",
    "kCCcccccccccCCCk",
    "kCCCcccccccCCCnk",
    ".kCCCCCCCCCCCnk.",
    "..kknnnnnnnnkk..",
    "....kkkkkkkk....",
])

sprite("books", "Book stack", "WORLD", "floor", (100, 97), [
    "..kkkkkkkkk..",
    "..kTTTTTTTk..",
    "..kkkkkkkkk..",
    ".kCCCCCCCCCk.",
    ".kkkkkkkkkkk.",
    "kGGGGGGGGGGGk",
    "kkkkkkkkkkkkk",
])

sprite("guitar", "Guitar", "WORLD", "floor", (147, 84), [
    "....kkkk....",
    "...kiiiik...",
    "...kiiiik...",
    "....kkkk....",
    ".....kk.....",
    "....kYYk....",
    "....kYYk....",
    "....kYYk....",
    "....kYYk....",
    "....kYYk....",
    "....kYYk....",
    "...kkYYkk...",
    "..kTtYYtTk..",
    ".kTttYYttTk.",
    "kTttkkkkttTk",
    "kTttkKKkttTk",
    "kTttkKKkttTk",
    "kTttkkkkttTk",
    "kTtttYYtttTk",
    "kTttttttttTk",
    ".kTtttttttk.",
    ".kTTttttTTk.",
    "..kTTTTTTk..",
    "...kkkkkk...",
])

sprite("record", "Record player", "HEART", "floor", (90, 72), [
    "kkkkkkkkkkkkkkk",
    "kwwwwwwwwwwwwwk",
    "kwkkkkkkkkwwwwk",
    "kwkKKKKKKkwwwwk",
    "kwkKKkkKKkwwYwk",
    "kwkKkiikKkwwwwk",
    "kwkKKkkKKkwwwwk",
    "kwkKKKKKKkwTwwk",
    "kwkkkkkkkkwwwwk",
    "kwwwwwwwwwwwwwk",
    "kkkkkkkkkkkkkkk",
])

# ----------------------------------------------------------------- wall things

sprite("poster", "Poster", "HEART", "wall", (128, 40), [
    "kkkkkkkkkkkk",
    "kiiiiiiiiiik",
    "kiSSSSSSSSik",
    "kiSSsssSSSik",
    "kiSssssssSik",
    "kiSSsssSSSik",
    "kiSSSSSSSSik",
    "kiGGGGGGGGik",
    "kiGhGhGhGGik",
    "kiiiiiiiiiik",
    "kkkkkkkkkkkk",
])

sprite("clock", "Clock", "ROOM", "wall", (112, 18), [
    "...kkkkkk...",
    "..kwwwwwwk..",
    ".kwwiiiiwwk.",
    "kwwikkkkiwwk",
    "kwiikkkkiiwk",
    "kwikkKkkkiwk",
    "kwikkKKKkiwk",
    "kwiikkkkiiwk",
    "kwwikkkkiwwk",
    ".kwwiiiiwwk.",
    "..kwwwwwwk..",
    "...kkkkkk...",
])

sprite("shelf", "Shelf", "ROOM", "wall", (6, 34), [
    ".kTk.kCk.kGk.kPk.",
    ".kTk.kCk.kGk.kPk.",
    ".kTk.kCk.kGk.kPk.",
    ".kTk.kCk.kGk.kPk.",
    "kkkkkkkkkkkkkkkkk",
    "kfffffffffffffffk",
    "kdddddddddddddddk",
    "kkkkkkkkkkkkkkkkk",
])

sprite("lights", "String lights", "HEART", "wall", (68, 9), [
    "k..k..k..k..k..k..k..k",
    ".k.k.k.k.k.k.k.k.k.k.k",
    "..k...k...k...k...k...",
    ".kyk.kYk.kyk.kYk.kyk..",
    ".kyk.kYk.kyk.kYk.kyk..",
    "..k...k...k...k...k...",
])

sprite("photo", "Framed photo", "HEART", "wall", (26, 28), [
    "kkkkkkkkk",
    "kYYYYYYYk",
    "kYiiiiiYk",
    "kYiPPPiYk",
    "kYiPpPiYk",
    "kYiiiiiYk",
    "kYYYYYYYk",
    "kkkkkkkkk",
])

sprite("pennant", "Pennant", "WORLD", "wall", (142, 22), [
    "kkkkkkkkkkkkk",
    "kTTTTTTTTTTTk",
    ".kTTyyyyTTTk.",
    "..kTTyyTTTk..",
    "...kTTTTTk...",
    "....kTTTk....",
    ".....kTk.....",
    "......k......",
])

sprite("window_box", "Window box", "WORLD", "wall", (70, 53), [
    "..g..G..g..G..g..",
    ".gGg.gGg.gGg.gGg.",
    "gGhGgGhGgGhGgGhGg",
    "kkkkkkkkkkkkkkkkk",
    "kTtttttttttttttTk",
    "kTTtttttttttttTTk",
    "kkkkkkkkkkkkkkkkk",
])


sprite("cushion", "Cushion", "HEART", "floor", (82, 88), [
    "..kkkkkkkk..",
    ".kpppppppPk.",
    "kppppppppPPk",
    "kpppppppppPk",
    "kPppppppppPk",
    "kPPpppppppPk",
    ".kPPPPPPPPk.",
    "..kkkkkkkk..",
])


# =================================================================== BODY

def _bed():
    c = Canvas(38, 17)
    c.rect(0, 0, 5, 17, "T"); c.rect(1, 1, 1, 15, "t"); c.frame(0, 0, 5, 17, "k")   # headboard
    c.rect(33, 6, 5, 11, "T"); c.rect(34, 7, 1, 9, "t"); c.frame(33, 6, 5, 11, "k")  # footboard
    c.rect(5, 11, 28, 3, "T"); c.rect(5, 13, 28, 1, "r"); c.rect(5, 14, 28, 1, "k")  # base
    c.rect(5, 8, 28, 3, "i"); c.rect(5, 10, 28, 1, "w")                              # mattress
    c.rect(12, 5, 21, 5, "p"); c.rect(12, 9, 21, 1, "P"); c.rect(12, 4, 21, 1, "k")  # duvet
    c.rect(11, 4, 1, 6, "k")
    for x in range(14, 32, 4):                                                       # quilting
        c.set(x, 7, "P")
    c.rect(32, 5, 1, 5, "P")
    c.rect(5, 4, 7, 4, "i"); c.frame(5, 3, 7, 5, "k"); c.set(6, 4, "w")              # pillow
    c.rect(5, 15, 1, 2, "k"); c.rect(32, 15, 1, 2, "k")                               # shadows
    return c.rows()


sprite("bed", "Bed", "BODY", "floor", (2, 76), _bed(), first=True)

sprite("water", "Water jug", "BODY", "floor", (116, 95), [
    "...kk..",
    "..kiik.",
    "..kkkk.",
    ".kcsssk",
    "kcsssSk",
    "kcSSSSk",
    "kcSbSSk",
    "kcSSSSk",
    "kcSSSbk",
    ".kkkkk.",
])

sprite("fruit", "Fruit bowl", "BODY", "floor", (124, 98), [
    "....kkk.....",
    "..kkgGkkk...",
    ".kTtkGkYyyk.",
    "kTttTkYYYYok",
    "kkkkkkkkkkkk",
    ".kccccccCCk.",
    "..kCCCCCCk..",
    "...kkkkkk...",
])

sprite("sneakers", "Trainers", "BODY", "floor", (30, 98), [
    "kkk.....kkk....",
    "kCCk....kCCk...",
    "kCiCkk..kCiCkk.",
    "kCCCCCk.kCCCCCk",
    "kiiiiik.kiiiiik",
    "kkkkkkk.kkkkkkk",
])

sprite("yoga_mat", "Yoga mat", "BODY", "floor", (4, 97), [
    "..kkkkkkkkkkkkkkkkkkkkkkk.",
    ".kppppppppppppppppppppppPk",
    "kpPppppppppppppppppppppPPk",
    "kPPPPPPPPPPPPPPPPPPPPPPPPk",
    ".kkkkkkkkkkkkkkkkkkkkkkkk.",
], flat=True)

sprite("dumbbells", "Dumbbells", "BODY", "floor", (14, 88), [
    ".kk.......kk.",
    "kCnk.....kCnk",
    "kCnkkkkkkkCnk",
    "kCnkKKKKKkCnk",
    "kCnkkkkkkkCnk",
    "kCnk.....kCnk",
    ".kk.......kk.",
])

sprite("mirror", "Mirror", "BODY", "wall", (46, 40), [
    "...kkkkk...",
    "..kYYYYYk..",
    ".kYsssssYk.",
    "kYsisssSSYk",
    "kYssissSSYk",
    "kYsssisSSYk",
    "kYssssiSSYk",
    "kYsssssSSYk",
    "kYssssSSSYk",
    "kYsssSSSSYk",
    "kYsssSSSbYk",
    "kYssSSSSbYk",
    "kYsSSSSbbYk",
    "kYoooooooYk",
    "kkkkkkkkkkk",
])

sprite("towel", "Towel", "BODY", "wall", (100, 44), [
    "kkkkkkkkkkkk",
    "kKKKKKKKKKKk",
    ".kkkkkkkkkk.",
    ".kccccccCCk.",
    ".kcccccccCk.",
    ".kiiiiiiiik.",
    ".kcccccccCk.",
    ".kcccccccCk.",
    ".kccccccCCk.",
    ".kiiiiiiiik.",
    ".kcccccccCk.",
    ".kCCCCCCCCk.",
    ".kkkkkkkkkk.",
])

sprite("slippers", "Slippers", "BODY", "floor", (48, 98), [
    ".kkkkk.kkkkk.",
    "kiPPppkiPPppk",
    "kPPPPPkPPPPPk",
    ".kkkkk.kkkkk.",
])


# =================================================================== ROOM

sprite("basket", "Laundry basket", "ROOM", "floor", (30, 86), [
    "..kk..kkk....",
    ".kPPk.kCCkk..",
    ".kPpkkCccCk..",
    "kkkkkkkkkkkkk",
    "kYoYoYoYoYoYk",
    "koYoYoYoYoYok",
    "kYoYoYoYoYoYk",
    "koYoYoYoYoYok",
    "kYoYoYoYoYoYk",
    "koYoYoYoYoYok",
    ".kkkkkkkkkkk.",
])

sprite("bin", "Bin", "ROOM", "floor", (150, 95), [
    ".kkkkkkk.",
    "kCCCCCCCk",
    "kkkkkkkkk",
    ".kcCccCk.",
    ".kcCccCk.",
    ".kcCccCk.",
    ".kcCccCk.",
    ".kcCccCk.",
    ".kCCCCCk.",
    ".kkkkkkk.",
])

sprite("broom", "Broom", "ROOM", "floor", (153, 74), [
    "..kk...",
    "..kTk..",
    "..ktk..",
    "..kTk..",
    "..ktk..",
    "..kTk..",
    "..ktk..",
    "..kTk..",
    "..ktk..",
    "..kTk..",
    "..ktk..",
    "..kTk..",
    "..ktk..",
    ".kkkkk.",
    ".kKKKk.",
    ".kYYYk.",
    "kYYoYYk",
    "kYoYoYk",
    "kYoYoYk",
    "kYoYoYk",
    "kkkkkkk",
])


def _boxes():
    c = Canvas(17, 15)
    c.rect(0, 6, 17, 9, "F"); c.rect(1, 7, 7, 7, "f"); c.frame(0, 6, 17, 9, "k")
    c.rect(1, 9, 15, 1, "v"); c.rect(10, 11, 4, 2, "i")           # tape and a label
    c.rect(3, 0, 11, 7, "f"); c.rect(4, 1, 5, 5, "y"); c.frame(3, 0, 11, 7, "k")
    c.rect(4, 3, 9, 1, "v")
    c.set(11, 4, "P"); c.set(12, 4, "P")
    return c.rows()


sprite("boxes", "Storage boxes", "ROOM", "floor", (136, 72), _boxes())


# =================================================================== HEART

def _desk():
    c = Canvas(26, 19)
    c.rect(0, 7, 26, 2, "F"); c.rect(0, 7, 26, 1, "f"); c.frame(0, 7, 26, 3, "k")    # top
    c.rect(1, 10, 2, 9, "d"); c.frame(0, 10, 4, 9, "k")                            # legs
    c.rect(23, 10, 2, 9, "d"); c.frame(22, 10, 4, 9, "k")
    c.rect(14, 10, 8, 4, "F"); c.frame(13, 10, 10, 5, "k"); c.set(17, 12, "Y")    # drawer
    c.rect(2, 4, 10, 3, "i"); c.frame(1, 3, 12, 5, "k"); c.rect(7, 3, 1, 4, "K")  # open journal
    for y in (4, 5):
        c.rect(3, y, 3, 1, "v"); c.rect(8, y, 3, 1, "v")
    c.rect(15, 2, 4, 4, "c"); c.frame(14, 1, 6, 6, "k"); c.rect(20, 3, 1, 2, "k")  # mug
    c.set(16, 0, "q"); c.set(17, 0, "q"); c.set(15, 0, ".")
    return c.rows()


sprite("desk", "Journal desk", "HEART", "floor", (108, 74), _desk())

sprite("candle", "Candle", "HEART", "floor", (92, 98), [
    "...y...",
    "..yYy..",
    "..YoY..",
    "...k...",
    ".kkkkk.",
    ".kiiwk.",
    ".kiiwk.",
    ".kiiwk.",
    ".kiwwk.",
    "kYYYYYk",
    "kkkkkkk",
])


# =================================================================== WORLD

def _map():
    c = Canvas(20, 13)
    c.rect(0, 0, 20, 13, "F"); c.frame(0, 0, 20, 13, "k")
    c.rect(2, 2, 16, 9, "c"); c.frame(1, 1, 18, 11, "d")
    for x, y, w, h in ((3, 3, 4, 3), (4, 6, 2, 3), (9, 3, 3, 2), (9, 5, 2, 4),
                       (13, 3, 4, 3), (15, 6, 2, 2)):
        c.rect(x, y, w, h, "G")
    for x, y in ((4, 3), (13, 3), (9, 3)):
        c.set(x, y, "g")
    c.set(10, 6, "T"); c.set(14, 4, "T")                                         # pins
    return c.rows()


sprite("map", "World map", "WORLD", "wall", (122, 30), _map(), first=True)

sprite("backpack", "Backpack", "WORLD", "floor", (96, 82), [
    "...kkkkkk...",
    "..kTTTTTTk..",
    ".kTttttttTk.",
    ".kTtkkkktTk.",
    "kkTtkYYktTkk",
    "kTTttttttTTk",
    "kTkkkkkkkkTk",
    "kTkttttttkTk",
    "kTkttttttkTk",
    "kTkkkkkkkkTk",
    "kTTTTTTTTTTk",
    "krrrrrrrrrrk",
    ".kkkkkkkkkk.",
])


def _globe():
    c = Canvas(13, 17)
    circle(c, 6, 6, 5, "C")
    circle(c, 5, 5, 4, "c")
    for x, y in ((3, 3), (4, 3), (3, 4), (4, 5), (8, 6), (8, 7), (7, 7), (6, 9), (9, 4)):
        c.set(x, y, "G")
    c.set(3, 4, "g")
    ring(c, 6, 6, 6, "k")
    for y in range(1, 12):                                    # the brass meridian
        c.set(12 if y in (5, 6, 7) else (11 if y in (3, 4, 8, 9) else 10), y, "Y")
    c.rect(6, 13, 1, 2, "o"); c.rect(3, 15, 7, 1, "Y"); c.rect(3, 16, 7, 1, "k")
    c.set(6, 12, "k")
    return c.rows()


sprite("globe", "Globe", "WORLD", "floor", (136, 98), _globe())


def _bike():
    c = Canvas(32, 18)
    for cx in (6, 25):
        ring(c, cx, 11, 5, "k")
        for a, b in ((-3, 0), (3, 0), (0, -3), (0, 3)):
            c.set(cx + a, 11 + b, "K")
        c.set(cx, 11, "K")
    line(c, 6, 11, 13, 11, "T"); line(c, 13, 11, 10, 4, "T")      # chain stay, seat tube
    line(c, 10, 4, 21, 4, "T"); line(c, 13, 11, 21, 4, "T")       # top tube, down tube
    line(c, 21, 4, 25, 11, "T"); line(c, 6, 11, 10, 4, "t")       # fork, seat stay
    c.rect(8, 2, 5, 1, "k"); c.rect(9, 3, 1, 1, "k")              # saddle
    line(c, 21, 4, 20, 1, "k"); c.rect(19, 1, 4, 1, "k")          # stem and bars
    c.rect(12, 10, 3, 3, "K")                                     # crank
    return c.rows()


sprite("bike", "Bike", "WORLD", "floor", (118, 82), _bike())


def _postcards():
    c = Canvas(17, 12)
    c.rect(0, 2, 8, 6, "i"); c.rect(1, 3, 6, 2, "S"); c.rect(1, 5, 6, 2, "f")
    c.frame(0, 2, 8, 6, "k")
    c.rect(6, 5, 7, 6, "i"); c.rect(7, 6, 5, 3, "G"); c.rect(7, 6, 5, 1, "s")
    c.frame(6, 5, 7, 6, "k"); c.set(11, 9, "P")
    c.rect(11, 0, 6, 6, "i"); c.rect(12, 1, 4, 2, "T"); c.rect(12, 3, 4, 2, "Y")
    c.frame(11, 0, 6, 6, "k")
    for x, y in ((3, 1), (9, 4), (13, 0)):
        c.set(x, y, "P")                                          # pins
    return c.rows()


sprite("postcards", "Postcards", "WORLD", "wall", (8, 52), _postcards())


def check():
    """Authoring checks that would otherwise only show up as a wrong-looking room."""
    problems = []
    from palette import PALETTE
    for n, s in SPRITES.items():
        if s["ragged"]:
            problems.append(f"{n}: ragged rows")
        bad = {ch for r in s["rows"] for ch in r} - set(PALETTE)
        if bad:
            problems.append(f"{n}: unknown palette keys {bad}")
    for fam in FAMILIES:
        members = [s for s in SPRITES.values() if s["family"] == fam]
        if sum(1 for s in members if s["first"]) != 1:
            problems.append(f"{fam}: needs exactly one first item")
    return problems


if __name__ == "__main__":
    for fam in FAMILIES:
        print(fam, [n for n, s in SPRITES.items() if s["family"] == fam])
    print(check() or "ok")
