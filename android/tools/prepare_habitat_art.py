#!/usr/bin/env python3
"""Turn the Canva habitat exports into shippable Android assets, and refuse unreadable ones.

Two jobs. Resize and compress the 1536x1024 exports down to something an APK can carry, and
verify that the text drawn over them is actually readable — which is the part that would
otherwise go wrong silently.

That check is not optional here. The old habitat was a procedural backdrop whose brightness
was solved against the WCAG floors; these are renders reaching luma 251.

The first version of this put the stage name over the artwork behind a gradient scrim, and
this script killed it: on "door open" the light wedge spills straight down the middle of the
floor and Muted came out at 1.01:1 — text and background the same colour. The scrim needed to
fix that would have buried the brightest, best part of the image, which is self-defeating when
the whole reason for the artwork is that the app had nothing to look at. So nothing is drawn
over the art. The stage name sits underneath it, on solid ground, and the scrim's only job is
to blend the bottom edge into the page so the hero does not end on a hard seam.

Usage:  python3 android/tools/prepare_habitat_art.py <src-dir>
        where <src-dir> holds the Canva exports s1 s2 s3 s7 .png, and optionally
        s4 s5 s6 .png, which are derived when absent.
"""

import pathlib
import sys

from PIL import Image

OUT_W, OUT_H = 1080, 720
QUALITY = 82

# The page colour the scrim fades into.
GROUND = (0x0E, 0x11, 0x16)
# The scrim, as fractions of the hero's height: fully transparent above SCRIM_TOP, fully
# ground at the bottom edge. Must match HabitatHero in ui/components/Habitat.kt. Shallow on
# purpose — it exists to hide the seam, not to make anything readable.
# SCRIM_END is deliberately short of 1.0. A gradient that only reaches full opacity *at* the
# last row never actually gets there — the first attempt used 1.0 and every hero ended on a
# visible hairline where the floor grid survived. It has to complete before the edge.
SCRIM_TOP, SCRIM_END = 0.70, 0.96

# How close to the ground colour the last rows have to land for the seam to be invisible.
# A Euclidean distance in sRGB, which is crude but more than good enough for "is this edge
# the same colour as the page".
SEAM_ROWS = 0.02
SEAM_TOLERANCE = 6.0

# The four that Canva actually generated before the account hit its credit quota.
REAL = {"s1.png": "habitat_sealed", "s2.png": "habitat_window",
        "s3.png": "habitat_door", "s7.png": "habitat_unwalled"}

# The three it did not. These are DERIVED, not generated: composited out of "door open" and
# "no walls" with a vertically varying alpha, so the room (walls, ceiling and doorway, all in
# the upper half) dissolves ahead of the grid floor the two images share and which is meant to
# persist at every stage. It works because both renders have the same vanishing point, the
# same grid and the same palette — a flat cross-dissolve of two unrelated images would be a
# double exposure, and this is not that.
#
# They are good enough to ship and they are not the real thing. Regenerate them properly from
# the prompts in docs/habitat-prompts.md once the Canva quota resets, drop them into the source
# directory as s4/s5/s6.png, and this script will prefer them automatically.
DERIVED = {"habitat_street": 0.50, "habitat_open": 0.67, "habitat_horizon": 0.84}
DERIVED_FILES = {"habitat_street": "s4.png", "habitat_open": "s5.png",
                 "habitat_horizon": "s6.png"}
LO_OPENNESS, HI_OPENNESS = 0.33, 1.0   # openness of "door open" and "no walls" in Habitat.kt


def scrimmed(im):
    """Composite the Compose scrim onto the image, so the check sees what the eye will."""
    out = im.convert("RGB").copy()
    px = out.load()
    w, h = out.size
    for y in range(h):
        f = (y / h - SCRIM_TOP) / (SCRIM_END - SCRIM_TOP)
        a = min(1.0, max(0.0, f))
        if a <= 0:
            continue
        for x in range(w):
            r, g, b = px[x, y]
            px[x, y] = (round(r + (GROUND[0] - r) * a),
                        round(g + (GROUND[1] - g) * a),
                        round(b + (GROUND[2] - b) * a))
    return out


def dissolve_mask(size, t):
    """Alpha ramp that crosses over at the top of the frame well before the bottom."""
    w, h = size
    m = Image.new("L", size)
    px = m.load()
    for y in range(h):
        v = round(max(0.0, min(1.0, t * (1.45 - 0.90 * (y / h)))) * 255)
        for x in range(w):
            px[x, y] = v
    return m


def worst_seam(im):
    """How far the bottom edge is from the page colour, at its worst pixel.

    The worst pixel rather than the mean: one bright line surviving the scrim is a visible
    hairline across the bottom of the hero, and a mean would not show it.
    """
    w, h = im.size
    band = im.crop((0, int(h * (1 - SEAM_ROWS)), w, h)).convert("RGB")
    return max(
        (sum((c - g) ** 2 for c, g in zip(px, GROUND)) ** 0.5
         for px in band.get_flattened_data()),
        default=0.0,
    )


def main():
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    src = pathlib.Path(sys.argv[1])
    dst = (pathlib.Path(__file__).resolve().parent.parent
           / "app" / "src" / "main" / "res" / "drawable-nodpi")
    dst.mkdir(parents=True, exist_ok=True)

    problems, rows = [], []
    if SCRIM_END >= 1.0:
        problems.append("SCRIM_END must be < 1.0 or the scrim never reaches the page colour")

    def load(name):
        return Image.open(src / name).convert("RGB").resize((OUT_W, OUT_H), Image.LANCZOS)

    produced = {}
    for name, stem in REAL.items():
        if not (src / name).is_file():
            problems.append(f"{name}: missing")
            continue
        produced[stem] = (load(name), "generated")

    for stem, openness in DERIVED.items():
        own = DERIVED_FILES[stem]
        if (src / own).is_file():
            # A real render turned up for this stage. Always prefer it.
            produced[stem] = (load(own), "generated")
            continue
        if "habitat_door" not in produced or "habitat_unwalled" not in produced:
            problems.append(f"{stem}: cannot be derived without both endpoints")
            continue
        door = produced["habitat_door"][0]
        unwalled = produced["habitat_unwalled"][0]
        t = (openness - LO_OPENNESS) / (HI_OPENNESS - LO_OPENNESS)
        produced[stem] = (
            Image.composite(unwalled, door, dissolve_mask(door.size, t)),
            "derived",
        )

    for stem, (im, origin) in sorted(produced.items()):
        out = dst / f"{stem}.webp"
        im.save(out, "WEBP", quality=QUALITY, method=6)

        seam = worst_seam(scrimmed(im))
        rows.append(
            f"  {stem:18} {origin:9} {out.stat().st_size // 1024:>4} KB  "
            f"seam off the page colour by {seam:5.2f}"
        )
        if seam > SEAM_TOLERANCE:
            problems.append(
                f"{stem}: the bottom edge is {seam:.2f} off the page colour "
                f"(tolerance {SEAM_TOLERANCE}) — the hero will end on a visible line"
            )

    print("\n".join(rows))
    if problems:
        print("\n" + "\n".join(problems))
        print(f"\n{len(problems)} problem(s). Deepen the scrim or move the text band.")
        return 1
    derived = sum(1 for _, (_, o) in produced.items() if o == "derived")
    print(f"\n{len(rows)} image(s) written to {dst}. Every one melts into the page at the "
          f"bottom edge, and nothing is drawn on top of any of them.")
    if derived:
        print(f"{derived} of them are DERIVED rather than generated — see the comment on "
              f"DERIVED in this file.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
