# DeBubble — Android app

Native Android, Kotlin + Jetpack Compose. No backend, no account, no network permission —
everything lives on the device.

```
./gradlew :app:assembleDebug     # build
./gradlew :app:test              # engine + curriculum invariants (JVM, no device needed)
./gradlew :app:installDebug      # install on a connected device
```

Requires JDK 17+, an Android SDK with API 35, and network access to Google's Maven repo.

> **Build status: not compiled.** This project was written in an environment with no Android
> SDK and no route to `dl.google.com`, so it has never been through `assembleDebug`. What *has*
> been verified is described under [Verification](#verification) — expect to fix trivia
> (an import, a signature) on the first real build.

## Structure

```
engine/          pure Kotlin, no Android, fully unit-tested
  Pillar.kt        the three dimensions + the needs vocabulary
  Calibration.kt   Baseline answers -> entry tier per pillar (Rule 4)
  Curriculum.kt    Challenge / Ladder models, Served result
  Engine.kt        serving, gating, progression, streaks, readouts
data/
  AppState.kt      one serializable snapshot of everything
  Repository.kt    DataStore persistence + curriculum loading
ui/
  theme/           palette, two type roles, spacing
  components/      BubbleMap, HoldToCommit, ChallengeCard, ExpansionBurst, Haptics
  screens/         Calibration, Dashboard, Challenge, Completion, Profile
assets/curriculum/ access.json, activity.json, social.json — 300 authored challenges
```

`DeBubbleViewModel` holds a `StateFlow<AppState>` and an explicit `Route`. Five screens is
too few to justify a nav graph, so navigation is a sealed `Route` plus `BackHandler`.

**Persistence is a single JSON blob in DataStore, not Room.** One user, local only, a few
hundred log rows, and no query beyond "newest first" — Room would add a codegen toolchain
for no benefit at this size. If the log ever needs real querying, that is the time to switch.

## How the 100-tier ladder works

The curriculum is **authored data, not generated at runtime**: 100 tiers per pillar in
`assets/curriculum/*.json`, each with a directive, a coach line, time/cost/exposure ratings,
capability requirements, and an alternate.

| Field | Meaning |
| --- | --- |
| `do` | The directive. An instruction, never a suggestion. |
| `why` | One sentence of framing. This is where the voice lives. |
| `min` / `cost` | Time in minutes, money in local currency. |
| `exp` | Exposure — the vulnerability rating, 1–10. Climbs hardest. |
| `needs` | Capabilities required: transit, bike, car, overnight, multiday, passport… |
| `alt` | Same rung, cheaper/closer/shorter route to the same lesson. |

### Personalization

1. **Entry tier** from six calibration cards:
   ```
   access   t₀ = clamp(1 + radiusKm/6 + transportModes,            1, 14)
   activity t₀ = clamp(1 + (100 − routinePct)/11 + noveltyRecency,  1, 14)
   social   t₀ = clamp(1 + (10 − resistance)*0.8 + convos/4,        1, 14)
   ```
2. **Capability gating.** A challenge needing something the user said they lack — or costing
   more than their stated budget — serves the authored `alt` instead, with the reason shown
   on the card. A challenge someone physically cannot do is worse than one slightly too easy.
3. **Adaptive pacing.** Three clean completions earn a two-tier step. Two frictions at one
   tier steps *back* — the ladder meets the user rather than stalling them against a rung
   that does not fit yet.
4. **Swap.** One "not this one" per pillar per day, serving the alternate at the same tier.
5. **Recalibrate** any time from the profile. It re-pitches the ladder and keeps every
   cleared tier.

### Why nothing here punishes

The app is meant to leave people more capable, so the failure modes are designed as carefully
as the rewards.

- **Friction is credit.** It counts awkward, refused, or abandoned-as-too-hard. It is the
  largest numeral on the profile, it sits *above* the streak, and it can only go up. Ember is
  reserved for it alone and never marks an error, so the colour reads as credit, not fault.
- **Aborting is free.** Backing out of a challenge logs nothing. Only *saying* it was too
  much logs friction — and that is rewarded.
- **A missed day pauses the streak**, restarting at 1 rather than 0, and never clears
  friction, tiers, or evidence.
- **Evidence, not score.** The profile reports places gone, things done, people reached, and
  hours invested. Streaks measure compliance with an app; evidence answers "am I actually
  different?", which is the only question that matters at day 60.

## Verification

Run without an Android SDK, so the *content and the algorithm* are checked rather than the
build:

- **300 challenges validated** against every invariant below. Exposure trend by decade:
  Access `1.6 → 10.0`, Activity `1.8 → 9.6`, Social `2.4 → 9.9`. Composite difficulty
  strictly increasing across all ten decades of all three pillars.
- **Engine algorithm transcribed to Java and executed** on JDK 21: calibration sweep over the
  entire answer space stays inside 1–14, double-step/step-back behave, tier floors at 1 and
  caps at 100, cleared counts survive backsliding, streak restarts at 1, radius is monotonic
  and visibly grows early (`t1 0.246 → t10 0.392`). Summit reached in 75 completions.
- **Structural check** on all 20 Kotlin files.

`app/src/test/` carries the same invariants as real JUnit tests (`CurriculumTest`,
`EngineTest`) reading the shipped asset files, so `./gradlew :app:test` re-checks all of it on
a machine with an SDK.

### Curriculum invariants

| Rule | Assertion |
| --- | --- |
| 1 — micro-beginnings | Tier 1 free, ≤5 min, exposure 1. Tiers 1–10 cost nothing. |
| 2 — invisible escalation | Exposure trend never reverses by decade; composite difficulty rises every decade; first 25 tiers stay ≤5 exposure; no exposure cliff >2 points; no difficulty backslide >0.20. |
| 3 — macro-endings | Tier 100 at exposure 10 and >50× tier 1's time; closing 25 tiers all ≥7 exposure. |
| 4 — calibration | Entry tier in 1–14 across the whole answer space; monotonic in capability; unmet needs always substitute. |
| Data | Exactly 100 tiers indexed 1–100 per pillar; no duplicate directives; every `needs` value gateable; `alt` never equals `do`. |

**Per-tier monotonicity is deliberately not asserted.** A ladder mixing two-minute acts with
month-long projects has local texture — a short sharp tier between two long ones is rhythm,
not a defect. Forcing monotonicity would mean inflating durations to satisfy a metric. The
tests assert trend plus a cap on local backslide instead.

Difficulty weights differ per pillar, because the pillars measure different things:

| Pillar | exposure | time | money |
| --- | --- | --- | --- |
| Access | 0.25 | 0.50 | 0.25 |
| Activity | 0.40 | 0.40 | 0.20 |
| Social | 0.75 | 0.20 | 0.05 |

A single global weighting made the honest Social ladder look mis-ordered — saying no to
someone takes five minutes and costs nothing, but it is a real step up.

## Known gaps

- Never compiled (see above).
- No notifications or daily reminder scheduling.
- `Access` progress is reported as tier and evidence count, not measured distance — no
  location permission is requested, by choice. Real km would need GPS and a privacy story.
- No export of the journal or history.
- Instrumented UI tests are not written; only JVM unit tests exist.
- Typography uses the platform grotesque and monospace rather than a licensed display face.
