"""The room, drawn as pixel art, one shell per stage.

A flat side-on elevation, not one-point perspective. Perspective was what the previous
version used and it is the wrong projection for this: a room you put things into needs a
floor line things stand on and a wall plane things hang flat against, and perspective gives
you neither without faking a scale for every object. Side-on is also the friendlier read —
it is a dollhouse rather than a corridor.

160x100 logical pixels. The wall is the top 62 rows, the floor the rest, and everything is
placed against those two numbers.

The seven stages open the room up and warm it up at the same time: bare wall, a window, a
door, a wide window onto a street, a wall half gone, a horizon, then no back wall at all.
Brightness rises with them, which the old navy version could not do because its palette was
rationed by contrast — here the room is a picture with nothing written on it, so it is not.
"""

import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from palette import rgb  # noqa: E402

W, H = 160, 100
FLOOR_Y = 62          # first row of floor
SKIRT = 3             # skirting board height, sits above FLOOR_Y

STAGES = ["sealed", "window", "door", "street", "open", "horizon", "unwalled"]


class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = [["." for _ in range(w)] for _ in range(h)]

    def set(self, x, y, ch):
        if 0 <= x < self.w and 0 <= y < self.h and ch != ".":
            self.px[y][x] = ch

    def rect(self, x, y, w, h, ch):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.set(xx, yy, ch)

    def frame(self, x, y, w, h, ch):
        for xx in range(x, x + w):
            self.set(xx, y, ch); self.set(xx, y + h - 1, ch)
        for yy in range(y, y + h):
            self.set(x, yy, ch); self.set(x + w - 1, yy, ch)

    def rows(self):
        return ["".join(r) for r in self.px]


def sky(c, x, y, w, h, warm):
    """Sky inside an opening, with a couple of clouds and a sun once it is wide enough."""
    for yy in range(y, y + h):
        t = (yy - y) / max(1, h - 1)
        c.rect(x, yy, w, 1, "s" if t < 0.45 else "S")
    # Clouds. Fixed positions rather than random: a sky that reshuffles between stages
    # reads as a different place, and this is meant to be the same window.
    for cx, cy, cw in ((x + 3, y + 3, 7), (x + w - 11, y + 6, 8), (x + 6, y + 10, 5)):
        if cx + cw < x + w - 1 and cy < y + h - 2:
            c.rect(cx, cy, cw, 1, "i")
            c.rect(cx + 1, cy + 1, max(1, cw - 2), 1, "i")
    if warm and w > 30:
        for yy in range(5):                       # a small sun
            for xx in range(5):
                if abs(xx - 2) + abs(yy - 2) <= 3:
                    c.set(x + w - 10 + xx, y + 3 + yy, "y" if abs(xx - 2) + abs(yy - 2) < 2 else "Y")


def hills(c, x, y, w, base):
    """A low skyline along the bottom of an opening."""
    for xx in range(x, x + w):
        t = (xx - x) / max(1, w - 1)
        import math
        top = base - int(4 + 3 * math.sin(t * 7.0) + 2 * math.sin(t * 2.3 + 1))
        for yy in range(top, base):
            c.set(xx, yy, "h" if yy < top + 2 else "G")


def light_pool(c, x, w, strength):
    """Daylight landing on the floor under an opening.

    A clean trapezoid widening toward the viewer. The first version grew by a fixed
    multiple per row and came out as a lump; the light has to keep the straight edges a
    rectangular opening actually casts, or it reads as a stain on the floorboards.
    """
    for i in range(strength):
        yy = FLOOR_Y + 1 + i
        if yy >= H:
            break
        spread = int(w * (0.72 + 0.14 * i))
        sx = x + w // 2 - spread // 2
        for xx in range(max(0, sx), min(W, sx + spread)):
            cur = c.px[yy][xx]
            if cur in ("F", "d"):
                c.px[yy][xx] = "f"
            elif cur == "f" and i < 2:
                c.px[yy][xx] = "y"              # a warm core right under the opening


