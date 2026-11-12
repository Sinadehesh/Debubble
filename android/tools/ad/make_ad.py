#!/usr/bin/env python3
"""The DeBubble advert: twenty seconds, 1080x1920, for Reels, Shorts, TikTok and the like.

  0.0 s  Four tiny things a day: day one's room, messy and sealed. Four ticks, each one
         cleaning something, and then the first things arrive.
  4.6 s  Step outside your bubble: the room through all seven stages, furnishing as it goes.
  9.2 s  The app itself, filmed on an emulator (androidTest/AdReel.kt, the "Ad footage"
         workflow): a tiny thing done, today's step outside, training, a habit, the wardrobe.
 16.2 s  The end card: the open room, the name, and where to get it.

Cut on the beats of its own music (music.py). The room is drawn by roomfx.py, by the app's own
rules; the wallpaper, captions and phone are store.py's, so the advert matches the listing.

Run:  python3 make_ad.py <footage folder> <out.mp4>
      python3 make_ad.py <footage folder> <out folder> --stills 0.5 4 9.8 17
"""

import math
import pathlib
import subprocess
import sys
import tempfile

HERE = pathlib.Path(__file__).resolve().parent
PIXEL = HERE.parent / "pixel"
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(PIXEL))

from PIL import Image, ImageDraw, ImageFont   # noqa: E402
import roomfx as R                            # noqa: E402
import music                                  # noqa: E402
import store                                  # noqa: E402
from fx import FX                             # noqa: E402

ROOT = HERE.parents[2]
NUNITO = ROOT / "android" / "app" / "src" / "main" / "res" / "font"
ICON = ROOT / "store" / "icon-512.png"

W, H, FPS = 1080, 1920, 30
BEAT = music.BEAT
DURATION = music.BARS * music.BAR + music.TAIL

# The app's own colours (ui/theme/Theme.kt).
INK = {
    "primary": (0x33, 0x24, 0x3A), "secondary": (0x5A, 0x46, 0x60), "muted": (0x6B, 0x5A, 0x6E),
    "surface": (0xFF, 0xFD, 0xF5), "shadow": (0xD7, 0xBE, 0x95), "well": (0xF3, 0xDF, 0xBB),
    "faint": (0xD7, 0xBE, 0x95),
    "body": (0x9B, 0xD7, 0xD1), "body_wash": (0xDD, 0xF1, 0xEE),
    "room": (0xE8, 0xB4, 0x76), "room_wash": (0xF6, 0xE3, 0xC6),
    "heart": (0xF3, 0xB8, 0xC8), "heart_wash": (0xFB, 0xE4, 0xEB),
    "gold": (0xFF, 0xE0, 0x8A), "gold_wash": (0xFF, 0xF2, 0xC6),
}

Z = 6                                   # room pixels to screen pixels: 160x100 -> 960x600
ROOM_X, ROOM_Y = (W - 160 * Z) // 2, 570
CAPTION_Y = 168                         # the caption, the same height in every scene
# Reels, Shorts and TikTok lay their own buttons and text over the bottom fifth, so the
# captions are at the top, and the room scenes and the end card finish above about 1550.

# Today's four, in the order they are ticked, with what each one cleans.
FOUR = [("Body", "icon_body", "body"), ("Room", "icon_room", "room"),
        ("Heart", "icon_heart", "heart"), ("Outside", "icon_out", "gold")]

# What arrives, in order: the first three on day one, then a few more with every stage.
ARRIVALS = ["bed", "lamp", "plant", "rug", "photo",                    # day one
            "shelf", "water",                                          # one window
            "basket", "cat",                                           # door open
            "record", "desk",                                          # the street
            "fruit", "books", "clock",                                 # open ground
            "beanbag", "guitar", "pennant",                            # horizon
            "globe", "map", "postcards"]                               # no walls
STAGE_ITEMS = [5, 7, 9, 11, 14, 17, 20]


def beats(n):
    return n * BEAT


# The timeline, in beats (104 BPM: a beat is 0.577 s, a bar 2.31 s).
T_TICKS = [1.5, 2.5, 3.5, 4.5]          # each of today's four
T_ARRIVE = 5.5                          # the first things arrive, a quarter-beat apart
T_STAGES = 8                            # step outside your bubble
T_STAGE = [9, 10, 11, 12, 13, 14]       # window, door, street, open, horizon, no walls
T_APP = 16                              # the phone
T_END = 28                              # the end card

