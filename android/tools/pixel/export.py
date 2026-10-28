#!/usr/bin/env python3
"""Export the pixel room into the Android project.

Two artefacts, both lossless, because pixel art does not survive lossy compression — a
quality-82 WebP turns a one-pixel outline into a three-pixel smear and the whole thing stops
being pixel art.

  res/drawable-nodpi/room_<stage>.webp   the seven room shells, 160x100
  res/drawable-nodpi/widget_room_preview.webp
                                         a lived-in room at 3x: the widget picker's preview,
                                         and what the widget shows before its first paint
  assets/pixel/items.webp                one atlas: every item, you, the mess, your heart,
                                         today's letter and the task icons
  assets/pixel/items.json                where each sprite is in the atlas, which family
                                         earns it, where it lands, and where the mess goes

An atlas rather than one file per sprite: fifty separate bitmaps is fifty decode calls and
fifty resource ids, and the index has to exist anyway to carry families and spots.

Run:  python3 android/tools/pixel/export.py
"""

import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from PIL import Image                      # noqa: E402
from palette import PALETTE, rgb           # noqa: E402
from room import STAGES, shell, to_image, W, H, FLOOR_Y, SKIRT   # noqa: E402
from sprites import SPRITES, FAMILIES, check as check_sprites   # noqa: E402
from fx import (FX, ROOM_MESS, ROOM_SPARKLES, ME_SPOT, ME_W, ME_H, MAX_DIRT,  # noqa: E402
                BUDDY_DX, BUDDY_DY, LETTER_DX, LETTER_DY, BANG_GAP, check as check_fx)
from palette import LOOKS, LOOK_KEYS       # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parents[2]
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "pixel"

PAD = 1          # transparent gutter between atlas entries, so no sprite bleeds into another


def sprite_image(spec):
    im = Image.new("RGBA", (spec["w"], spec["h"]), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(spec["rows"]):
        for x, ch in enumerate(row):
            c = rgb(ch)
            if c:
                px[x, y] = c + (255,)
    return im


def build_atlas():
    """Shelf-pack every sprite, tallest first. Simple and good enough for fifty."""
    items = sorted(list(SPRITES.values()) + list(FX.values()), key=lambda s: (-s["h"], s["name"]))
    max_w = 256
    x = y = row_h = 0
    placed = []
    for s in items:
        if x + s["w"] + PAD > max_w:
            x, y, row_h = 0, y + row_h + PAD, 0
        placed.append((s, x, y))
        x += s["w"] + PAD
        row_h = max(row_h, s["h"])
    height = y + row_h
    atlas = Image.new("RGBA", (max_w, height), (0, 0, 0, 0))
    items, fx = [], []
    for s, sx, sy in placed:
        atlas.alpha_composite(sprite_image(s), (sx, sy))
        rect = {"x": sx, "y": sy, "w": s["w"], "h": s["h"]}
        if s["name"] in SPRITES:
            items.append({
                "id": s["name"], "label": s["label"], "family": s["family"],
                "anchor": s["anchor"], "flat": s["flat"], "first": s["first"],
                "spot": {"x": s["spot"][0], "b": s["spot"][1]}, **rect,
            })
        else:
            fx.append({"id": s["name"], **rect})
    # Catalogue order, not packing order: it is the order the reward picker walks.
    order = list(SPRITES)
    items.sort(key=lambda i: order.index(i["id"]))
    fx.sort(key=lambda f: f["id"])
    return atlas, items, fx


def main():
    DRAWABLE.mkdir(parents=True, exist_ok=True)
    ASSETS.mkdir(parents=True, exist_ok=True)

    total = 0
    for stage in STAGES:
        out = DRAWABLE / f"room_{stage}.webp"
        to_image(shell(stage)).save(out, "WEBP", lossless=True, quality=100, method=6)
        total += out.stat().st_size
        print(f"  room_{stage:9} {out.stat().st_size // 1024:>3} KB  {W}x{H}")

    problems = check_sprites() + check_fx()
    for i in list(SPRITES.values()):
        x, b = i["spot"]
        lo, hi = ((FLOOR_Y + 2, H) if i["anchor"] == "floor" else (i["h"] + 2, FLOOR_Y - SKIRT))
        if not (lo <= b <= hi) or x < 0 or x + i["w"] > W:
            problems.append(f"{i['name']}: default spot {i['spot']} is outside the {i['anchor']}")
    if problems:
        for p in problems:
            print("FAIL", p)
        return 1

    # The widget's stand-in: a week in, a bit lived-in, today's letter in hand. Scaled here,
    # nearest-neighbour, because a launcher scaling a 160px image would blur every edge.
    from scene import compose, default_spots
    preview = compose("window", default_spots(["bed", "lamp", "plant", "rug", "water", "map", "cat"]),
                      1, 1, 1, letter=True).resize((W * 3, H * 3), Image.NEAREST)
    ppath = DRAWABLE / "widget_room_preview.webp"
    preview.save(ppath, "WEBP", lossless=True, quality=100, method=6)
    total += ppath.stat().st_size
    print(f"  widget_room_preview {ppath.stat().st_size // 1024:>3} KB  {preview.width}x{preview.height}")

    atlas, index, fx = build_atlas()
    apath = ASSETS / "items.webp"
    atlas.save(apath, "WEBP", lossless=True, quality=100, method=6)

    manifest = {
        "atlas": "pixel/items.webp",
        "atlasWidth": atlas.width,
        "atlasHeight": atlas.height,
        # The room's own geometry, so the Kotlin and the web renderer agree about where the
        # floor is without either of them hard-coding it.
        "room": {"w": W, "h": H, "floorY": FLOOR_Y, "skirt": SKIRT},
        "stages": STAGES,
        "palette": {k: v[0] for k, v in PALETTE.items() if v[0]},
        "families": list(FAMILIES),
        "items": index,
        "fx": fx,
        # Where the Room mess goes, by level, and where a clean room sparkles. See fx.py.
        "mess": [{"fx": n, "x": x, "b": b, "level": lvl} for n, x, b, lvl in ROOM_MESS],
        "sparkles": [{"x": x, "y": y} for x, y in ROOM_SPARKLES],
        "me": {"x": ME_SPOT[0], "b": ME_SPOT[1], "w": ME_W, "h": ME_H,
               "buddyDx": BUDDY_DX, "buddyDy": BUDDY_DY,
               "letterDx": LETTER_DX, "letterDy": LETTER_DY, "bangGap": BANG_GAP},
        # The ramps a character can be drawn in. Entry 0 is what the atlas holds; the app
        # swaps by exact colour inside the "me" frames. See palette.LOOKS.
        "looks": {k: [list(r) for r in v] for k, v in LOOKS.items()},
        "maxDirt": MAX_DIRT,
    }
    jpath = ASSETS / "items.json"
    jpath.write_text(json.dumps(manifest, indent=1) + "\n")

    print(f"  items.webp    {apath.stat().st_size // 1024:>3} KB  "
          f"{atlas.width}x{atlas.height}  {len(index)} items + {len(fx)} fx")
    print(f"  items.json    {jpath.stat().st_size // 1024:>3} KB")
    print(f"\ntotal {(total + apath.stat().st_size + jpath.stat().st_size) // 1024} KB")

    for fam in FAMILIES:
        n = sum(1 for i in index if i["family"] == fam)
        print(f"  {fam:5} {n} items, first: "
              f"{next(i['id'] for i in index if i['family'] == fam and i['first'])}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
