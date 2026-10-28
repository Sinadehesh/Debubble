#!/usr/bin/env python3
"""Build the interactive web preview of the room, with the shipped assets embedded.

Reads exactly what the app reads — the seven room shells, the sprite atlas and its index,
the care routines, the stage thresholds in Habitat.kt and the first rungs of each mission
ladder — and inlines them, so the page cannot drift from what Android draws. Its script is a
transcription of engine/Care.kt, engine/Home.kt and engine/Today.kt (same rules, same draw
order, same SplitMix seed hashing, same look swap), which tools/check_care.py also
transcribes and tests. It also mocks the home-screen widget from the same state.

Run:  python3 android/tools/pixel/make_page.py
"""

import base64
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
MAIN = ROOT / "app" / "src" / "main"
DRAWABLE = MAIN / "res" / "drawable-nodpi"
ASSETS = MAIN / "assets"
OUT = pathlib.Path(__file__).resolve().parent.parent / "preview" / "room.html"
TEMPLATE = pathlib.Path(__file__).resolve().parent / "page_template.html"

PILLARS = {"ACCESS": "access", "ACTIVITY": "activity", "SOCIAL": "social"}
MISSION_SAMPLE = 30


def b64(path):
    return "data:image/webp;base64," + base64.b64encode(path.read_bytes()).decode()


def main():
    manifest = json.loads((ASSETS / "pixel" / "items.json").read_text())
    manifest.pop("palette", None)
    lib = json.loads((ASSETS / "care" / "routines.json").read_text())

    hab = (MAIN / "java" / "com" / "debubble" / "app" / "engine" / "Habitat.kt").read_text()
    found = re.findall(
        r"threshold\s*=\s*(\d+),\s*label\s*=\s*\"([^\"]+)\",\s*note\s*=\s*\"([^\"]+)\"", hab, re.S)
    if len(found) != len(manifest["stages"]):
        print(f"Habitat.kt has {len(found)} stages, assets have {len(manifest['stages'])}")
        return 1

    missions = {}
    for key, name in PILLARS.items():
        ladder = json.loads((ASSETS / "curriculum" / f"{name}.json").read_text())
        missions[key] = [{k: t.get(k, "") for k in ("t", "do", "why", "when", "open", "done", "stop", "min")}
                         for t in ladder["tiers"][:MISSION_SAMPLE]]

    data = {
        "manifest": manifest,
        "atlas": b64(ASSETS / "pixel" / "items.webp"),
        "stages": [
            {"key": key, "threshold": int(th), "label": label, "note": note,
             "img": b64(DRAWABLE / f"room_{key}.webp")}
            for (th, label, note), key in zip(found, manifest["stages"])
        ],
        "lib": lib,
        "missions": missions,
    }
    page = TEMPLATE.read_text().replace("__DATA__", json.dumps(data, separators=(",", ":")))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(page)
    print(f"wrote {OUT.relative_to(ROOT.parent)} ({OUT.stat().st_size // 1024} KB): "
          f"{len(lib['routines'])} routines, {len(manifest['items'])} items, "
          f"{len(data['stages'])} rooms, {MISSION_SAMPLE} missions per ladder")
    return 0


if __name__ == "__main__":
    sys.exit(main())
