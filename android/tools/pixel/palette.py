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
  - Limited count. 29 colours total. A big palette at 16x16 just reads as mush.
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

    # Weather indoors: the Heart cloud. Lilac-grey rather than grey, so even a storm cloud
    # sits in the warm family and reads as a mood rather than a warning.
    "q": ("#E4DDF0", "cloud light"),
    "Q": ("#B3A8C9", "cloud"),
    "x": ("#82769C", "cloud dark"),
}

ORDER = list(PALETTE.keys())
INDEX = {k: i for i, k in enumerate(ORDER)}


def rgb(key):
    hexval = PALETTE[key][0]
    if hexval is None:
        return None
    return tuple(int(hexval[i:i + 2], 16) for i in (1, 3, 5))
