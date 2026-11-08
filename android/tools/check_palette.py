#!/usr/bin/env python3
"""WCAG contrast for the app's one palette.

The app is drawn in the room's colours: a cream ground, paper cards, plum outlines and ink,
and for each colour a dark ink (for text and lines) and a light fill (for what is filled in
that colour, with plum text on it). This checks every pairing the design relies on:

  - every text ink on every surface it is set on: the ground, paper, white, a well, and the
    pale wash behind a chosen option or a tip
  - every coloured ink on the ground, paper, white and its own wash, as text
  - plum on every light fill (and cream on plum), as text
  - the plum outline against the ground, paper and every fill, as the edge of a control

Reads the values out of ui/theme/Theme.kt rather than taking a copy, so the check cannot
drift away from what ships.

Run:  python3 android/tools/check_palette.py
"""

import pathlib
import re
import sys

HERE = pathlib.Path(__file__).resolve().parent
THEME = HERE.parent / "app" / "src" / "main" / "java" / "com" / "debubble" / "app" / "ui" / "theme" / "Theme.kt"

TEXT = 4.5      # WCAG AA, body text
EDGE = 3.0      # WCAG AA, non-text boundaries and large text


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


def parse():
    src = THEME.read_text()
    ink_block = src[src.index("object Ink {"):src.index("\n}\n", src.index("object Ink {"))]
    ink = {m.group(1): int(m.group(2)[2:], 16)
           for m in re.finditer(r"val (\w+) = Color\(0x([0-9A-Fa-f]{8})\)", ink_block)}

    def table(name):
        start = src.index(f"private val {name} = mapOf(")
        block = src[start:src.index(")\n\n", start)]
        return {m.group(1): int(m.group(2)[2:], 16)
                for m in re.finditer(r"Ink\.(\w+) to Color\(0x([0-9A-Fa-f]{8})\)", block)}

    return ink, table("Fills"), table("Washes")


def main():
    ink, fills, washes = parse()
    need = ["Void", "Surface", "SurfaceHigh", "Well", "Border", "Primary", "Secondary", "Muted",
            "OnAccent", "Cream"]
    missing = [n for n in need if n not in ink]
    if missing:
        print("Theme.kt is missing", ", ".join(missing))
        return 1

    accents = [n for n in fills if n != "Primary"]
    grounds = {n: ink[n] for n in ("Void", "Surface", "SurfaceHigh")}
    problems, rows = [], []

    def check(what, fg, bg, floor):
        r = ratio(fg, bg)
        if r < floor:
            problems.append(f"{what} is {r:.2f}:1, floor {floor}")
        return r

    for name in ("Primary", "Secondary", "Muted"):
        worst = min(
            [check(f"{name} on {g}", ink[name], v, TEXT) for g, v in grounds.items()]
            + [check(f"{name} on the well", ink[name], ink["Well"], TEXT)]
            + [check(f"{name} on the {a} wash", ink[name], w, TEXT) for a, w in washes.items()]
        )
        rows.append(f"  text   {name:11} worst {worst:5.2f}:1")

    for name in accents:
        if name not in ink:
            problems.append(f"{name} has a fill but no ink")
            continue
        worst = min(
            [check(f"{name} on {g}", ink[name], v, TEXT) for g, v in grounds.items()]
            + [check(f"{name} on its own wash", ink[name], washes[name], TEXT)]
        )
        well = check(f"{name} on the well", ink[name], ink["Well"], EDGE)
        on_fill = check(f"plum on the {name} fill", ink["OnAccent"], fills[name], TEXT)
        edge = check(f"the outline on the {name} fill", ink["Border"], fills[name], EDGE)
        rows.append(
            f"  colour {name:11} text {worst:5.2f}:1  well {well:5.2f}:1  "
            f"plum on fill {on_fill:5.2f}:1  outline {edge:5.2f}:1"
        )
        if name not in washes:
            problems.append(f"{name} has a fill but no wash")

    rows.append(f"  plum   cream on plum {check('cream on plum', ink['Cream'], fills['Primary'], TEXT):5.2f}:1")
    for g, v in list(grounds.items()) + [("Well", ink["Well"])]:
        check(f"the outline on {g}", ink["Border"], v, EDGE)

    print("\n".join(rows))
    if problems:
        print("\nPalette problems:")
        for p in problems:
            print("  " + p)
        return 1
    print(f"\n{len(accents)} colours, 3 text inks: every pairing clears its floor.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
