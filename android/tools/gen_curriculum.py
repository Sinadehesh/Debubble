#!/usr/bin/env python3
"""Fold the protocol into the three ladders, and refuse to write anything that fails the checks.

The directives themselves were already decent prose. What was missing was everything around
them: when to start, what the first move is, how you know you are finished, which safety
behaviour comes off, and what you predicted beforehand so you can find out you were wrong.
See engine/Protocol.kt for the sources each of those fields comes from.

A handful of directives are rewritten here rather than scaffolded, because they were not
checkable in any wording — "until you genuinely cannot continue" has no end state, and at the
top of the Access ladder the instruction was a budget problem wearing a psychology costume.

Run:  python3 android/tools/gen_curriculum.py
"""

import json
import pathlib
import re
import sys

HERE = pathlib.Path(__file__).resolve().parent
ASSETS = HERE.parent / "app" / "src" / "main" / "assets" / "curriculum"
sys.path.insert(0, str(HERE))

from protocol_data import ANCHORS, DROPS          # noqa: E402
import protocol_access, protocol_activity, protocol_social  # noqa: E402

LADDERS = {
    "access": protocol_access.ROWS,
    "activity": protocol_activity.ROWS,
    "social": protocol_social.ROWS,
}

# ------------------------------------------------------------------ directive rewrites
#
# Keyed by (ladder, tier) -> {field: new text}. Deliberately a short, explicit list: a
# generator that quietly rewrites authored prose is a generator nobody can review.
REWRITES = {
    ("activity", 25): {
        "do": "Do one physical movement until your form breaks, then stop.",
        "why": "Form failure is a real, visible limit and a safe one. "
               "Exhaustion is neither, which is why it is not the instruction.",
        "alt": "Push one exercise to the point the technique goes, and stop there.",
    },
    ("activity", 12): {
        "do": "Move your body for twenty minutes in a way it does not know.",
        "why": "Your body has a small vocabulary. Twenty minutes is one new word.",
    },
    ("access", 70): {
        "do": "Get a thousand kilometres from home — and start by finding out what that "
              "actually costs.",
        "why": "This one is usually refused on money before it is refused on nerve. "
               "Price it first. A number you have looked at is a decision; a number you "
               "have not is an excuse.",
    },
    ("access", 40): {
        "do": "Go somewhere solely because you cannot picture it.",
        "why": "Unimaginable is the only selection criterion from here on. "
               "Do not look it up before you go.",
    },
}

# ------------------------------------------------------------------ checks
#
# Words Copy.adapt does not rewrite and therefore must never appear: a wheelchair user
# served "step out of your door" is exactly the bug the mobility work was meant to end.
# walk / walked / walking / stand are all handled by the rewriter, so they are allowed.
GAIT = re.compile(r"\b(steps?|stepped|stepping|feet|legs?|stride|strides|strolls?)\b", re.I)
FOOT = re.compile(r"\bfoot\b", re.I)
VAGUE = ["when i can", "when i get a chance", "sometime", "soon", "later",
         "if i feel", "when i feel", "eventually", "at some point", "when ready"]

# "Done when <done>" and "You expect <test>" both have to read as one sentence.
CLAUSE_OK = re.compile(r"^[a-z0-9]")
OPENER_MAX = 130

# ------------------------------------------------- Copy.adapt, transcribed, and what it breaks
#
# The rewriter is a regex pass, so it turns "you have walked it end to end" into "you have been
# it end to end" for a user who picked neither walking nor wheels. That is not a bug in the
# rewriter — "head" has no usable past tense in these sentences, which Mobility.kt says
# outright — it is a bug in the sentence, and it is only visible after the substitution. So the
# generator performs the substitution itself and refuses copy that comes out as non-English.
_NOUN = [(r"\bon foot\b", "under your own power"),
         (r"\bOn foot\b", "Under your own power"),
         (r"\bhour's walk\b", "hour's travel"),
         (r"\b(\w+)-minute walk\b", r"\1-minute trip"),
         (r"\ba walk\b", "a trip"), (r"\bA walk\b", "A trip"),
         (r"\bthe walk\b", "the trip"), (r"\bThe walk\b", "The trip")]
_POSTURE = [(r"\bStand\b", "Stay"), (r"\bstand\b", "stay")]

# Constructions that only appear after a substitution and are always wrong when they do.
BROKEN = re.compile(
    r"\bbeen (a|an|the|it|back|for|outside)\b"      # "you have been it end to end"
    r"|\bbe been\b|\bbeen been\b"
    r"|\b(?:stay|stays|staying) out\b"              # "stand out" -> "stay out"
    r"|\bHead until\b|\bRoll until\b"
    r"|\bminutes of going\b|\bminutes of rolling have\b",
    re.I,
)