# The app, filmed: (caption, small line, beats shown, [(clip, from, to, beats)]). Each piece of
# a clip plays at whatever speed fits it into its beats, which is how a three-second hold fits
# in two; a piece with no times is the clip's last frame, held. The times are seconds into
# the footage AdReel films, where each clip opens on the screen before for about a second.
SHOTS = [
    ("Two minutes counts", "for your body, your room and your heart", 4,
     [("3-care", 3.25, 6.85, 2.25), ("3-care", 6.95, 8.4, 1.75)]),
    ("One brave step a day", "sized just right for you", 2, [("4-letter", 3.55, 5.12, 2)]),
    ("Train anywhere", "a move sized to what you did last time", 2, [("6-training", 3.6, 5.06, 2)]),
    ("Every day adds up", "no streaks to break", 2, [("7-habit", 3.05, 4.77, 2)]),
    ("Level up", "and dress your little you", 2, [("9-wardrobe", None, None, 2)]),
]

# Where the finger is while "I did it" is held, from taps.txt, in the care clip's seconds.
HOLD = ("3-care", 3.36, 6.76, 674, 1741)


# ----------------------------------------------------------------------------- drawing

def ease_out(p):
    p = min(1.0, max(0.0, p))
    return 1 - (1 - p) ** 3


def stepped(box, s):
    """The app's pixel corners: each corner cut in two steps of s pixels."""
    x0, y0, x1, y1 = box
    return [
        (x0, y0 + 2 * s), (x0 + s, y0 + 2 * s), (x0 + s, y0 + s), (x0 + 2 * s, y0 + s), (x0 + 2 * s, y0),
        (x1 - 2 * s, y0), (x1 - 2 * s, y0 + s), (x1 - s, y0 + s), (x1 - s, y0 + 2 * s), (x1, y0 + 2 * s),
        (x1, y1 - 2 * s), (x1 - s, y1 - 2 * s), (x1 - s, y1 - s), (x1 - 2 * s, y1 - s), (x1 - 2 * s, y1),
        (x0 + 2 * s, y1), (x0 + 2 * s, y1 - s), (x0 + s, y1 - s), (x0 + s, y1 - 2 * s), (x0, y1 - 2 * s),
    ]


def panel(img, box, fill, border=INK["primary"], width=5, step=5, lift=8, shadow=INK["shadow"]):
    """A card: its shadow below, a plum outline, stepped corners, as Kit.kt draws one."""
    d = ImageDraw.Draw(img)
    x0, y0, x1, y1 = box
    if lift:
        d.polygon(stepped((x0, y0 + lift, x1, y1 + lift), step), fill=shadow)
    d.polygon(stepped(box, step), fill=border)
    d.polygon(stepped((x0 + width, y0 + width, x1 - width, y1 - width), max(1, step - 1)), fill=fill)


_TEXT = {}


def pixel_text(words, size, key="k", shadow=None):
    """A line in Jersey 10, drawn by store.text so it is pixel-exact, on a clear layer."""
    k = (words, size, key, shadow)
    if k not in _TEXT:
        font = ImageFont.truetype(str(store.FONTS / "Jersey10-Regular.ttf"), size)
        _, _, r, b = ImageDraw.Draw(Image.new("L", (1, 1))).textbbox((0, 0), words, font=font)
        pad = size // 5
        layer = Image.new("RGBA", (r + 2 * pad, b + 2 * pad), (0, 0, 0, 0))
        store.text(layer, words, pad, pad, size, key, shadow=shadow)
        box = layer.getbbox() or (0, 0, 1, 1)
        _TEXT[k] = layer.crop((box[0], 0, box[2], layer.height))
    return _TEXT[k]


def round_text(words, size, colour, weight="extrabold"):
    k = ("round", words, size, colour, weight)
    if k not in _TEXT:
        font = ImageFont.truetype(str(NUNITO / f"nunito_{weight}.ttf"), size)
        _, _, r, b = ImageDraw.Draw(Image.new("L", (1, 1))).textbbox((0, 0), words, font=font)
        layer = Image.new("RGBA", (r + 8, b + 12), (0, 0, 0, 0))
        ImageDraw.Draw(layer).text((4, 4), words, font=font, fill=colour + (255,))
        box = layer.getbbox() or (0, 0, 1, 1)
        _TEXT[k] = layer.crop((box[0], 0, box[2], layer.height))
    return _TEXT[k]


