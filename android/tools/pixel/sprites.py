"""The things you put in the room.

Hand-authored as character grids, one character per pixel, keys from palette.py. Authored
rather than generated: at 16x16 every pixel is a decision, and a generator that places them
produces the mush that makes pixel art look machine-made.

Light comes from the upper left in all of them. Each has a warm outline rather than black,
and sits on the floor line or hangs flat on the wall — `anchor` says which, because that is
what decides where the room will let you put it.
"""

SPRITES = {}


def sprite(name, label, anchor, unlock, rows):
    w = max(len(r) for r in rows)
    SPRITES[name] = {
        "name": name,
        "label": label,
        "anchor": anchor,          # "floor" | "wall"
        "unlock": unlock,          # (currency, amount)
        "w": w,
        "h": len(rows),
        "rows": [r.ljust(w, ".") for r in rows],
    }


# ----------------------------------------------------------------- floor things

sprite("plant", "Monstera", "floor", ("rungs", 0), [
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
])

sprite("lamp", "Floor lamp", "floor", ("level", 2), [
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
])

sprite("rug", "Round rug", "floor", ("rungs", 6), [
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
])

sprite("cat", "Sleeping cat", "floor", ("friction", 8), [
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

sprite("beanbag", "Beanbag", "floor", ("rungs", 20), [
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

sprite("books", "Book stack", "floor", ("level", 5), [
    "..kkkkkkkkk..",
    "..kTTTTTTTk..",
    "..kkkkkkkkk..",
    ".kCCCCCCCCCk.",
    ".kkkkkkkkkkk.",
    "kGGGGGGGGGGGk",
    "kkkkkkkkkkkkk",
])

sprite("guitar", "Guitar", "floor", ("level", 8), [
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

sprite("record", "Record player", "floor", ("friction", 16), [
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

sprite("poster", "Poster", "wall", ("rungs", 0), [
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

sprite("clock", "Clock", "wall", ("level", 3), [
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

sprite("shelf", "Shelf", "wall", ("rungs", 45), [
    ".kTk.kCk.kGk.kPk.",
    ".kTk.kCk.kGk.kPk.",
    ".kTk.kCk.kGk.kPk.",
    ".kTk.kCk.kGk.kPk.",
    "kkkkkkkkkkkkkkkkk",
    "kfffffffffffffffk",
    "kdddddddddddddddk",
    "kkkkkkkkkkkkkkkkk",
])

sprite("lights", "String lights", "wall", ("friction", 4), [
    "k..k..k..k..k..k..k..k",
    ".k.k.k.k.k.k.k.k.k.k.k",
    "..k...k...k...k...k...",
    ".kyk.kYk.kyk.kYk.kyk..",
    ".kyk.kYk.kyk.kYk.kyk..",
    "..k...k...k...k...k...",
])

sprite("photo", "Framed photo", "wall", ("rungs", 90), [
    "kkkkkkkkk",
    "kYYYYYYYk",
    "kYiiiiiYk",
    "kYiPPPiYk",
    "kYiPpPiYk",
    "kYiiiiiYk",
    "kYYYYYYYk",
    "kkkkkkkkk",
])

sprite("pennant", "Pennant", "wall", ("level", 10), [
    "kkkkkkkkkkkkk",
    "kTTTTTTTTTTTk",
    ".kTTyyyyTTTk.",
    "..kTTyyTTTk..",
    "...kTTTTTk...",
    "....kTTTk....",
    ".....kTk.....",
    "......k......",
])

sprite("window_box", "Window box", "wall", ("rungs", 150), [
    "..g..G..g..G..g..",
    ".gGg.gGg.gGg.gGg.",
    "gGhGgGhGgGhGgGhGg",
    "kkkkkkkkkkkkkkkkk",
    "kTtttttttttttttTk",
    "kTTtttttttttttTTk",
    "kkkkkkkkkkkkkkkkk",
])

sprite("mug", "Mug", "floor", ("rungs", 0), [
    "..kkkk...",
    ".kwwwwk..",
    "kwccccwkk",
    "kwccccwkw",
    "kwccccwkw",
    "kwccccwkk",
    ".kwwwwk..",
    "..kkkk...",
])

sprite("cushion", "Cushion", "floor", ("rungs", 0), [
    "..kkkkkkkk..",
    ".kpppppppPk.",
    "kppppppppPPk",
    "kpppppppppPk",
    "kPppppppppPk",
    "kPPpppppppPk",
    ".kPPPPPPPPk.",
    "..kkkkkkkk..",
])
