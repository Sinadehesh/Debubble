"""Compose the whole room: shell, furniture, mess, you, your heart, and today's letter.

This is the reference for draw order. The Android HouseScene and the web preview both follow
it exactly, and the order is the part that is easy to get subtly wrong:

  1. the room shell for the stage
  2. wall items, then the cobweb (it hangs in the corner, over anything on the wall)
  3. flat things on the floor — a rug, a mat — under everything that stands
  4. everything standing, sorted by baseline: furniture, mess and you together, so a sock
     in front of the bed is in front of it and a cat behind you is behind you
  5. today's letter in your hand, with its "!", when there is a mission still to do
  6. your heart buddy at your shoulder, a beat out of step with you
  7. sparkles, last, because they are light

Run:  python3 scene.py out.png      (a contact sheet of states, for looking at)
"""

import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from PIL import Image  # noqa: E402
from palette import rgb  # noqa: E402
from room import shell, to_image, W, H  # noqa: E402
from sprites import SPRITES  # noqa: E402
from fx import (FX, ROOM_MESS, ROOM_SPARKLES, ME_SPOT, ME_W, ME_H,  # noqa: E402
                BUDDY_DX, BUDDY_DY, LETTER_DX, LETTER_DY, BANG_GAP)


def img(spec):
    im = Image.new("RGBA", (spec["w"], spec["h"]), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(spec["rows"]):
        for x, ch in enumerate(row):
            c = rgb(ch)
            if c:
                px[x, y] = c + (255,)
    return im


def put(base, spec, x, baseline):
    base.alpha_composite(img(spec), (x, baseline - spec["h"]))


def sides(me):
    """Where the buddy and the letter go: (buddy_x, letter_x). The buddy takes your right
    unless that leaves the room; the letter takes whichever side the buddy did not."""
    bw, lw = FX["buddy0"]["w"], FX["letter"]["w"]
    if me[0] + BUDDY_DX + bw <= W:
        bx, lx = me[0] + BUDDY_DX, me[0] + LETTER_DX
    else:
        bx, lx = me[0] + ME_W - BUDDY_DX - bw, me[0] + ME_W - LETTER_DX - lw
    return bx, max(0, min(W - lw, lx))


def compose(stage, placed, body, room, heart, me=ME_SPOT, frame=0, letter=False):
    """placed: {item_id: (x, baseline)}. Levels are 0..3. letter: a mission is still open."""
    base = to_image(shell(stage))
    walls_gone = stage == "unwalled"

    for iid, (x, b) in placed.items():
        s = SPRITES[iid]
        if s["anchor"] == "wall" and not walls_gone:
            put(base, s, x, b)
    for name, x, b, lvl in ROOM_MESS:
        if name == "cobweb" and room >= lvl and not walls_gone:
            put(base, FX[name], x, b)

    for iid, (x, b) in placed.items():
        s = SPRITES[iid]
        if s["anchor"] == "floor" and s["flat"]:
            put(base, s, x, b)

    standing = []                                  # (baseline, tiebreak, spec, x)
    for iid, (x, b) in placed.items():
        s = SPRITES[iid]
        if s["anchor"] == "floor" and not s["flat"]:
            standing.append((b, 0, s, x))
    for name, x, b, lvl in ROOM_MESS:
        if name != "cobweb" and room >= lvl:
            standing.append((b, 1, FX[name], x))
    bob = 1 if frame % 2 else 0
    standing.append((me[1], 2, FX[f"me{body}"], me[0]))
    for b, tie, s, x in sorted(standing, key=lambda t: (t[0], t[1])):
        put(base, s, x, b - (bob if tie == 2 else 0))

    bx, lx = sides(me)
    if letter:
        lb = me[1] - LETTER_DY - bob
        put(base, FX["letter"], lx, lb)
        put(base, FX["bang"], lx + (FX["letter"]["w"] - FX["bang"]["w"]) // 2,
            lb - FX["letter"]["h"] - BANG_GAP)
    put(base, FX[f"buddy{heart}"], bx, me[1] - BUDDY_DY - (1 - bob))

    if room == 0:
        for i, (x, y) in enumerate(ROOM_SPARKLES):
            if (i + frame) % 2 == 0:
                put(base, FX["sparkle"], x, y)
    if body == 0:
        put(base, FX["sparkle"], me[0] + 2, me[1] - ME_H + 6 - bob)
    return base


def default_spots(ids):
    return {i: tuple(SPRITES[i]["spot"]) for i in ids}


if __name__ == "__main__":
    from PIL import ImageDraw
    Z, PAD = 4, 10
    everything = list(SPRITES)
    week = ["bed", "lamp", "plant", "rug", "water", "map"]
    scenes = [
        ("day 1, before anything: nothing, and it is messy", "sealed", [], 3, 3, 3),
        ("day 1, after the three tiny ones", "sealed", ["bed", "lamp", "plant"], 1, 2, 1),
        ("week 2, mostly kept up", "window", week + ["fruit", "basket", "cat"], 0, 1, 0),
        ("two months, a bad week", "door", everything[:28], 2, 3, 2),
        ("two months, all clean", "street", everything, 0, 0, 0),
        ("far out, everything", "horizon", everything, 0, 0, 0),
    ]
    cw, ch = W * Z + PAD * 2, H * Z + PAD * 2 + 14
    sheet = Image.new("RGB", (cw * 2, ch * 3), (38, 30, 44))
    d = ImageDraw.Draw(sheet)
    for i, (title, stage, ids, body, room, heart) in enumerate(scenes):
        im = compose(stage, default_spots(ids), body, room, heart, letter=i % 2 == 0)
        big = im.resize((W * Z, H * Z), Image.NEAREST)
        cx, cy = (i % 2) * cw, (i // 2) * ch
        sheet.paste(big, (cx + PAD, cy + PAD), big)
        d.text((cx + PAD, cy + ch - 13), f"{title}  [body {body} room {room} heart {heart}]",
               fill=(225, 215, 205))
    out = sys.argv[1] if len(sys.argv) > 1 else "scenes.png"
    sheet.save(out)
    print("wrote", out, sheet.size)
