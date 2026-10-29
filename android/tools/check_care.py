#!/usr/bin/env python3
"""A line-for-line transcription of engine/Care.kt and engine/Home.kt, run against the real
assets, asserting what CareTest asserts.

CI is the only thing that compiles Kotlin here, so this is how the rules get exercised
locally before a push. Kept deliberately literal — same names, same order, same integer
arithmetic, 64-bit wraparound emulated for the hash — so a divergence between the two is
a bug in one of them, not a translation choice.

Run:  python3 android/tools/check_care.py
"""

import json
import math
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1] / "app" / "src" / "main"
LIB = json.loads((ROOT / "assets" / "care" / "routines.json").read_text())
CAT = json.loads((ROOT / "assets" / "pixel" / "items.json").read_text())

# ---------------------------------------------------------------- Care.kt
DAYS_PER_LEVEL = {"BODY": 1, "ROOM": 2, "HEART": 2}
MAX_DIRT = 3
START_DIRT = 1
REWARD_AT = [1, 3, 6, 10, 15, 21, 30, 45, 66]
AUTOMATICITY_DAYS = 66
TINY_FIRST_REPS = 7
COMING_BACK_DAYS = 3
FAMILY = {"BODY": "BODY", "ROOM": "ROOM", "HEART": "HEART"}


def state(**kw):
    s = dict(dirt=START_DIRT, setDay=0, reps=0, lastDoneDay=0, routine="", anchor="",
             better=0, same=0, worse=0)
    s.update(kw)
    return s


