#!/usr/bin/env python3
"""The launcher icon and the Google Play listing graphics, drawn from the room's own sprites.

The old icon was three glowing rings from before the room existed, so the icon on the home
screen promised a different app from the one it opened. This one is the app's opening shot:
you, with your heart at your shoulder, standing in your room under the window.

The icon is a 54x54 grid, two dp to a pixel on the adaptive icon's 108dp canvas. Two dp is
the smallest size that stays a whole number of device pixels at every density (2, 3, 4, 6, 8),
so no pixel lands on a half and blurs. A launcher shows the middle 72dp, which is the middle
36 pixels, and a round mask keeps a circle of that; everything that matters sits inside it.

  res/drawable/ic_launcher_background.xml   the room
  res/drawable/ic_launcher_foreground.xml   you and your heart
  res/drawable/ic_launcher_monochrome.xml   the same two as one colour, for themed icons
  store/icon-512.png                        the Play listing icon (Play adds its own corners)
  store/feature-graphic.png                 1024x500, the banner at the top of the listing
  store/screenshots/*.png                   the real app's screens, captioned (see below)

Run:  python3 android/tools/pixel/store.py
      python3 android/tools/pixel/store.py screenshots <raw folder>

The raw screenshots come from the "Store screenshots" workflow, which photographs the real
app on an emulator (androidTest/StoreShots.kt); this only adds the captions.
"""

import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from PIL import Image, ImageDraw, ImageFont   # noqa: E402
from palette import rgb                    # noqa: E402
from fx import FX                          # noqa: E402
import room                                # noqa: E402
import scene                               # noqa: E402

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[2]
FONTS = HERE / "fonts"          # Jersey 10, SIL Open Font License (fonts/OFL-Jersey10.txt)
RES = ROOT / "android" / "app" / "src" / "main" / "res" / "drawable"
STORE = ROOT / "store"

N = 54            # icon grid
SHOWN = 36        # the middle a launcher shows (72dp of 108dp)

# Where things stand, in grid pixels. The pair is centred on the canvas as a group.
FLOOR = 36        # first floor row
ME_X, ME_B = 11, 39
BUDDY_X, BUDDY_B = 29, 28