def shell(stage):
    c = Canvas(W, H)
    i = STAGES.index(stage)
    warmth = i / (len(STAGES) - 1)

    # --- wall. Lighter at the top, where the light is.
    for y in range(FLOOR_Y):
        t = y / FLOOR_Y
        c.rect(0, y, W, 1, "w" if t < 0.35 else ("W" if t < 0.8 else "v"))

    # --- floor. Boards running away from the viewer.
    #
    # The first version put a seam every 7 rows and a staggered board end every 23 columns,
    # which is a brick bond, and it read as a brick wall laid flat. Two things fix it: the
    # seams bunch up toward the back, which is the only perspective cue a flat elevation
    # gets, and the board ends are few and irregular. Regular board ends are what made it
    # masonry.
    for y in range(FLOOR_Y, H):
        t = (y - FLOOR_Y) / (H - FLOOR_Y)
        c.rect(0, y, W, 1, "d" if t < 0.18 else ("F" if t < 0.62 else "f"))

    seams, y, gap = [], FLOOR_Y + 2, 3.0
    while y < H:
        seams.append(int(y))
        c.rect(0, int(y), W, 1, "d")
        y += gap
        gap *= 1.42                                # boards widen toward the viewer
    # A handful of board ends, at positions that do not line up with each other.
    for band, x in ((1, 104), (2, 41), (3, 123), (4, 28), (5, 88)):
        if band < len(seams) - 1:
            for yy in range(seams[band], seams[band + 1]):
                c.set(x, yy, "d")

    # --- skirting board
    c.rect(0, FLOOR_Y - SKIRT, W, SKIRT, "v")
    c.rect(0, FLOOR_Y - SKIRT, W, 1, "K")
    c.rect(0, FLOOR_Y, W, 1, "k")

    # --- what is in the back wall, by stage
    if stage == "sealed":
        pass                                       # nothing. That is the point of it.

    elif stage == "window":
        x, y, w, h = 62, 16, 34, 26
        c.frame(x - 2, y - 2, w + 4, h + 4, "k")
        c.rect(x - 1, y - 1, w + 2, h + 2, "i")
        sky(c, x, y, w, h, False)
        c.rect(x + w // 2 - 1, y, 2, h, "i")       # mullion
        c.rect(x, y + h // 2 - 1, w, 2, "i")
        light_pool(c, x, w, 5)

    elif stage == "door":
        x, y, w, h = 58, 14, 40, 30
        c.frame(x - 2, y - 2, w + 4, h + 4, "k")
        c.rect(x - 1, y - 1, w + 2, h + 2, "i")
        sky(c, x, y, w, h, False)
        c.rect(x + w // 2 - 1, y, 2, h, "i")
        light_pool(c, x, w, 6)
        # An open door on the left: the leaf swung back against the wall, and the opening
        # beside it full of light. A plain lit rectangle read as a yellow sticker.
        dx, dy, dw, dh = 16, 24, 26, FLOOR_Y - 24
        c.frame(dx - 2, dy - 2, dw + 4, dh + 2, "k")
        c.rect(dx, dy, dw, dh, "Y")                # daylight in the opening
        c.rect(dx, dy, dw, 3, "y")
        leaf = dw // 2
        c.rect(dx, dy, leaf, dh, "T")              # the door itself, standing open
        c.rect(dx + 1, dy + 1, leaf - 2, dh - 1, "t")
        for py in (dy + 4, dy + dh // 2 + 2):      # two recessed panels
            c.frame(dx + 3, py, leaf - 6, dh // 2 - 7, "r")
        c.rect(dx + leaf - 4, dy + dh // 2, 2, 2, "k")   # handle
        c.rect(dx + leaf, dy, 1, dh, "k")          # the open edge
        light_pool(c, dx + leaf, dw - leaf, 7)

    elif stage == "street":
        x, y, w, h = 20, 12, 120, 34
        c.frame(x - 2, y - 2, w + 4, h + 4, "k")
        c.rect(x - 1, y - 1, w + 2, h + 2, "i")
        sky(c, x, y, w, h, False)
        for bx, bw, bh in ((26, 18, 14), (50, 14, 20), (70, 20, 11), (96, 16, 17), (118, 18, 13)):
            c.rect(bx, y + h - bh, bw, bh, "n")
            for wy in range(y + h - bh + 2, y + h - 2, 4):
                for wx in range(bx + 2, bx + bw - 2, 4):
                    c.set(wx, wy, "y")
        for mx in (x + w // 3, x + 2 * w // 3):
            c.rect(mx, y, 2, h, "i")
        light_pool(c, x, w, 8)

    elif stage == "open":
        x, y, w, h = 14, 10, 132, 42
        c.frame(x - 2, y - 2, w + 4, h + 4, "k")
        sky(c, x, y, w, h, True)
        hills(c, x, y, w, y + h)
        c.rect(x + w // 2 - 1, y, 2, h, "i")
        light_pool(c, x, w, 10)

    elif stage == "horizon":
        x, y, w, h = 6, 6, 148, 50
        c.frame(x - 2, y - 2, w + 4, h + 4, "k")
        sky(c, x, y, w, h, True)
        hills(c, x, y, w, y + h)
        for bx, by in ((x + 24, y + 10), (x + 30, y + 7), (x + 36, y + 11)):
            c.set(bx, by, "k"); c.set(bx + 1, by - 1, "k"); c.set(bx + 2, by, "k")
        light_pool(c, x, w, 12)

    else:  # unwalled — the wall is gone, the room is a terrace
        c.rect(0, 0, W, FLOOR_Y - SKIRT, ".")
        sky(c, 0, 0, W, FLOOR_Y - SKIRT, True)
        hills(c, 0, 0, W, FLOOR_Y - SKIRT)
        for bx, by in ((30, 14), (38, 11), (46, 15), (112, 18), (120, 15)):
            c.set(bx, by, "k"); c.set(bx + 1, by - 1, "k"); c.set(bx + 2, by, "k")
        c.rect(0, FLOOR_Y - SKIRT, W, 1, "k")
        for y in range(FLOOR_Y, H):                # the whole floor is sunlit now
            for x in range(W):
                if c.px[y][x] == "d":
                    c.px[y][x] = "F"
                elif c.px[y][x] == "F":
                    c.px[y][x] = "f"

    return c


def to_image(c):
    from PIL import Image
    im = Image.new("RGBA", (c.w, c.h), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(c.rows()):
        for x, ch in enumerate(row):
            col = rgb(ch)
            if col:
                px[x, y] = col + (255,)
    return im


if __name__ == "__main__":
    from PIL import Image, ImageDraw
    Z, PAD = 3, 10
    ims = [(s, to_image(shell(s))) for s in STAGES]
    cw = W * Z + PAD * 2
    ch = H * Z + PAD * 2 + 14
    cols = 4
    rows = (len(ims) + cols - 1) // cols
    sheet = Image.new("RGB", (cw * cols, ch * rows), (38, 30, 44))
    d = ImageDraw.Draw(sheet)
    for i, (name, im) in enumerate(ims):
        big = im.resize((W * Z, H * Z), Image.NEAREST)
        cx, cy = (i % cols) * cw, (i // cols) * ch
        sheet.paste(big, (cx + PAD, cy + PAD), big)
        d.text((cx + PAD, cy + ch - 13), f"{i+1}. {name}", fill=(225, 215, 205))
    out = sys.argv[1] if len(sys.argv) > 1 else "rooms.png"
    sheet.save(out)
    print("wrote", out, sheet.size)