def dirt(d, s, today):
    base = max(0, min(MAX_DIRT, s["dirt"]))
    if s["setDay"] <= 0:
        return base
    days = max(0, today - s["setDay"])
    slipped = min(days // DAYS_PER_LEVEL[d], MAX_DIRT)
    return min(MAX_DIRT, base + slipped)


def after_care(d, s, size, today):
    now = dirt(d, s, today)
    cleaned = max(0, now - 1) if size == "TINY" else 0
    counts = s["lastDoneDay"] != today
    return {**s, "dirt": cleaned, "setDay": today, "reps": s["reps"] + (1 if counts else 0),
            "lastDoneDay": today}


def check_in(s, level, today):
    return {**s, "dirt": max(0, min(MAX_DIRT, level)), "setDay": today}


def suggested(s, today):
    if s["reps"] < TINY_FIRST_REPS:
        return "TINY"
    if s["lastDoneDay"] > 0 and today - s["lastDoneDay"] >= COMING_BACK_DAYS:
        return "TINY"
    return "STANDARD"


def owed(count):
    return sum(1 for r in REWARD_AT if r <= count)


def next_reward_in(count):
    return next((r - count for r in REWARD_AT if r > count), None)


def automaticity(reps):
    if reps <= 0:
        return 0.0
    k = math.log(20.0) / AUTOMATICITY_DAYS
    return min(1.0, max(0.0, 1.0 - math.exp(-k * reps)))


# ---------------------------------------------------------------- Home.kt
M64 = (1 << 64) - 1


def s64(x):
    x &= M64
    return x - (1 << 64) if x >> 63 else x


def mix(seed, n):
    z = s64(seed + n * -0x61c8864680b583eb)
    z = s64((z ^ ((z & M64) >> 30)) * -0x40a7b892e31b1a47)
    z = s64((z ^ ((z & M64) >> 27)) * -0x6b2fb644ecceee15)
    return s64(z ^ ((z & M64) >> 31))


def floor_mod(a, b):
    return a - b * math.floor(a / b) if abs(a) < 2 ** 52 else a % b   # Python % is floorMod


def family(f):
    return [i for i in CAT["items"] if i["family"] == f]


def pick(f, owned, seed):
    left = [i for i in family(f) if i["id"] not in owned]
    if not left:
        return None
    for i in left:
        if i["first"]:
            return i
    return left[mix(seed, len(owned)) % len(left)]


def owned_in(home, f):
    ids = {i["id"]: i for i in CAT["items"]}
    return sum(1 for o in home["owned"] if ids.get(o, {}).get("family") == f)


def settle(home, f, count, seed):
    owe = owed(count) - owned_in(home, f)
    h = {**home, "owned": list(home["owned"]), "placed": dict(home["placed"]),
         "unseen": set(home["unseen"])}
    granted = []
    for _ in range(max(0, owe)):
        nxt = pick(f, h["owned"], seed)
        if nxt is None:
            break
        h["owned"].append(nxt["id"])
        h["placed"][nxt["id"]] = (nxt["spot"]["x"], nxt["spot"]["b"])
        h["unseen"].add(nxt["id"])
        granted.append(nxt)
    return h, granted


def clamp(w, h, on_wall, x, b):
    r = CAT["room"]
    min_b = h + 2 if on_wall else r["floorY"] + 2
    max_b = r["floorY"] - r["skirt"] if on_wall else r["h"]
    return (max(0, min(max(0, r["w"] - w), x)), max(min_b, min(max(max_b, min_b), b)))


def empty_home():
    return {"owned": [], "placed": {}, "me": None, "unseen": set()}


# ---------------------------------------------------------------- Scene (Home.kt)
ME = CAT["me"]
FXD = {f["id"]: f for f in CAT["fx"]}


def buddy(heart):
    return f"buddy{max(0, min(MAX_DIRT, heart))}"


def sides(me):
    bw, lw = FXD["buddy0"]["w"], FXD["letter"]["w"]
    right = me[0] + ME["buddyDx"] + bw <= CAT["room"]["w"]
    bx = me[0] + ME["buddyDx"] if right else me[0] + ME["w"] - ME["buddyDx"] - bw
    lx = me[0] + ME["letterDx"] if right else me[0] + ME["w"] - ME["letterDx"] - lw
    return bx, max(0, min(max(0, CAT["room"]["w"] - lw), lx))


def draws(stage, home, dirt, frame=0, moving=None, burst=False, letter=False):
    body, room, heart = dirt
    walls = stage != "unwalled"
    items = {i["id"]: i for i in CAT["items"]}

    def spot_of(i, default):
        return moving[1] if moving and moving[0] == i else default

    placed = [(items[i], spot_of(i, home["placed"][i])) for i in home["owned"]
              if i in items and i in home["placed"]]
    out = []
    if walls:
        out += [(d["id"], s[0], s[1]) for d, s in placed if d["anchor"] == "wall"]
        out += [(m["fx"], m["x"], m["b"]) for m in CAT["mess"] if m["fx"] == "cobweb" and room >= m["level"]]
    out += [(d["id"], s[0], s[1]) for d, s in placed if d["anchor"] != "wall" and d["flat"]]
    standing = [(s[1], 0, (d["id"], s[0], s[1])) for d, s in placed if d["anchor"] != "wall" and not d["flat"]]
    standing += [(m["b"], 1, (m["fx"], m["x"], m["b"])) for m in CAT["mess"]
                 if m["fx"] != "cobweb" and room >= m["level"]]
    me = spot_of("me", home.get("me") or (ME["x"], ME["b"]))
    bob = 1 if frame % 2 == 1 else 0
    standing.append((me[1], 2, (f"me{body}", me[0], me[1] - bob)))
    hat = home.get("hat", 0)
    if 0 < hat <= len(CAT["hats"]):
        hid = CAT["hats"][hat - 1]["id"]
        standing.append((me[1], 3, (hid, me[0], me[1] - ME["h"] + FXD[hid]["h"] - bob)))
    out += [t[2] for t in sorted(standing, key=lambda t: (t[0], t[1]))]
    bx, lx = sides(me)
    if letter:
        lb = me[1] - ME["letterDy"] - bob
        out.append(("letter", lx, lb))
        out.append(("bang", lx + (FXD["letter"]["w"] - FXD["bang"]["w"]) // 2,
                    lb - FXD["letter"]["h"] - ME["bangGap"]))
    out.append((buddy(heart), bx, me[1] - ME["buddyDy"] - (1 - bob)))
    if room == 0:
        out += [("sparkle", p["x"], p["y"]) for i, p in enumerate(CAT["sparkles"]) if (i + frame) % 2 == 0]
    if body == 0:
        out.append(("sparkle", me[0] + 2, me[1] - ME["h"] + 6 - bob))
    return out


def size(i):
    items = {it["id"]: it for it in CAT["items"]}
    d = items.get(i) or FXD.get(i)
    return (d["w"], d["h"]) if d else None


def under(d, x, y):
    wh = size(d[0])
    return bool(wh) and d[1] <= x < d[1] + wh[0] and d[2] - wh[1] <= y < d[2]


def hit(stage, home, dirt, x, y):
    items = {i["id"] for i in CAT["items"]}
    me_ids = {f"me{i}" for i in range(MAX_DIRT + 1)}
    for d in reversed(draws(stage, home, dirt)):
        if d[0] not in me_ids and d[0] not in items:
            continue
        if under(d, x, y):
            return "me" if d[0] in me_ids else d[0]
    return None


def tap(stage, home, dirt, letter, x, y):
    mess = {m["fx"] for m in CAT["mess"]}
    for d in reversed(draws(stage, home, dirt, letter=letter)):
        kind = ("HEART" if d[0].startswith("buddy") else
                "OUTSIDE" if d[0] in ("letter", "bang") else
                "BODY" if d[0] in {f"me{i}" for i in range(MAX_DIRT + 1)} else
                "ROOM" if d[0] in mess else None)
        if kind and under(d, x, y):
            return kind
    return None


def kind_of(i):
    mess = {m["fx"] for m in CAT["mess"]}
    return ("HEART" if i in {f"buddy{n}" for n in range(MAX_DIRT + 1)} else
            "OUTSIDE" if i in ("letter", "bang") else
            "BODY" if i in {f"me{n}" for n in range(MAX_DIRT + 1)} else
            "ROOM" if i in mess else None)


def spots(stage, home, dirt, letter):
    """Mirror of Scene.spots: one area per task, whose middle a tap resolves to that task."""
    drawn = draws(stage, home, dirt, letter=letter)
    out = {}
    for kind in ("BODY", "ROOM", "HEART", "OUTSIDE"):
        areas = []
        for d in drawn:
            wh = size(d[0])
            if kind_of(d[0]) == kind and wh:
                areas.append((d[1], d[2] - wh[1], wh[0], wh[1]))
        areas.sort(key=lambda a: -a[2] * a[3])
        pick = next((a for a in areas
                     if tap(stage, home, dirt, letter, a[0] + a[2] // 2, a[1] + a[3] // 2) == kind),
                    areas[0] if areas else None)
        if pick:
            out[kind] = pick
    return out


def check_hats(ok):
    """Every hat sits on your head, in your frame, and never gets in the way of a tap."""
    ok(len(CAT["hats"]) >= 1, "hats exported")
    levels = [h["level"] for h in CAT["hats"]]
    ok(levels == sorted(levels) and levels[0] > 1, f"hats unlock in level order, none at 1: {levels}")
    for n, h in enumerate(CAT["hats"], start=1):
        f = FXD[h["id"]]
        ok(f["w"] == ME["w"] and f["h"] < ME["h"], f"{h['id']} is drawn in your frame")
        home = dict(home_with("bed"), hat=n)
        dr = draws("window", home, (1, 1, 1))
        ids = [d[0] for d in dr]
        ok(ids.index(h["id"]) == ids.index("me1") + 1, f"{h['id']} straight after you")
        top = next(d for d in dr if d[0] == "me1")
        hat = next(d for d in dr if d[0] == h["id"])
        ok(hat[2] - f["h"] == top[2] - ME["h"], f"{h['id']} top aligned with yours")
        me = (ME["x"], ME["b"])
        ok(tap("window", home, (1, 1, 1), False, me[0] + ME["w"] // 2, me[1] - ME["h"] + 3) == "BODY",
           f"tapping {h['id']} is tapping you")


def check_spots(ok):
    """Every spot the tour lights up opens what the tour says it does, in every state."""
    homes = [home_with(), home_with("rug", "cat", "bed", "sneakers"), home_with("bed", "lamp", "plant")]
    for stage in ("sealed", "window", "unwalled"):
        for h in homes:
            for dd in [(b, r, he) for b in range(4) for r in range(4) for he in range(4)]:
                for letter in (False, True):
                    sp = spots(stage, h, dd, letter)
                    ok({"BODY", "HEART"} <= set(sp), f"you and your heart always there {stage} {dd}")
                    ok(("ROOM" in sp) == (dd[1] > 0), f"mess spot iff mess {stage} {dd}")
                    ok(("OUTSIDE" in sp) == letter, f"letter spot iff letter {stage} {dd}")
                    for kind, (x, y, w, hh) in sp.items():
                        ok(0 <= x and x + w <= CAT["room"]["w"] and 0 <= y and y + hh <= CAT["room"]["h"],
                           f"{kind} spot inside the room {stage} {dd}")
                        ok(tap(stage, h, dd, letter, x + w // 2, y + hh // 2) == kind,
                           f"tap the middle of {kind} {stage} {dd} letter={letter}")


def argb(h):
    return 0xFF000000 | int(h.lstrip("#"), 16)


def swaps(look):
    out = {}
    for key, pick in zip(("skin", "hair", "top"), look):
        ramps = CAT["looks"][key]
        to = ramps[max(0, min(len(ramps) - 1, pick))]
        for a, b in zip(ramps[0], to):
            if argb(a) != argb(b):
                out[argb(a)] = argb(b)
    return out


def home_with(*ids):
    items = {i["id"]: i for i in CAT["items"]}
    return {"owned": list(ids), "placed": {i: (items[i]["spot"]["x"], items[i]["spot"]["b"]) for i in ids},
            "me": None, "unseen": set()}


def check_scene(ok):
    order = [d[0] for d in draws("window", home_with("rug", "cat", "bed", "sneakers"), (1, 1, 1))]
    ok(order.index("rug") < order.index("cat"), "rug over cat")
    ok(order.index("bed") < order.index("sneakers"), "bed over trainers")
    mess = {m["fx"] for m in CAT["mess"]}
    counts = [sum(1 for d in draws("window", home_with(), (1, r, 1)) if d[0] in mess) for r in range(4)]
    ok(counts[0] == 0 and counts == sorted(counts) and counts[3] > counts[1], f"mess counts {counts}")
    ok(any(d[0] == "sparkle" for d in draws("window", home_with(), (1, 0, 1))), "clean sparkles")
    ok(not any(d[0] == "sparkle" for d in draws("window", home_with(), (1, 1, 1))), "lived-in sparkles")
    for b in range(4):
        dr = draws("sealed", home_with(), (b, 1, b))
        ok(sum(1 for d in dr if d[0] == f"me{b}") == 1, "me frame")
        ok(sum(1 for d in dr if d[0] == f"buddy{b}") == 1, "buddy frame")
        ok((FXD[f"me{b}"]["w"], FXD[f"me{b}"]["h"]) == (ME["w"], ME["h"]), "me size")
    ok(not any(d[0] in ("letter", "bang") for d in draws("sealed", home_with(), (1, 1, 1))), "no letter")
    lt = [d[0] for d in draws("sealed", home_with(), (1, 1, 1), letter=True)]
    ok(lt.index("letter") > lt.index("me1") and "bang" in lt, "letter in front of you")
    for x in (0, 40, 70, CAT["room"]["w"] - ME["w"]):
        bx, lx = sides((x, 96))
        ok(0 <= bx and bx + FXD["buddy0"]["w"] <= CAT["room"]["w"], f"buddy in room at {x}")
        ok(0 <= lx and lx + FXD["letter"]["w"] <= CAT["room"]["w"], f"letter in room at {x}")
        ok((bx > x) != (lx > x), f"buddy and letter same side at {x}")
    me = (ME["x"], ME["b"])
    bx, lx = sides(me)
    dd = (1, 3, 1)
    ok(tap("sealed", home_with(), dd, True, me[0] + ME["w"] // 2, me[1] - 4) == "BODY", "tap body")
    bh = FXD["buddy1"]
    ok(tap("sealed", home_with(), dd, True, bx + bh["w"] // 2, me[1] - ME["buddyDy"] - bh["h"] // 2 - 1) == "HEART",
       "tap heart")
    ok(tap("sealed", home_with(), dd, True, lx + 1, me[1] - ME["letterDy"] - 2) == "OUTSIDE", "tap letter")
    sock = next(m for m in CAT["mess"] if m["fx"] == "sock")
    ok(tap("sealed", home_with(), dd, True, sock["x"] + 2, sock["b"] - 2) == "ROOM", "tap mess")
    ok(tap("sealed", home_with(), dd, True, CAT["room"]["w"] - 2, 2) is None, "tap nothing")
    ok(tap("sealed", home_with(), dd, False, lx + 1, me[1] - ME["letterDy"] - 2) != "OUTSIDE", "tap no letter")
    ok(swaps((0, 0, 0)) == {}, "first look is the atlas")
    L = CAT["looks"]
    other = (len(L["skin"]) - 1, len(L["hair"]) - 1, len(L["top"]) - 1)
    own = {argb(c) for c in L["skin"][0] + L["hair"][0] + L["top"][0]}
    ok(swaps(other) and set(swaps(other)) <= own, "look swaps only the character")
    ok(swaps((99, 99, 99)) == swaps(other), "look clamps")
    h = home_with("mirror", "bed")
    ok(any(d[0] == "mirror" for d in draws("window", h, (1, 1, 1))), "mirror on wall")
    ok(not any(d[0] == "mirror" for d in draws("unwalled", h, (1, 1, 1))), "mirror without walls")
    hb = home_with("bed")
    bed = next(i for i in CAT["items"] if i["id"] == "bed")
    ok(hit("window", hb, (1, 0, 1), bed["spot"]["x"] + 5, bed["spot"]["b"] - 3) == "bed", "hit bed")
    ok(hit("window", hb, (1, 0, 1), CAT["me"]["x"] + 10, CAT["me"]["b"] - 8) == "me", "hit me")
    ok(hit("window", hb, (1, 0, 1), 158, 2) is None, "hit nothing")
    return counts


# ---------------------------------------------------------------- the assertions
def main():
    fails = []

    def ok(cond, msg):
        if not cond:
            fails.append(msg)

    day = 20000
    r = LIB["rules"]
    ok(r["maxDirt"] == MAX_DIRT and r["rewardAt"] == REWARD_AT and
       r["automaticityDays"] == AUTOMATICITY_DAYS and r["daysPerLevel"] == DAYS_PER_LEVEL,
       "asset rules differ from the engine constants")
    ok(CAT["maxDirt"] == MAX_DIRT, "items.json maxDirt differs")

    s = state()
    ok(dirt("BODY", s, day) == START_DIRT and dirt("BODY", s, day + 400) == START_DIRT,
       "the room moves before moving in")
    clean = check_in(state(), 0, day)
    ok(dirt("BODY", clean, day + 1) == 1, "body does not slip daily")
    ok(dirt("ROOM", clean, day + 1) == 0 and dirt("ROOM", clean, day + 2) == 1,
       "room does not slip every other day")
    ok(dirt("HEART", clean, day + 4) == 2, "heart slip")
    ok(all(dirt(d, clean, day + 10000) == MAX_DIRT for d in DAYS_PER_LEVEL), "mess not capped")

    messy = check_in(state(), 3, day)
    ok(dirt("ROOM", after_care("ROOM", messy, "TINY", day), day) == 2, "tiny should lift one")
    ok(dirt("ROOM", after_care("ROOM", messy, "STANDARD", day), day) == 0, "standard cleans")
    ok(dirt("ROOM", after_care("ROOM", clean, "TINY", day), day) == 0, "tiny below zero")

    t = check_in(state(), 2, day)
    t = after_care("HEART", t, "TINY", day)
    t2 = after_care("HEART", t, "DEEP", day)
    ok(t["reps"] == 1 and t2["reps"] == 1 and dirt("HEART", t2, day) == 0,
       "second session same day must clean but not count")
    ok(after_care("HEART", t2, "TINY", day + 1)["reps"] == 2, "next day counts")

    honest = check_in(clean, 1, day + 5)
    ok(dirt("BODY", honest, day + 5) == 1 and dirt("BODY", honest, day + 6) == 2,
       "check-in should restart the slip")

    ok(suggested(state(), day) == "TINY", "first week tiny")
    settled = state(reps=TINY_FIRST_REPS, lastDoneDay=day - 1)
    ok(suggested(settled, day) == "STANDARD", "settled standard")
    ok(suggested({**settled, "lastDoneDay": day - COMING_BACK_DAYS}, day) == "TINY", "gap tiny")

    ok(owed(0) == 0 and owed(1) == 1 and owed(3) == 2 and owed(1000) == len(REWARD_AT), "owed")
    ok(next_reward_in(1) == 2 and next_reward_in(66) is None, "next reward")
    gaps = [b - a for a, b in zip(REWARD_AT, REWARD_AT[1:])]
    ok(gaps == sorted(gaps), "reward gaps shrink somewhere")

    for f in CAT["families"]:
        ok(len(family(f)) == len(REWARD_AT), f"{f}: {len(family(f))} items for {len(REWARD_AT)} rewards")
        ok(sum(1 for i in family(f) if i["first"]) == 1, f"{f}: first")
        first = pick(f, [], 42)
        ok(first is not None and first["first"], f"{f}: first item not first")
        home, granted = settle(empty_home(), f, 66, 42)
        ok({g["id"] for g in granted} == {i["id"] for i in family(f)}, f"{f}: not all granted")
        ok(len(home["owned"]) == len(set(home["owned"])), f"{f}: duplicate grant")
        ok(pick(f, home["owned"], 42) is None, f"{f}: pick after complete")

    once, first = settle(empty_home(), "ROOM", 6, 7)
    twice, again = settle(once, "ROOM", 6, 7)
    ok(len(first) == 3 and not again and once["owned"] == twice["owned"], "settle not idempotent")
    mixed, _ = settle(once, "BODY", 1, 7)
    ok(owned_in(mixed, "ROOM") == 3 and owned_in(mixed, "BODY") == 1, "families leak")

    home, granted = settle(empty_home(), "WORLD", 40, 3)
    ok(len(granted) == owed(40), "upgrader not paid in full")
    ok(all(home["placed"][g["id"]] == (g["spot"]["x"], g["spot"]["b"]) for g in granted), "spots")

    a = [g["id"] for g in settle(empty_home(), "HEART", 66, 1)[1]]
    b = [g["id"] for g in settle(empty_home(), "HEART", 66, 999999)[1]]
    ok(a[0] == b[0] and a != b, "surprise order should depend on the seed, after the first")

    for i in CAT["items"]:
        c = clamp(i["w"], i["h"], i["anchor"] == "wall", i["spot"]["x"], i["spot"]["b"])
        ok(c == (i["spot"]["x"], i["spot"]["b"]), f"{i['id']}: default spot {i['spot']} clamps to {c}")
    room = CAT["room"]
    ok(clamp(24, 10, False, 50, 3)[1] == room["floorY"] + 2, "floor clamp")
    ok(clamp(12, 12, True, 50, 99)[1] == room["floorY"] - room["skirt"], "wall clamp")
    ok(clamp(20, 10, False, 500, 80)[0] == room["w"] - 20, "edge clamp")

    ok(abs(automaticity(AUTOMATICITY_DAYS) - 0.95) < 0.001, "automaticity at 66")
    vals = [automaticity(n) for n in range(0, 201)]
    ok(all(b > a for a, b in zip(vals, vals[1:])) and vals[0] == 0, "automaticity monotone")

    counts = check_scene(ok)
    check_spots(ok)
    check_hats(ok)

    if fails:
        for f in fails:
            print("FAIL", f)
        return 1
    print(f"care engine: ok ({len(LIB['routines'])} routines, {len(CAT['items'])} items, "
          f"automaticity at 10 / 30 / 66 days = "
          f"{automaticity(10):.0%} / {automaticity(30):.0%} / {automaticity(66):.0%}; "
          f"mess pieces by Room level {counts})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