class Grid:
    """A canvas of palette keys, so the icon is drawn in exactly the room's colours."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = [["." for _ in range(w)] for _ in range(h)]

    def set(self, x, y, k):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = k

    def rect(self, x, y, w, h, k):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.set(xx, yy, k)

    def frame(self, x, y, w, h, k):
        self.rect(x, y, w, 1, k)
        self.rect(x, y + h - 1, w, 1, k)
        self.rect(x, y, 1, h, k)
        self.rect(x + w - 1, y, 1, h, k)

    def sprite(self, spec, x, baseline):
        top = baseline - spec["h"]
        for y, row in enumerate(spec["rows"]):
            for xx, ch in enumerate(row):
                if ch != ".":
                    self.set(x + xx, top + y, ch)

    def image(self):
        im = Image.new("RGBA", (self.w, self.h), (0, 0, 0, 0))
        p = im.load()
        for y in range(self.h):
            for x in range(self.w):
                c = rgb(self.px[y][x])
                if c:
                    p[x, y] = c + (255,)
        return im


def background():
    """The room, as the app draws it: papered wall, the window, a teal wainscot, honey floor."""
    g = Grid(N, N)
    rail = FLOOR - 9
    for y in range(rail):
        g.rect(0, y, N, 1, "w" if y < 14 else ("W" if y < rail - 3 else "v"))
    for y in range(3, rail - 2, 6):                       # the wallpaper's little diamonds
        for x in range((y // 6) % 2 * 4 + 1, N, 8):
            g.set(x, y, "W")
            g.set(x - 1, y + 1, "W"); g.set(x, y + 1, "W"); g.set(x + 1, y + 1, "W")
            g.set(x, y + 2, "W")

    # The window, high and a little to the right, so the sky sits behind your heart.
    x, y, w, h = 21, 11, 20, 13
    g.frame(x - 2, y - 2, w + 4, h + 4, "k")
    g.rect(x - 1, y - 1, w + 2, h + 2, "i")
    for yy in range(y, y + h):
        g.rect(x, yy, w, 1, "s" if yy < y + h // 2 else "S")
    g.rect(x + 3, y + 2, 4, 1, "i"); g.rect(x + 2, y + 3, 6, 1, "i")      # a cloud
    g.rect(x + 13, y + 8, 3, 1, "i"); g.rect(x + 12, y + 9, 5, 1, "i")
    g.rect(x + w // 2 - 1, y, 2, h, "i")                                 # mullion
    g.rect(x, y + h // 2, w, 1, "i")
    g.rect(x - 3, y + h + 2, w + 6, 1, "i")                              # sill
    g.rect(x - 3, y + h + 3, w + 6, 1, "K")

    g.rect(0, rail, N, 1, "i")                                           # chair rail
    g.rect(0, rail + 1, N, 1, "K")
    g.rect(0, rail + 2, N, FLOOR - rail - 4, "c")                        # wainscot
    for px in range(-3, N, 13):
        g.frame(px, rail + 3, 11, FLOOR - rail - 6, "C")
    g.rect(0, FLOOR - 2, N, 1, "i")                                      # skirting
    g.rect(0, FLOOR - 1, N, 1, "k")

    for yy in range(FLOOR, N):                                           # boards
        t = (yy - FLOOR) / (N - FLOOR)
        g.rect(0, yy, N, 1, "d" if t < 0.12 else ("F" if t < 0.55 else "f"))
    sy, gap = FLOOR + 2, 2.0
    while sy < N:
        g.rect(0, int(sy), N, 1, "d")
        sy += gap
        gap *= 1.5
    # The pool of light from the window, falling on the boards in front of you.
    for yy in range(FLOOR + 1, FLOOR + 5):
        inset = (yy - FLOOR) * 2
        for xx in range(x - 2 + inset, x + w + 2 - inset):
            if g.px[yy][xx] == "F":
                g.set(xx, yy, "f")
    return g


def foreground():
    """You, freshly washed (the sparkle), with your heart at your shoulder, smiling."""
    g = Grid(N, N)
    # A soft shadow under your feet, so you stand on the floor rather than float over it.
    g.rect(ME_X + 4, ME_B - 1, 12, 1, "d")
    g.rect(ME_X + 6, ME_B, 8, 1, "d")
    g.sprite(FX["me0"], ME_X, ME_B)
    g.sprite(FX["buddy0"], BUDDY_X, BUDDY_B)
    g.sprite(FX["sparkle"], ME_X, ME_B - 17)
    return g


def monochrome(fg):
    """Themed icons tint one shape. Outlines become gaps, so the shape keeps a face."""
    g = Grid(N, N)
    for y in range(N):
        for x in range(N):
            k = fg.px[y][x]
            if k not in (".", "k", "d", "y", "i"):
                g.set(x, y, "k")
    return g


# ---------------------------------------------------------------------------- vector export

def vector(g, comment):
    """One path per colour, each a run of same-coloured pixels along a row."""
    by_key = {}
    for y in range(g.h):
        x = 0
        while x < g.w:
            k = g.px[y][x]
            if k == ".":
                x += 1
                continue
            start = x
            while x < g.w and g.px[y][x] == k:
                x += 1
            by_key.setdefault(k, []).append(f"M{start},{y}h{x - start}v1h-{x - start}z")
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- {comment} Generated by tools/pixel/store.py; edit that, not this. -->",
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="108dp"',
        '    android:height="108dp"',
        f'    android:viewportWidth="{g.w}"',
        f'    android:viewportHeight="{g.h}">',
    ]
    for k in sorted(by_key):
        r, gg, b = rgb(k)
        lines.append(f'    <path android:fillColor="#FF{r:02X}{gg:02X}{b:02X}"')
        lines.append(f'        android:pathData="{"".join(by_key[k])}" />')
    lines.append("</vector>")
    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------------------- store icon

def icon_png(size=512):
    """The shown 36x36 middle at a whole-number zoom, cropped to exactly size x size."""
    full = background().image()
    full.alpha_composite(foreground().image())
    z = -(-size // SHOWN)                      # 15 for 512: 36*15 = 540, crop 14 each side
    big = full.resize((N * z, N * z), Image.NEAREST)
    c = N * z // 2
    return big.crop((c - size // 2, c - size // 2, c + size // 2, c + size // 2))


# ---------------------------------------------------------------------------- feature graphic

FEATURE_W, FEATURE_H, FEATURE_Z = 205, 100, 5      # 1025x500 at 5x, cropped to 1024


def text(base, words, x, y, size, key, shadow=None):
    """Jersey 10 is drawn on a ten-unit pixel grid: at size 50 each of its pixels is a 5x5
    block, one of the room's pixels in the banner, and at 100 it is two. Drawn without
    smoothing at the final size, so no letter loses a pixel to rounding."""
    font = ImageFont.truetype(str(FONTS / "Jersey10-Regular.ttf"), size)
    mask = Image.new("L", base.size, 0)
    d = ImageDraw.Draw(mask)
    d.fontmode = "1"
    d.text((x, y), words, font=font, fill=255)
    if shadow:
        step = size // 10
        base.paste(Image.new("RGBA", base.size, rgb(shadow) + (255,)), (step, step), mask)
    base.paste(Image.new("RGBA", base.size, rgb(key) + (255,)), (0, 0), mask)
    return d.textbbox((x, y), words, font=font)


def feature_graphic():
    """The banner: a wider view of a lived-in room, with the name on the wall."""
    saved = room.W
    room.W = FEATURE_W                      # the same room, drawn wider
    try:
        placed = {
            "bed": (2, 76), "lamp": (40, 70), "plant": (56, 72), "rug": (52, 92),
            "basket": (28, 86), "photo": (26, 28), "shelf": (6, 34),
            "slippers": (46, 98), "record": (92, 72),
            "desk": (118, 74), "beanbag": (150, 90), "guitar": (186, 84), "books": (132, 97),
            "water": (116, 95),
        }
        # The room's resting sparkles, minus any that would land on the words.
        sparkles = scene.ROOM_SPARKLES
        scene.ROOM_SPARKLES = [(x, y) for x, y in sparkles if not (x > 100 and y < 42)]
        im = scene.compose("window", placed, 0, 0, 0, me=(70, 96), frame=0, letter=True)
    finally:
        room.W = saved
        scene.ROOM_SPARKLES = sparkles
    # A second sparkle or two in the air, because a finished room glitters a little.
    for x, y in ((100, 60), (172, 64)):
        scene.put(im, FX["sparkle"], x, y)

    big = im.resize((FEATURE_W * FEATURE_Z, FEATURE_H * FEATURE_Z), Image.NEAREST)
    big = big.crop((0, 0, 1024, 500))
    text(big, "DeBubble", 545, 0, 120, "k", shadow="v")
    text(big, "Look after yourself.", 550, 115, 50, "K")
    text(big, "Watch your world grow.", 550, 155, 50, "K")
    return big


# ---------------------------------------------------------------------------- screenshots

SHOT_W, SHOT_H = 1080, 1920          # Play's phone shape: no more than twice as tall as wide

CAPTIONS = {
    "1-home": ("Four tiny things a day", "and your room comes alive"),
    "2-remind": ("Not now? Pick a time.", "A nudge, only if you want one"),
    "3-letter": ("One brave step a day", "sized just right for you"),
    "4-outside": ("Three roads out", "of your bubble"),
    "5-journey": ("Pick a journey.", "Walk it one stop at a time."),
    "6-wardrobe": ("Level up,", "and dress your little you"),
    "7-journal": ("Keep a postcard", "from every step"),
}


def wallpaper(w, h, z):
    """The room's papered wall at zoom z: cream, with its little diamonds."""
    g = Grid(-(-w // z), -(-h // z))
    g.rect(0, 0, g.w, g.h, "w")
    for y in range(3, g.h, 6):
        for x in range((y // 6) % 2 * 4 + 1, g.w, 8):
            g.set(x, y, "W")
            g.set(x - 1, y + 1, "W"); g.set(x, y + 1, "W"); g.set(x + 1, y + 1, "W")
            g.set(x, y + 2, "W")
    return g.image().resize((g.w * z, g.h * z), Image.NEAREST).crop((0, 0, w, h))


def framed(shot, caption):
    """A real screenshot under a two-line caption, on the room's wallpaper."""
    out = wallpaper(SHOT_W, SHOT_H, 6)
    top, bottom = caption
    for words, y, size, key in ((top, 70, 100, "k"), (bottom, 175, 70, "K")):
        font = ImageFont.truetype(str(FONTS / "Jersey10-Regular.ttf"), size)
        width = ImageDraw.Draw(out).textbbox((0, 0), words, font=font)[2]
        text(out, words, (SHOT_W - width) // 2, y, size, key, shadow="v" if key == "k" else None)

    # The phone: the screenshot scaled to fit, with rounded corners and a plum outline,
    # running off the bottom edge so it reads as a phone held up rather than a postcard.
    w = 920
    h = round(shot.height * w / shot.width)
    phone = shot.convert("RGBA").resize((w, h), Image.LANCZOS)
    x, y, r, edge = (SHOT_W - w) // 2, 300, 48, 12
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, h + r), radius=r, fill=255)
    ImageDraw.Draw(out).rounded_rectangle(
        (x - edge, y - edge, x + w + edge - 1, SHOT_H + r), radius=r + edge, fill=rgb("k") + (255,))
    out.paste(phone, (x, y), mask)
    return out.convert("RGB")


def screenshots(raw_dir):
    """store/screenshots/<name>.png for every raw screenshot that has a caption."""
    dest = STORE / "screenshots"
    dest.mkdir(parents=True, exist_ok=True)
    for name, caption in CAPTIONS.items():
        src = pathlib.Path(raw_dir) / f"{name}.png"
        if src.exists():
            framed(Image.open(src), caption).save(dest / f"{name}.png")
            print("wrote", dest / f"{name}.png")


def main():
    bg, fg = background(), foreground()
    RES.mkdir(parents=True, exist_ok=True)
    (RES / "ic_launcher_background.xml").write_text(vector(bg, "The room behind you."))
    (RES / "ic_launcher_foreground.xml").write_text(vector(fg, "You, and your heart."))
    (RES / "ic_launcher_monochrome.xml").write_text(
        vector(monochrome(fg), "You and your heart, as one shape for themed icons."))
    STORE.mkdir(parents=True, exist_ok=True)
    icon_png().convert("RGB").save(STORE / "icon-512.png")
    feature_graphic().convert("RGB").save(STORE / "feature-graphic.png")
    print("wrote the launcher icon layers,", STORE / "icon-512.png", "and", STORE / "feature-graphic.png")


if __name__ == "__main__":
    if len(sys.argv) > 2 and sys.argv[1] == "screenshots":
        screenshots(sys.argv[2])       # store.py screenshots <folder of raw emulator shots>
    else:
        main()
