"""Put items in the room and look at it. Scale is the thing that only shows up composited."""
import sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from PIL import Image, ImageDraw
from room import shell, to_image, W, H, FLOOR_Y, SKIRT
from sprites import SPRITES
from palette import rgb

Z = 4


def sprite_img(name):
    s = SPRITES[name]
    im = Image.new("RGBA", (s["w"], s["h"]), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(s["rows"]):
        for x, ch in enumerate(row):
            c = rgb(ch)
            if c:
                px[x, y] = c + (255,)
    return im


def place(base, name, x, baseline):
    """Put a sprite's bottom edge on `baseline`.

    One rule for everything, floor and wall alike. The first version forced every floor item
    onto FLOOR_Y, which is the back wall junction — so the whole room stood in a single row
    against the skirting, items collided with the window frame, and the rug vanished because
    a rug is 10px tall and ended up hovering at head height for a cat.

    On the floor, y IS depth: a bigger baseline is nearer the viewer.
    """
    base.alpha_composite(sprite_img(name), (x, baseline - SPRITES[name]["h"]))


# Scenes use real depths now, and floor items draw back to front so overlaps are right.
scenes = [
    ("early \u00b7 sealed", "sealed",
     [("poster", 70, 32), ("plant", 18, 76), ("books", 122, 88)]),
    ("mid \u00b7 door", "door",
     [("clock", 112, 30), ("rug", 58, 92), ("plant", 120, 74),
      ("cat", 64, 88), ("lamp", 140, 80)]),
    ("late \u00b7 horizon", "horizon",
     [("shelf", 20, 40), ("rug", 44, 94), ("beanbag", 22, 80),
      ("plant", 2, 72), ("cat", 52, 92), ("record", 96, 74),
      ("guitar", 124, 86), ("lamp", 146, 78)]),
    ("full \u00b7 unwalled", "unwalled",
     [("rug", 46, 96), ("beanbag", 18, 82), ("plant", 1, 70),
      ("cat", 54, 94), ("books", 88, 72), ("record", 100, 80),
      ("guitar", 122, 90), ("lamp", 144, 76)]),
]

PAD = 10
cw, ch = W * Z + PAD * 2, H * Z + PAD * 2 + 14
sheet = Image.new("RGB", (cw * 2, ch * 2), (38, 30, 44))
d = ImageDraw.Draw(sheet)
for i, (title, stage, items) in enumerate(scenes):
    base = to_image(shell(stage))
    # Back to front. Without this a nearer item can be drawn under a further one.
    for name, x, baseline in sorted(items, key=lambda it: it[2]):
        place(base, name, x, baseline)
    big = base.resize((W * Z, H * Z), Image.NEAREST)
    cx, cy = (i % 2) * cw, (i // 2) * ch
    sheet.paste(big, (cx + PAD, cy + PAD), big)
    d.text((cx + PAD, cy + ch - 13), title, fill=(225, 215, 205))
out = sys.argv[1] if len(sys.argv) > 1 else "composed.png"
sheet.save(out)
print("wrote", out, sheet.size)