def adapt(text, verb, past, progressive):
    """Transcription of Copy.adapt. Must stay in step with engine/Mobility.kt."""
    out = text
    for pattern, to in _NOUN + _POSTURE:
        out = re.sub(pattern, to, out)
    out = re.sub(r"\bWalk\b", verb, out)
    out = re.sub(r"\bwalk\b", verb.lower(), out)
    out = re.sub(r"\bwalked\b", past, out)
    out = re.sub(r"\bwalking\b", progressive, out)
    return out


VARIANTS = (("Roll", "rolled", "rolling"), ("Head", "been", "going"))


def specific(cue):
    """Transcription of Cues.isSpecific. The app would reject a cue this fails."""
    c = cue.strip().lower()
    return len(c) >= 8 and not any(v in c for v in VAGUE)


def problems(name, rows):
    out = []
    tiers = [r[0] for r in rows]
    if tiers != list(range(1, 101)):
        out.append(f"{name}: tiers are not 1..100")

    for row in rows:
        tier, anchor, opener, done, test, drop = row[:6]
        say = row[6] if len(row) > 6 else ""
        at = f"{name} t{tier}"

        if anchor not in ANCHORS:
            out.append(f"{at}: unknown anchor '{anchor}'")
        elif not specific(ANCHORS[anchor]):
            out.append(f"{at}: anchor would fail the app's own specificity test")
        if drop not in DROPS:
            out.append(f"{at}: unknown drop '{drop}'")

        for field, text in (("opener", opener), ("done", done), ("test", test)):
            if not text.strip():
                out.append(f"{at}: empty {field}")
            if not text.rstrip().endswith((".", "\"")):
                out.append(f"{at}: {field} does not end a sentence")
            bad = GAIT.findall(text) + [
                m for m in FOOT.findall(text) if "on foot" not in text.lower()
            ]
            if bad:
                out.append(f"{at}: {field} uses gait words the rewriter cannot fix: {bad}")

        # The opener is the two-minute rule made concrete. A long one is a second task.
        if len(opener) > OPENER_MAX:
            out.append(f"{at}: opener is {len(opener)} chars — too long to be two minutes")
        if not opener[:1].isupper():
            out.append(f"{at}: opener should read as an instruction")

        # These two are rendered after a fixed lead-in, so they must start mid-sentence.
        if not CLAUSE_OK.match(done):
            out.append(f"{at}: done must continue 'Done when ...' — got '{done[:30]}'")
        if not CLAUSE_OK.match(test):
            out.append(f"{at}: test must continue 'You expect ...' — got '{test[:30]}'")

        for field, text in (("when", ANCHORS.get(anchor, "")), ("opener", opener),
                            ("done", done), ("test", test), ("stop", DROPS.get(drop, ""))):
            for verb, past, progressive in VARIANTS:
                rewritten = adapt(text, verb, past, progressive)
                hit = BROKEN.search(rewritten)
                # Only what the substitution introduced. "You have been back inside" was
                # authored that way and reads correctly; flagging it would be the checker
                # reporting a problem that is not there.
                if hit and rewritten != text and not BROKEN.search(text):
                    out.append(
                        f"{at}: {field} becomes non-English for a '{verb}' user "
                        f"(\"{hit.group(0)}\"): {rewritten}"
                    )

        if say and '"' not in say:
            out.append(f"{at}: script should contain the actual words, in quotes")
    return out


def main():
    errors = []
    for name, rows in LADDERS.items():
        errors += problems(name, rows)
    if errors:
        print("\n".join(errors))
        print(f"\n{len(errors)} problem(s). Nothing written.")
        return 1

    summary = []
    for name, rows in LADDERS.items():
        path = ASSETS / f"{name}.json"
        data = json.loads(path.read_text())
        by_tier = {r[0]: r for r in rows}
        rewritten = 0
        scripted = 0

        for challenge in data["tiers"]:
            tier = challenge["t"]
            row = by_tier[tier]
            for field, text in REWRITES.get((name, tier), {}).items():
                challenge[field] = text
                rewritten += 1
            challenge["when"] = ANCHORS[row[1]]
            challenge["open"] = row[2]
            challenge["done"] = row[3]
            challenge["test"] = row[4]
            challenge["stop"] = DROPS[row[5]]
            script = row[6] if len(row) > 6 else ""
            if script:
                challenge["say"] = script
                scripted += 1
            else:
                challenge.pop("say", None)

        path.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n")
        summary.append(
            f"{name:9} 100 tiers · {scripted:3} with a script · {rewritten} field(s) rewritten"
        )

    print("\n".join(summary))
    print(f"\n{len(ANCHORS)} anchors, {len(DROPS)} safety behaviours.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
