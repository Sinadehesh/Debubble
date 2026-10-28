"""Render the sprites as a contact sheet, so they can be looked at instead of imagined."""
import sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from PIL import Image, ImageDraw
from palette import rgb
from sprites import SPRITES

Z = 6            # zoom
PAD = 14
COLS = 5
BG = (38, 30, 44)


def draw(spec):
    im = Image.new("RGBA", (spec["w"], spec["h"]), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(spec["rows"]):
        for x, ch in enumerate(row):
            c = rgb(ch)
            if c:
                px[x, y] = c + (255,)
    return im.resize((spec["w"] * Z, spec["h"] * Z), Image.NEAREST)


names = list(SPRITES)
tiles = {n: draw(SPRITES[n]) for n in names}
cw = max(t.width for t in tiles.values()) + PAD * 2
ch = max(t.height for t in tiles.values()) + PAD * 2 + 16
rows = (len(names) + COLS - 1) // COLS
sheet = Image.new("RGB", (cw * COLS, ch * rows), BG)
d = ImageDraw.Draw(sheet)
for i, n in enumerate(names):
    t = tiles[n]
    cx, cy = (i % COLS) * cw, (i // COLS) * ch
    sheet.paste(t, (cx + (cw - t.width) // 2, cy + PAD), t)
    s = SPRITES[n]
    d.text((cx + 6, cy + ch - 14), f'{n} {s["w"]}x{s["h"]} {s["anchor"]}', fill=(220, 210, 200))
sheet.save(sys.argv[1] if len(sys.argv) > 1 else "sprites.png")
print("wrote", sys.argv[1], sheet.size, f"| {len(names)} sprites")
