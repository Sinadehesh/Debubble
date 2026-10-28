"""The room's palette.

Warm and bright on purpose. The previous room was navy, cinematic and — the word used about
it was doom and gloom — correct about that. This one is a sunny flat in the late afternoon.

Pixel art rules this palette follows, because breaking them is what makes pixel art look
generated rather than drawn:

  - No pure black. The darkest value is a deep plum, so outlines sit warm against the wood
    and the cream rather than punching holes in them.
  - Every hue family is a ramp of three (light / mid / dark) with a hue shift along it —
    shadows go cooler and more saturated, highlights go warmer. Three values is enough to
    read as volume at this size and few enough to stay crisp.
  - Limited count, under forty. A big palette at 16x16 just reads as mush.
  - One light direction for everything: upper left.
"""

# key -> (hex, human name). '.' is transparent and never drawn.
PALETTE = {
    ".": (None, "transparent"),

    # Outlines. Warm, never black.
    "k": ("#33243A", "outline"),
    "K": ("#5A4660", "outline soft"),

    # Walls — warm cream.
    "w": ("#FFF3DC", "wall light"),
    "W": ("#F3DFBB", "wall"),
    "v": ("#D7BE95", "wall shadow"),

    # Floor — honey wood.
    "f": ("#E8B476", "floor light"),
    "F": ("#C88B4E", "floor"),
    "d": ("#9C6436", "floor dark"),

    # Sky through the window.
    "s": ("#CFEEFF", "sky light"),
    "S": ("#86CCF2", "sky"),
    "b": ("#4FA3D9", "sky deep"),

    # Foliage.
    "g": ("#B4E86A", "leaf light"),
    "G": ("#63C04C", "leaf"),
    "h": ("#2F7F3E", "leaf dark"),

    # Terracotta — pots, rugs, brick.
    "t": ("#E08A5F", "clay light"),
    "T": ("#C2603A", "clay"),
    "r": ("#8E3F2A", "clay dark"),

    # Warm light — lamps, windows at dusk, brass.
    "y": ("#FFE08A", "glow light"),
    "Y": ("#FFBF4D", "glow"),
    "o": ("#E08A2B", "glow dark"),

    # Cool accent — fabric, ceramics, denim.
    "c": ("#9BD7D1", "teal light"),
    "C": ("#4FA8A4", "teal"),
    "n": ("#2E6F75", "teal dark"),

    # Soft accent — cushions, blossom, a cat's nose.
    "p": ("#F3B8C8", "blush light"),
    "P": ("#D97B9B", "blush"),

    # Near-white, used sparingly for specular pops and paper.
    "i": ("#FFFDF5", "paper"),

    # Steam and dust. Lilac-grey rather than grey, so even the dust sits in the warm family.
    "q": ("#E4DDF0", "haze light"),
    "Q": ("#B3A8C9", "haze"),

    # You. Skin, hair and top are each a ramp of three that the app swaps for the ramps in
    # LOOKS below, so these values are only the first look and are never used by anything
    # else — a swap can then be done by exact colour without touching the room.
    "a": ("#FFE1C7", "skin light"),
    "A": ("#F2BE98", "skin"),
    "u": ("#D18E6B", "skin shadow"),
    "e": ("#FF9DB0", "blush"),
    "m": ("#8A5A44", "hair light"),
    "M": ("#6A3F33", "hair"),
    "N": ("#46282A", "hair dark"),
    "l": ("#8FD3F0", "top light"),
    "L": ("#56A9D6", "top"),
    "j": ("#3A78A8", "top dark"),
}

# The ramps a character can be drawn in, light to dark. Entry 0 of each is what the sprite
# is drawn in (it must equal the palette keys above); the app swaps by exact colour, inside
# the character's own frames only. Skin carries its own blush, because one pink does not sit
# on every skin. Chosen by the user when they move in, and changeable any time: people
# persist longer with a character they made to look like them (Birk, Atkins, Bowey &
# Mandryk, 2016, CHI).
LOOK_KEYS = {"skin": "aAue", "hair": "mMN", "top": "lLj"}
LOOKS = {
    "skin": [
        ("#FFE1C7", "#F2BE98", "#D18E6B", "#FF9DB0"),
        ("#F6CDA5", "#E2A87C", "#B97850", "#F59A9A"),
        ("#D9A06E", "#BE8352", "#93603A", "#E8897C"),
        ("#A86A45", "#8C5435", "#683B24", "#CF7462"),
        ("#73462F", "#5C3624", "#3F2418", "#B45E50"),
    ],
    "hair": [
        ("#8A5A44", "#6A3F33", "#46282A"),
        ("#5A465E", "#3E2F45", "#2A1E30"),
        ("#E8914F", "#C2652F", "#8E4224"),
        ("#FFE08A", "#EBB955", "#B88330"),
        ("#F7A8C8", "#DB7DA6", "#A85580"),
        ("#8FDCD0", "#4FB3A8", "#2E7F7A"),
    ],
    "top": [
        ("#8FD3F0", "#56A9D6", "#3A78A8"),
        ("#A6E89A", "#5CC46B", "#34894A"),
        ("#FFB4A0", "#F07A68", "#B9504A"),
        ("#CDA8F0", "#A071D6", "#6E4A9E"),
        ("#FFE38A", "#FFC24D", "#D08A2B"),
        ("#E6E9EF", "#B3BCCB", "#7D889B"),
    ],
}

ORDER = list(PALETTE.keys())
INDEX = {k: i for i, k in enumerate(ORDER)}


def rgb(key):
    hexval = PALETTE[key][0]
    if hexval is None:
        return None
    return tuple(int(hexval[i:i + 2], 16) for i in (1, 3, 5))
