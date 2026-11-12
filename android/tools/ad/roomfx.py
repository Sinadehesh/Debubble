"""The room for the advert, drawn by the same rules as the app, with three things added.

scene.compose in tools/pixel is the reference for draw order, and this follows it exactly.
On top of it: the character in the look the store screenshots use (skin 1, hair 2, top 1,
the flower clip, as in androidTest/Camera.kt), things arriving by dropping into place, and
the celebration burst the app shows after something is cleaned (Home.BURST in Home.kt).
"""

import pathlib
import random
import sys

PIXEL = pathlib.Path(__file__).resolve().parents[1] / "pixel"
sys.path.insert(0, str(PIXEL))

from PIL import Image                     # noqa: E402
from palette import LOOKS, LOOK_KEYS      # noqa: E402
from room import shell, to_image, W, H    # noqa: E402
from sprites import SPRITES               # noqa: E402
from fx import (FX, ROOM_MESS, ROOM_SPARKLES, ME_SPOT, ME_H,  # noqa: E402
                BUDDY_DY, LETTER_DY, BANG_GAP)
import scene                              # noqa: E402

LOOK = {"skin": 1, "hair": 2, "top": 1}
HAT = "hat_flower"

# Home.BURST: where the celebration twinkles, as (x, baseline) in room pixels.
BURST = [(14, 18), (40, 30), (66, 12), (92, 26), (118, 16), (146, 30), (24, 50), (58, 46),
         (104, 52), (136, 48), (30, 76), (80, 70), (128, 74), (150, 88)]

STAGES = ["sealed", "window", "door", "street", "open", "horizon", "unwalled"]
STAGE_LABELS = ["Sealed room", "One window", "Door open", "The street", "Open ground",
                "Horizon", "No walls"]


def _hex(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def _swaps():
    out = {}
    for part, keys in LOOK_KEYS.items():
        for base, chosen in zip(LOOKS[part][0], LOOKS[part][LOOK[part]]):
            out[_hex(base)] = _hex(chosen)
    return out


_SWAPS = _swaps()
_ME = {}


def me_image(level):
    """You, at a body level, recoloured into the look by exact colour, as the app does."""
    if level not in _ME:
        im = scene.img(FX[f"me{level}"])
        px = im.load()
        for y in range(im.height):
            for x in range(im.width):
                r, g, b, a = px[x, y]
                if a and (r, g, b) in _SWAPS:
                    px[x, y] = _SWAPS[(r, g, b)] + (a,)
        _ME[level] = im
    return _ME[level]


_SHELLS = {}


def shell_image(stage):
    if stage not in _SHELLS:
        _SHELLS[stage] = to_image(shell(stage))
    return _SHELLS[stage].copy()


def compose(stage, placed, body, room, heart, frame=0, letter=False, drop=None, burst=False,
            base=None):
    """placed: {id: (x, baseline)}. drop: {id: pixels above its spot}, for things arriving.
    base: a shell image to draw on instead of the stage's own, for a stage changing."""
    drop = drop or {}
    im = base.copy() if base is not None else shell_image(stage)
    walls = stage != "unwalled"

    def lift(iid):
        return drop.get(iid, 0)

    for iid, (x, b) in placed.items():
        s = SPRITES[iid]
        if s["anchor"] == "wall" and walls:
            scene.put(im, s, x, b - lift(iid))
    for name, x, b, lvl in ROOM_MESS:
        if name == "cobweb" and room >= lvl and walls:
            scene.put(im, FX[name], x, b)
    for iid, (x, b) in placed.items():
        s = SPRITES[iid]
        if s["anchor"] == "floor" and s["flat"]:
            scene.put(im, s, x, b - lift(iid))

    standing = []
    for iid, (x, b) in placed.items():
        s = SPRITES[iid]
        if s["anchor"] == "floor" and not s["flat"]:
            standing.append((b, 0, "item", iid, x))
    for name, x, b, lvl in ROOM_MESS:
        if name != "cobweb" and room >= lvl:
            standing.append((b, 1, "fx", name, x))
    bob = 1 if frame % 2 else 0
    mx, mb = ME_SPOT
    standing.append((mb, 2, "me", body, mx))
    standing.append((mb, 3, "hat", HAT, mx))
    for b, _, kind, ref, x in sorted(standing, key=lambda t: (t[0], t[1])):
        if kind == "item":
            scene.put(im, SPRITES[ref], x, b - lift(ref))
        elif kind == "fx":
            scene.put(im, FX[ref], x, b)
        elif kind == "me":
            im.alpha_composite(me_image(ref), (x, b - ME_H - bob))
        else:
            scene.put(im, FX[ref], x, b - ME_H + FX[ref]["h"] - bob)

    bx, lx = scene.sides(ME_SPOT)
    if letter:
        lb = mb - LETTER_DY - bob
        scene.put(im, FX["letter"], lx, lb)
        scene.put(im, FX["bang"], lx + (FX["letter"]["w"] - FX["bang"]["w"]) // 2,
                  lb - FX["letter"]["h"] - BANG_GAP)
    scene.put(im, FX[f"buddy{heart}"], bx, mb - BUDDY_DY - (1 - bob))

    if room == 0:
        for i, (x, y) in enumerate(ROOM_SPARKLES):
            if (i + frame) % 2 == 0:
                scene.put(im, FX["sparkle"], x, y)
    if body == 0:
        scene.put(im, FX["sparkle"], mx + 2, mb - ME_H + 6 - bob)
    if burst:
        for i, (x, y) in enumerate(BURST):
            if (i + frame) % 3 != 0:
                scene.put(im, FX["sparkle"], x, y)
    return im


def dissolve(a, b, p, seed=7):
    """From shell a to shell b in whole room pixels, in a fixed random order: p in 0..1."""
    rnd = random.Random(seed)
    order = [rnd.random() for _ in range(W * H)]
    out = a.copy()
    pa, pb = out.load(), b.load()
    for y in range(H):
        for x in range(W):
            if order[y * W + x] < p:
                pa[x, y] = pb[x, y]
    return out


def spots(ids):
    return {i: tuple(SPRITES[i]["spot"]) for i in ids}
