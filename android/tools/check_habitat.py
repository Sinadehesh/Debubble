#!/usr/bin/env python3
"""WCAG contrast for every habitat stage.

The room opens by changing the colour under everything, which is exactly the kind of change
that makes a sentence unreadable on one stage out of seven and is never noticed, because
whoever reviewed the design was looking at stage one. So every ink that is ever drawn on a
habitat ground is checked against every stage's ground, horizon and structure.

Reads the stage colours out of engine/Habitat.kt rather than taking a copy, so the check
cannot silently drift away from what ships.

Run:  python3 android/tools/check_habitat.py
"""

import pathlib
import re
import sys

HERE = pathlib.Path(__file__).resolve().parent
SRC = HERE.parent / "app" / "src" / "main" / "java" / "com" / "debubble" / "app"

# Text and structure inks, from ui/theme/Theme.kt. Each entry is (name, hex, floor).
# 4.5 is the WCAG AA floor for body text, 3.0 for large text and non-text boundaries.
INKS = [
    ("Primary", 0xF5F7FA, 4.5),
    ("Secondary", 0xB4BECD, 4.5),
    ("Muted", 0x8593A6, 4.5),
    ("Border", 0x6B7789, 3.0),
    ("Access", 0x4DA3FF, 4.5),
    ("Activity", 0x3DDC97, 4.5),
    ("Social", 0xFF6B81, 4.5),
    ("Ember", 0xFFB020, 4.5),
    ("Gold", 0xFFC94D, 4.5),
]

# Surfaces that sit on top of a habitat ground and carry their own fill. Unchanged by the
# habitat, so they are checked once — but they are checked, because a panel whose fill is
# lighter than the ground behind it loses its edge.
SURFACES = [("Surface", 0x181D26), ("SurfaceHigh", 0x232B38), ("Well", 0x11151C)]


def channel(c):
    s = c / 255.0
    return s / 12.92 if s <= 0.03928 else ((s + 0.055) / 1.055) ** 2.4


def luminance(rgb):
    r, g, b = (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF
    return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)


def ratio(a, b):
    la, lb = luminance(a), luminance(b)
    hi, lo = max(la, lb), min(la, lb)
    return (hi + 0.05) / (lo + 0.05)


def stages():
    """Pull (label, ground, horizon, structure) out of the Kotlin enum."""
    src = (SRC / "engine" / "Habitat.kt").read_text()
    out = []
    for block in re.finditer(
        r"label\s*=\s*\"([^\"]+)\".*?ground\s*=\s*0x(FF[0-9A-Fa-f]{6}).*?"
        r"horizon\s*=\s*0x(FF[0-9A-Fa-f]{6}).*?structure\s*=\s*0x(FF[0-9A-Fa-f]{6})",
        src,
        re.S,
    ):
        label, ground, horizon, structure = block.groups()
        out.append(
            (label, int(ground[2:], 16), int(horizon[2:], 16), int(structure[2:], 16))
        )
    return out


def main():
    found = stages()
    if len(found) != 7:
        print(f"expected 7 stages in Habitat.kt, parsed {len(found)}")
        return 1

    problems = []
    rows = []
    for label, ground, horizon, structure in found:
        # The gradient runs ground -> horizon, so text has to clear BOTH ends. Checking only
        # the average would pass a gradient whose bright end eats the muted text.
        for bg_name, bg in (("ground", ground), ("horizon", horizon)):
            for ink_name, ink, floor in INKS:
                r = ratio(ink, bg)
                if r < floor:
                    problems.append(
                        f"{label}: {ink_name} on {bg_name} is {r:.2f}:1, floor {floor}"
                    )
        # Structure is drawn only in the top band, behind the ring, where no text is placed —
        # so it is not held to a text floor. It is held to a *visibility* floor instead,
        # because a wall nobody can see is a stage that does not read as a change, and the
        # whole point of the habitat is that progress is visible.
        r = ratio(structure, ground)
        if r < 1.15:
            problems.append(f"{label}: structure is invisible on its own ground ({r:.2f}:1)")
        if label != "Sealed room" and r < 1.25:
            problems.append(f"{label}: structure is too faint to read as a change ({r:.2f}:1)")

        worst = min(ratio(i, b) for _, i, _ in INKS for b in (ground, horizon))
        rows.append(
            f"  {label:13} ground {ground:06X}  worst ink {worst:5.2f}:1"
            f"  structure {ratio(structure, ground):.2f}:1"
        )

    # Panels keep their own fill, so their edge against the lightest habitat ground is what
    # decides whether a card still reads as a card at stage seven.
    lightest = max(found, key=lambda s: luminance(s[2]))
    for name, fill in SURFACES:
        r = ratio(0x6B7789, fill)
        if r < 3.0:
            problems.append(f"{name}: Border on it is {r:.2f}:1, floor 3.0")
        rows.append(f"  {name:13} border {r:5.2f}:1")
    rows.append(
        f"  lightest ground is {lightest[0]} ({lightest[2]:06X}); "
        f"Surface against it: {ratio(0x181D26, lightest[2]):.2f}:1"
    )

    print("\n".join(rows))
    if problems:
        print("\n" + "\n".join(problems))
        print(f"\n{len(problems)} contrast problem(s).")
        return 1
    print(f"\n{len(found)} stages, all inks clear their floor on every ground.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
