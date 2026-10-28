#!/usr/bin/env python3
"""Export the pixel room into the Android project.

Two artefacts, both lossless, because pixel art does not survive lossy compression — a
quality-82 WebP turns a one-pixel outline into a three-pixel smear and the whole thing stops
being pixel art.

  res/drawable-nodpi/room_<stage>.webp   the seven room shells, 160x100
  assets/pixel/items.webp                one atlas holding every item
  assets/pixel/items.json                where each item is in the atlas, and what unlocks it

An atlas rather than one file per item: fifteen separate bitmaps is fifteen decode calls and
fifteen resource ids, and the index has to exist anyway to carry the unlock rules.

Run:  python3 android/tools/pixel/export.py
"""

import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from PIL import Image                      # noqa: E402
from palette import PALETTE, rgb           # noqa: E402
from room import STAGES, shell, to_image, W, H, FLOOR_Y, SKIRT   # noqa: E402
from sprites import SPRITES                # noqa: E402

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
    """Shelf-pack the sprites, tallest first. Simple and good enough for fifteen items."""
    items = sorted(SPRITES.values(), key=lambda s: -s["h"])
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
    index = []
    for s, sx, sy in placed:
        atlas.alpha_composite(sprite_image(s), (sx, sy))
        index.append({
            "id": s["name"], "label": s["label"], "anchor": s["anchor"],
            "unlock": {"currency": s["unlock"][0], "amount": s["unlock"][1]},
            "x": sx, "y": sy, "w": s["w"], "h": s["h"],
        })
    return atlas, index


def main():
    DRAWABLE.mkdir(parents=True, exist_ok=True)
    ASSETS.mkdir(parents=True, exist_ok=True)

    total = 0
    for stage in STAGES:
        out = DRAWABLE / f"room_{stage}.webp"
        to_image(shell(stage)).save(out, "WEBP", lossless=True, quality=100, method=6)
        total += out.stat().st_size
        print(f"  room_{stage:9} {out.stat().st_size // 1024:>3} KB  {W}x{H}")

    atlas, index = build_atlas()
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
        "items": index,
    }
    jpath = ASSETS / "items.json"
    jpath.write_text(json.dumps(manifest, indent=1) + "\n")

    print(f"  items.webp    {apath.stat().st_size // 1024:>3} KB  "
          f"{atlas.width}x{atlas.height}  {len(index)} sprites")
    print(f"  items.json    {jpath.stat().st_size // 1024:>3} KB")
    print(f"\ntotal {(total + apath.stat().st_size + jpath.stat().st_size) // 1024} KB")

    # Every sprite must actually be reachable: an unlock rule naming a currency nothing
    # grants is an item nobody can ever get, and it would look exactly like a bug nobody
    # reports because the item simply never appears.
    currencies = {"rungs", "level", "friction"}
    bad = [i["id"] for i in index if i["unlock"]["currency"] not in currencies]
    if bad:
        print(f"\nunknown unlock currency on: {bad}")
        return 1
    starters = [i["id"] for i in index if i["unlock"]["amount"] == 0]
    if not starters:
        print("\nnothing unlocks at zero — a new user opens an empty room with an empty tray")
        return 1
    print(f"available on day one: {', '.join(starters)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