def put(img, layer, x, y, alpha=1.0):
    if alpha <= 0:
        return
    if alpha < 1:
        layer = layer.copy()
        a = layer.getchannel("A").point(lambda v: int(v * alpha))
        layer.putalpha(a)
    img.alpha_composite(layer, (int(x), int(y)))


def centred(img, layer, y, alpha=1.0, dx=0):
    put(img, layer, (W - layer.width) // 2 + dx, y, alpha)


def caption(img, big, small, t0, t, y=CAPTION_Y):
    """The two-line caption, rising into place over a fifth of a second from t0."""
    p = ease_out((t - t0) / 0.22)
    rise = round((1 - p) * 26)
    centred(img, pixel_text(big, 100, "k", shadow="v"), y + rise, p)
    if small:
        centred(img, round_text(small, 44, INK["secondary"], "bold"), y + 118 + rise, p)


def glyph(img, rows, x, y, z, colour):
    d = ImageDraw.Draw(img)
    for r, row in enumerate(rows):
        for c, ch in enumerate(row):
            if ch == "#":
                d.rectangle((x + c * z, y + r * z, x + (c + 1) * z - 1, y + (r + 1) * z - 1), fill=colour)


CHECK = [".......#", "......##", "#....##.", "##..##..", ".####...", "..##....", "........"]


def sprite(name, z):
    k = ("sprite", name, z)
    if k not in _TEXT:
        im = R.scene.img(FX[name])
        _TEXT[k] = im.resize((im.width * z, im.height * z), Image.NEAREST)
    return _TEXT[k]


_WALL = store.wallpaper(W, H, 6).convert("RGBA")


def wall():
    return _WALL.copy()


# ----------------------------------------------------------------------------- the room

def room_state(t):
    """Everything about the room at time t, in the first two scenes and the end."""
    b = t / BEAT
    ticks = sum(1 for x in T_TICKS if b >= x)
    body = 0 if ticks >= 1 else 3
    room = 0 if ticks >= 2 else 3
    heart = 0 if ticks >= 3 else 3
    letter = ticks < 4
    stage_i = sum(1 for x in T_STAGE if b >= x)
    # Items: the day-one five a quarter-beat apart, then each stage's few on its beat.
    if b < T_STAGES:
        n = sum(1 for k in range(STAGE_ITEMS[0]) if b >= T_ARRIVE + k * 0.25)
        times = {ARRIVALS[k]: T_ARRIVE + k * 0.25 for k in range(n)}
    else:
        n = STAGE_ITEMS[stage_i]
        times = {ARRIVALS[k]: T_ARRIVE + k * 0.25 for k in range(STAGE_ITEMS[0])}
        for s in range(1, stage_i + 1):
            for j, k in enumerate(range(STAGE_ITEMS[s - 1], STAGE_ITEMS[s])):
                times[ARRIVALS[k]] = T_STAGE[s - 1] + 0.15 + j * 0.2
    owned = ARRIVALS[:n]
    drop = {}
    for iid in owned:
        age = (b - times[iid]) * BEAT                     # seconds since it arrived
        if age < 0.22:
            drop[iid] = round(14 * (1 - ease_out(age / 0.22)))
        elif age < 0.32:
            drop[iid] = -1 if age < 0.27 else 0           # a one-pixel bump as it lands
    last = max([x for x in T_TICKS if b >= x] + [T_ARRIVE if n else -9] + [x for x in T_STAGE if b >= x])
    burst = 0 <= (b - last) * BEAT < 0.7
    return {
        "stage": stage_i, "owned": owned, "drop": drop, "body": body, "room": room,
        "heart": heart, "letter": letter, "burst": burst, "ticks": ticks,
    }


def room_image(t, end=False):
    """The room at time t, at Z, with the stage dissolving in over a fifth of a second."""
    s = room_state(t) if not end else {
        "stage": 6, "owned": ARRIVALS, "drop": {}, "body": 0, "room": 0, "heart": 0,
        "letter": False, "burst": True, "ticks": 4,
    }
    frame = int(t / BEAT)                     # you bob on the beat
    stage = R.STAGES[s["stage"]]
    base = None
    if not end and s["stage"] > 0:
        since = (t / BEAT - T_STAGE[s["stage"] - 1]) * BEAT
        if since < 0.2:
            base = R.dissolve(R.shell_image(R.STAGES[s["stage"] - 1]), R.shell_image(stage), since / 0.2)
    im = R.compose(stage, R.spots(s["owned"]), s["body"], s["room"], s["heart"], frame=frame,
                   letter=s["letter"], drop=s["drop"], burst=s["burst"], base=base)
    return im.resize((160 * Z, 100 * Z), Image.NEAREST), s


def room_card(img, t, y=ROOM_Y, end=False, alpha=1.0):
    pic, s = room_image(t, end)
    card = Image.new("RGBA", (W, 100 * Z + 40), (0, 0, 0, 0))
    panel(card, (ROOM_X - 7, 7, ROOM_X + 160 * Z + 6, 100 * Z + 20), INK["surface"], width=7, step=7, lift=12)
    card.alpha_composite(pic, (ROOM_X, 14))
    put(img, card, 0, y - 14, alpha)
    return s


def four_chips(img, ticks, t, alpha=1.0):
    """Today's four under the room, each ticking on its beat."""
    y0, h, gap = ROOM_Y + 100 * Z + 74, 250, 18
    w = (160 * Z - 3 * gap) // 4
    layer = Image.new("RGBA", (W, h + 60), (0, 0, 0, 0))
    rings = []
    for i, (label, icon, accent) in enumerate(FOUR):
        x = ROOM_X + i * (w + gap)
        done = i < ticks
        since = (t / BEAT - T_TICKS[i]) * BEAT
        press = 6 if done and since < 0.12 else 0          # it sinks as it is pressed
        fill = INK[f"{accent}_wash"] if done else INK["surface"]
        panel(layer, (x, press, x + w, h - 8 + press), fill, width=5, step=5, lift=0 if press else 8)
        ic = sprite(icon, 5)
        layer.alpha_composite(ic, (x + (w - ic.width) // 2, 24 + press + (66 - ic.height) // 2))
        lab = round_text(label, 40, INK["primary"])
        layer.alpha_composite(lab, (x + (w - lab.width) // 2, 98 + press))
        cx, cy = x + w // 2, 190 + press
        box = (cx - 26, cy - 26, cx + 26, cy + 26)
        d = ImageDraw.Draw(layer)
        d.rectangle((box[0] - 5, box[1] - 5, box[2] + 5, box[3] + 5), fill=INK["primary"])
        d.rectangle(box, fill=INK[accent] if done else INK["surface"])
        if done:
            glyph(layer, CHECK, box[0] + 10, box[1] + 12, 4, INK["primary"])
        if 0 <= since < 0.35:
            rings.append((cx, cy, since / 0.35))
    for cx, cy, p in rings:
        r = round(34 + 46 * ease_out(p))
        ring = Image.new("RGBA", layer.size, (0, 0, 0, 0))
        dr = ImageDraw.Draw(ring)
        dr.rectangle((cx - r, cy - r, cx + r, cy + r), outline=INK["primary"] + (round(255 * (1 - p)),), width=6)
        layer.alpha_composite(ring)
    put(img, layer, 0, y0, alpha)


def stage_strip(img, stage_i, t, alpha=1.0):
    """The room's name, and seven squares for the seven stages, filling as it opens."""
    y0 = ROOM_Y + 100 * Z + 74
    layer = Image.new("RGBA", (W, 230), (0, 0, 0, 0))
    panel(layer, (ROOM_X, 0, ROOM_X + 160 * Z, 190), INK["surface"], width=5, step=5, lift=8)
    since = (t / BEAT - (T_STAGE[stage_i - 1] if stage_i else T_STAGES)) * BEAT
    p = ease_out(since / 0.2)
    name = pixel_text(R.STAGE_LABELS[stage_i], 76, "k")
    layer.alpha_composite(name, ((W - name.width) // 2, 14 + round((1 - p) * 10)))
    d = ImageDraw.Draw(layer)
    size, gap = 84, 22
    x = (W - (7 * size + 6 * gap)) // 2
    for i in range(7):
        on = i <= stage_i
        bx = x + i * (size + gap)
        d.rectangle((bx, 112, bx + size, 112 + 52), fill=INK["primary"] if on else INK["faint"])
        d.rectangle((bx + 5, 117, bx + size - 5, 112 + 47), fill=INK["gold"] if on else INK["well"])
    put(img, layer, 0, y0, alpha)


# ----------------------------------------------------------------------------- the app

class Clip:
    """A stretch of one filmed clip, played to fit [frames] frames of the advert.

    screenrecord only writes a frame when the screen changes, so the footage is read from its
    start at a steady 30 a second (a still screen repeats its frame), and the stretch is
    picked out of that by frame number. Reading is in order, one frame held at a time."""

    def __init__(self, path, start, end, frames, width):
        self.n = frames
        self.start, self.end = start, end
        self.speed = 0 if end is None else (end - start) * FPS / max(1, frames - 1)
        self.width = width
        self.height = round(1920 * width / 1080)
        vf = f"fps={FPS},scale={width}:{self.height}:flags=lanczos"
        self.proc = subprocess.Popen(
            ["ffmpeg", "-loglevel", "error", "-i", str(path), "-vf", vf,
             "-f", "rawvideo", "-pix_fmt", "rgb24", "-"],
            stdout=subprocess.PIPE)
        self.last = None
        self.read = 0

    def source(self, k):
        """Seconds into the clip that frame k of this stretch shows."""
        if self.end is None:
            return None
        return self.start + min(k, self.n - 1) * self.speed / FPS

    def frame(self, k):
        target = 10 ** 9 if self.end is None else int(round(self.source(k) * FPS))
        size = self.width * self.height * 3
        while self.read <= target:
            buf = self.proc.stdout.read(size)
            if len(buf) < size:
                break
            self.last = Image.frombytes("RGB", (self.width, self.height), buf)
            self.read += 1
        return self.last


class Shot:
    """One caption's worth of the app: its stretches of footage, one after another."""

    def __init__(self, footage, pieces, width):
        self.pieces = []
        for name, start, end, length in pieces:
            frames = int(round(beats(length) * FPS))
            self.pieces.append((name, Clip(pathlib.Path(footage) / f"{name}.mp4", start, end, frames + 1, width), frames))

    def at(self, k):
        """(clip name, seconds into it or None, frame) for frame k of the shot."""
        for i, (name, clip, frames) in enumerate(self.pieces):
            if k < frames or i == len(self.pieces) - 1:
                return name, clip.source(k), clip.frame(k)
            k -= frames


# The whole phone, not store.framed's phone running off the bottom: the buttons that are
# pressed in the footage are at the bottom of the screen.
PHONE_W, PHONE_Y, RADIUS, EDGE = 780, 404, 42, 12
PHONE_H = round(1920 * PHONE_W / 1080)
SCALE = PHONE_W / 1080
STATUS = round(store.STATUS_BAR * SCALE)


def phone(img, shot, y):
    """store.framed's phone: the screen, rounded, in a plum edge, on a hard shadow."""
    if shot is None:
        return
    x = (W - PHONE_W) // 2
    y = int(y)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((x - EDGE, y - EDGE + 14, x + PHONE_W + EDGE - 1, y + PHONE_H + EDGE + 13),
                        radius=RADIUS + EDGE, fill=INK["shadow"] + (255,))
    d.rounded_rectangle((x - EDGE, y - EDGE, x + PHONE_W + EDGE - 1, y + PHONE_H + EDGE - 1),
                        radius=RADIUS + EDGE, fill=INK["primary"] + (255,))
    screen = shot.copy()
    fill = screen.getpixel((screen.width // 2, STATUS // 2))
    ImageDraw.Draw(screen).rectangle((0, 0, screen.width, STATUS), fill=fill)
    mask = Image.new("L", screen.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, screen.width - 1, screen.height - 1), radius=RADIUS, fill=255)
    img.paste(screen, (x, y), mask)


def finger(img, sx, sy, held):
    """Where a finger is pressing, in screen pixels of the footage: a soft pixel disc that
    breathes a little while it is held."""
    x = (W - PHONE_W) // 2 + sx * SCALE
    y = PHONE_Y + sy * SCALE
    r = 26 + 3 * math.sin(held * 9)
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse((x - r - 5, y - r - 5, x + r + 5, y + r + 5), fill=INK["primary"] + (120,))
    d.ellipse((x - r, y - r, x + r, y + r), fill=(255, 255, 255, 110))
    img.alpha_composite(layer)


# ----------------------------------------------------------------------------- the end

def end_card(img, t):
    since = t - beats(T_END)
    p = lambda delay: ease_out((since - delay) / 0.3)     # noqa: E731
    icon = Image.open(ICON).convert("RGBA").resize((176, 176), Image.NEAREST)
    mask = Image.new("L", icon.size, 0)
    ImageDraw.Draw(mask).polygon(stepped((0, 0, 175, 175), 12), fill=255)
    icon.putalpha(mask)
    framed = Image.new("RGBA", (196, 204), (0, 0, 0, 0))
    ImageDraw.Draw(framed).polygon(stepped((0, 8, 195, 203), 13), fill=INK["shadow"])
    ImageDraw.Draw(framed).polygon(stepped((0, 0, 195, 195), 13), fill=INK["primary"])
    framed.alpha_composite(icon, (10, 10))
    centred(img, framed, 150 + round((1 - p(0)) * 30), p(0))
    centred(img, pixel_text("DeBubble", 150, "k", shadow="v"), 356 + round((1 - p(0.06)) * 30), p(0.06))
    centred(img, round_text("Look after yourself. Watch your world grow.", 44, INK["secondary"], "bold"),
            526, p(0.12))
    room_card(img, t, y=656, end=True, alpha=p(0.18))
    cta = Image.new("RGBA", (W, 170), (0, 0, 0, 0))
    words = pixel_text("Free on Google Play", 84, "k")
    bw = words.width + 120
    panel(cta, ((W - bw) // 2, 0, (W + bw) // 2, 120), INK["gold"], width=6, step=6, lift=10, shadow=INK["primary"])
    cta.alpha_composite(words, ((W - words.width) // 2, 10))
    put(img, cta, 0, 1350 + round((1 - p(0.3)) * 40), p(0.3))
    centred(img, round_text("No ads. No account. Nothing leaves your phone.", 38, INK["muted"], "bold"),
            1526, p(0.42))


# ----------------------------------------------------------------------------- frames

def frame_at(t, clips):
    img = wall()
    b = t / BEAT
    if b < T_APP:
        out = 1 - ease_out((t - beats(T_APP) + 0.25) / 0.25) if b > T_APP - 0.5 else 1.0
        if b < T_STAGES:
            # Already in place on the first frame, which is what a feed shows before it plays.
            caption(img, "Four tiny things a day", "and your room comes alive", -1.0, t)
        else:
            caption(img, "Step outside your bubble", "and watch your world open up", beats(T_STAGES), t)
        s = room_card(img, t, alpha=out)
        if b < T_STAGES:
            fade = 1 - ease_out((t - beats(T_STAGES) + 0.2) / 0.2) if b > T_STAGES - 0.4 else 1.0
            four_chips(img, s["ticks"], t, alpha=fade * out)
        else:
            stage_strip(img, s["stage"], t, alpha=ease_out((t - beats(T_STAGES)) / 0.25) * out)
        if b > T_APP - 0.5:
            slide = ease_out((t - beats(T_APP) + 0.3) / 0.3)
            phone(img, clips[0].at(0)[2] if clips else None, PHONE_Y + (1 - slide) * (H - PHONE_Y))
        return img
    if b < T_END:
        at = T_APP
        for i, (big, small, length, _) in enumerate(SHOTS):
            if b < at + length or i == len(SHOTS) - 1:
                caption(img, big, small, beats(at), t)
                k = int(round((t - beats(at)) * FPS))
                if clips:
                    name, sec, shot = clips[i].at(k)
                    phone(img, shot, PHONE_Y)
                    hold_clip, h0, h1, hx, hy = HOLD
                    if name == hold_clip and sec is not None and h0 <= sec <= h1:
                        finger(img, hx, hy, sec - h0)
                break
            at += length
        return img
    end_card(img, t)
    return img


def open_clips(footage):
    return [Shot(footage, pieces, PHONE_W) for _, _, _, pieces in SHOTS]


# ----------------------------------------------------------------------------- sound

def sounds():
    """The music, with the picture's own little sounds on top, each where its thing happens:
    a blip for every tick, a pop as each thing lands, a chime as the room opens, a soft rush
    as the phone comes up, and a sparkle on the end card."""
    import numpy as np
    mix = music.render()
    fx = np.zeros_like(mix)

    def at(sec, sig):
        music.place(fx, sec, sig)

    for b in T_TICKS:
        at(beats(b), music.note(music.midi(84), 0.035, "pulse", 0.20, duty=0.5, decay=0.03, release=0.02))
        at(beats(b) + 0.045, music.note(music.midi(91), 0.06, "pulse", 0.16, duty=0.5, decay=0.05, release=0.04))
    landings = [T_ARRIVE + k * 0.25 for k in range(STAGE_ITEMS[0])]
    for st in range(1, len(STAGE_ITEMS)):
        landings += [T_STAGE[st - 1] + 0.15 + j * 0.2 for j in range(STAGE_ITEMS[st] - STAGE_ITEMS[st - 1])]
    for b in landings:
        n = int(0.06 * music.RATE)
        tt = np.arange(n) / music.RATE
        f = 900 * np.exp(-tt * 30) + 260
        pop = 0.16 * np.sin(2 * np.pi * np.cumsum(f) / music.RATE) * np.exp(-tt * 45)
        at(beats(b) + 0.22, pop)
    chord = [72, 76, 79, 84]
    for i, b in enumerate(T_STAGE):
        for k, m in enumerate(chord):
            at(beats(b) + k * 0.035, music.note(music.midi(m + (i % 2) * 2), 0.09, "pulse", 0.08,
                                                duty=0.125, decay=0.08, release=0.12))
    n = int(0.4 * music.RATE)
    tt = np.arange(n) / music.RATE
    rush = music.lowpass(np.random.default_rng(5).uniform(-1, 1, n), 1800) * np.sin(np.pi * tt / 0.4) * 0.35
    at(beats(T_APP) - 0.32, rush)
    for k, m in enumerate([84, 88, 91, 96]):
        at(beats(T_END) + 0.05 + k * 0.06, music.note(music.midi(m), 0.12, "pulse", 0.07, duty=0.125,
                                                      decay=0.1, release=0.25))
    out = mix + fx
    return 0.89 * out / max(1e-9, float(np.max(np.abs(out))))


def render(footage, out):
    total = int(round(DURATION * FPS))
    with tempfile.TemporaryDirectory() as tmp:
        wav = pathlib.Path(tmp) / "music.wav"
        music.write(wav, sounds())
        enc = subprocess.Popen(
            ["ffmpeg", "-loglevel", "error", "-y",
             "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-",
             "-i", str(wav),
             "-c:v", "libx264", "-preset", "slow", "-crf", "17", "-pix_fmt", "yuv420p",
             "-profile:v", "high", "-level", "4.1", "-movflags", "+faststart",
             "-af", "loudnorm=I=-14:TP=-1.5:LRA=11", "-c:a", "aac", "-b:a", "192k", "-ar", "48000",
             "-shortest", str(out)],
            stdin=subprocess.PIPE)
        clips = open_clips(footage)
        for i in range(total):
            enc.stdin.write(frame_at(i / FPS, clips).convert("RGB").tobytes())
            if i % 60 == 0:
                print(f"  {i / FPS:5.1f}s", flush=True)
        enc.stdin.close()
        enc.wait()
    print("wrote", out)


def stills(footage, folder, times):
    folder = pathlib.Path(folder)
    folder.mkdir(parents=True, exist_ok=True)
    for t in times:
        clips = open_clips(footage)
        frame_at(t, clips).convert("RGB").save(folder / f"t{t:05.2f}.png")
    print("wrote", len(times), "stills to", folder)


if __name__ == "__main__":
    if len(sys.argv) > 3 and sys.argv[3] == "--stills":
        stills(sys.argv[1], sys.argv[2], [float(x) for x in sys.argv[4:]])
    else:
        render(sys.argv[1], sys.argv[2])
