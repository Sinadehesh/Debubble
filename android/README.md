# DeBubble — Android app

Native Android, Kotlin + Jetpack Compose. No backend, no account, no network permission —
everything lives on the device.

```
./gradlew :app:assembleDebug     # build
./gradlew :app:test              # engine + curriculum invariants (JVM, no device needed)
./gradlew :app:installDebug      # install on a connected device
```

Requires JDK 17+, an Android SDK with API 35, and network access to Google's Maven repo.

> **Build status:** the authoring environment has no Android SDK and no route to
> `dl.google.com`, so nothing here is compiled locally. `.github/workflows/android.yml` builds
> the debug APK on every push touching `android/**`, and that is the authority on whether it
> compiles. What is verified locally is described under [Verification](#verification).

## Structure

```
engine/          pure Kotlin, no Android, fully unit-tested
  Pillar.kt        the three dimensions + the needs vocabulary
  Goal.kt          the five campaigns: missions, reps, principles
  Calibration.kt   Baseline answers -> entry tier per pillar (Rule 4)
  Curriculum.kt    Challenge / Ladder models, Served result
  Engine.kt        serving, gating, progression, streaks, readouts
data/
  AppState.kt      one serializable snapshot of everything
  Repository.kt    DataStore persistence + curriculum loading
audio/           procedural synthesis; Synth.kt is pure Kotlin and unit-tested
  Synth.kt         drone, tension bed, chime, stab — PCM generated at runtime
  SoundEngine.kt   AudioTrack playback, ringer/music/opt-in rules
ui/
  theme/           palette, two type roles, spacing
  components/      LivingBubble (AGSL), BubbleMap (fallback), HoldToCommit,
                   Fracture, Topography, ChallengeCard, ExpansionBurst,
                   Shockwave, Haptics, Motion
  screens/         Calibration, GoalPicker, Dashboard, Challenge, Campaign,
                   Completion, Principles, Profile, Transcendence
assets/curriculum/ access.json, activity.json, social.json — 300 authored challenges
assets/goals/      5 campaigns — 150 missions, 40 principles, 25 rep types
```

`DeBubbleViewModel` holds a `StateFlow<AppState>` and an explicit `Route`. Five screens is
too few to justify a nav graph, so navigation is a sealed `Route` plus `BackHandler`.

**Persistence is a single JSON blob in DataStore, not Room.** One user, local only, a few
hundred log rows, and no query beyond "newest first" — Room would add a codegen toolchain
for no benefit at this size. If the log ever needs real querying, that is the time to switch.

## The goal layer

The three daily challenges expand a bubble in general. A **campaign** points that expansion at
something the user actually wants, and it is what stops the app running out after three cards.

| Goal | Home pillar | What it is |
| --- | --- | --- |
| A life worth describing | Access | Break the weekly loop, collect experiences, become the one who organises things |
| A real circle | Social | Adult friendship: go first, go deeper, build the room |
| Find someone | Social | Dating built on vulnerability rather than technique |
| Closer, and honest about it | Social | Confidence and communication about desire and intimacy |
| Get good at something | Activity | A hobby taken from curiosity to visible competence |

Each campaign is **30 missions in 3 phases**, 8 unlockable principles, and its own set of rep
types. Progress is kept per goal, so switching costs nothing and coming back resumes exactly.

### Three mechanics, because one was not enough

The original loop gave you three cards and then nothing. Three things fixed that:

1. **Missions** — a fourth daily card, drawn from the active campaign. Reuses the whole Action
   Screen (commit ring, coach block, friction exit); only the source of the directive differs.
2. **Reps** — the unlimited half. Tiers and missions are capped at one a day by design, because
   a tier should be earned once. Reps are the volume work: log as many as you like, all day.
   A rep flagged `friction` (got turned down, it went badly, no reply) feeds the anti-score,
   so being refused is recorded as **output rather than failure**. Each mission suggests a
   daily rep target; over a month the rep count is what separates people.
3. **Principles** — short readable ideas that unlock as the campaign advances, so the reading
   tracks the doing rather than front-loading theory nobody acts on.

The new surfaces carry the same accessibility treatment as the rest: 48dp minimum targets,
explicit roles, and spoken state where meaning is carried by colour or dimming alone. A rep
row is one button that announces its label, its running total and what a tap will do.

### On the dating and intimacy campaigns

Both are built on the actual thesis of *Models* (Mark Manson) — attraction comes from
vulnerability and honest expression, not from technique — and neither contains anything
resembling pickup tactics. The through-line is: build a life worth having, say what you want
plainly, and let people say no.

Two rules are load-bearing and stated explicitly in the principles:

- **A no is final, immediately.** No persuading, no second ask, no going cold. An ask that
  cannot be refused cleanly was never an honest ask.
- **The intimacy campaign assumes an enthusiastic, freely given yes** that can be withdrawn at
  any moment. Asking is the practice, not a formality to get past. It is written as
  communication, shame and nerve — not as content.

Rejections are logged as reps and counted as credit, which is the same anti-score logic the
rest of the app already runs on.

## The interface

The app is a dark instrument panel that opens up as the ladder is climbed. That progression is
carried by light rather than by layout: `LocalAscension` supplies mean tier as 0..1 from the
root, and the surfaces that read it soften continuously — nobody should be able to point at the
day it changed.

The look is chiaroscuro, and that word is doing real work rather than decorating a colour
choice. The ground is `#000000` — not a dark grey, because tenebrism needs somewhere for the
light to fall off *to*. Pigments are earth: lapis for Access, verdigris for Activity, madder
for Social, all held above 4.5:1 on the panel, with candlelight reserved for Friction and used
nowhere else so that ember never reads as fault. Text is parchment at 16.6:1; `Ink.Faint` is
structure only and is never allowed to carry a word.

Nothing is outlined. Panels are defined by where light lands on them — `Modifier.litSurface`
rakes a gradient across a surface and falls off to transparent — and hierarchy is carried by
that treatment's emphasis rather than by borders of differing weight, which is a register the
old uniform 1px rules had no way to express. Corners are 3dp. Rules that survive at all, like
the divider, fade to transparent at both ends. The one thing that keeps a visible edge is a
progress groove, because a track has to be seen to read as a track.

| Piece | What it is |
| --- | --- |
| **Living bubble** | An AGSL membrane: three metaball lobes with fbm-displaced boundaries, breathing at 60 BPM. Viscosity comes from days since last activity, so a neglected bubble goes *heavy* rather than merely small; energy spikes on a completion; light comes from mean tier; touch pulls the fluid toward the thumb. |
| **Hold-to-Commit** | Three seconds of sustained pressure. Tension is progress squared, so the first second is quiet and the last is violent. A 24-step rising haptic waveform, shake on both axes, hollow thud if you let go early, sharp strike and a full-surface shockwave if you don't. |
| **Fracture** | Friction tears the surface: RGB channel separation and horizontal slip via a RenderEffect, with ember tear bands over the top. |
| **Topography** | The campaign as terrain — 30 monuments on ground that ascends away from you, cleared ones permanently lit, everything ahead fogged. |
| **Sound** | Four procedurally synthesised voices. No audio assets ship. |
| **Transcendence** | Tier 100 plays once: the interface comes apart into particles, the bubble expands past every boundary it had, and one line is left. |

### Two constraints that shaped all of it

**AGSL, not GLSL or Vulkan.** Compose cannot drive either. Android's runtime shader language is
AGSL and `RuntimeShader` arrived in API 33; minSdk here is 26. Both shader effects fall back
below 33 — the bubble to a flat additive canvas, the fracture to its ember tear bands alone.
Those paths are what much of the install base will actually see, so they are designed states
rather than stubs.

**Nothing engineered to retain.** There is deliberately no variable-ratio reward schedule, no
"critical hit" system, no jackpot animation. An app whose stated endgame is to render itself
obsolete cannot also be built to maximise time-in-app, and the users this is aimed at —
isolated people — are exactly the population where engineered compulsion does damage. The real
variable reward is already present and pointed outward: you genuinely do not know whether she
will say yes. See `TranscendenceScreen.kt`.

### Sound, and why it stays out of the way

Three rules, because this is the feature most likely to make someone uninstall:

- The ringer mode is absolute. Silent means silent, one-shots included.
- The ambient bed never plays over music — it stays out rather than ducking and fighting.
- The bed is opt-in. One-shot feedback on a deliberate action is expected; a drone that starts
  by itself is an intrusion. Both toggle from the profile and persist.

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

- **150 missions, 40 principles and 25 rep types validated**: 30 missions per campaign, every
  principle reference resolves and is surfaced by a mission, unlock points ascend, exposure
  escalates across all three phases of all five campaigns, every campaign can log volume and
  can log a refusal.
- **300 challenges validated** against every invariant below. Exposure trend by decade:
  Access `1.6 → 10.0`, Activity `1.8 → 9.6`, Social `2.4 → 9.9`. Composite difficulty
  strictly increasing across all ten decades of all three pillars.
- **Campaign algorithm transcribed to Java and executed**: this caught a real bug — progress
  was derived from the campaign step, which caps at 30, so a *finished* campaign showed a bar
  stuck at 96.7%. It now derives from completions.
- **Engine algorithm transcribed to Java and executed** on JDK 21: calibration sweep over the
  entire answer space stays inside 1–14, double-step/step-back behave, tier floors at 1 and
  caps at 100, cleared counts survive backsliding, streak restarts at 1, radius is monotonic
  and visibly grows early (`t1 0.246 → t10 0.392`). Summit reached in 75 completions.
- **Structural check** across every Kotlin file: brace/paren balance, symbol cross-reference,
  and a scan for Kotlin's nested-block-comment trap.
- **Isometric projection prototyped in a browser canvas and screenshotted** before being ported
  to Compose, because none of that geometry is visible from the authoring environment. The
  first attempt drew the monuments as flat bowties.
- **9 audio tests** on clipping, loop-seam silence, decay, filter selectivity and normalisation
  of both silent and overdriven buffers. Audio fails loudly and invisibly.

`app/src/test/` carries the same invariants as real JUnit tests (`CurriculumTest`,
`EngineTest`, `GoalTest`) reading the shipped asset files, so `./gradlew :app:test` re-checks all of it on
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

## What each kind of check actually catches

Worth recording, because the split turned out to be absolutely clean over this project's life:

| Found locally | Found only by CI |
| --- | --- |
| A progress bar stuck at 96.7% on a finished campaign | Missing imports |
| Monuments rendered as flat bowties | A `\n` that became a literal line break inside a string |
| A difficulty metric that was wrong for the Social pillar | Kotlin prohibiting varargs of value classes |
| A float LCG that overflowed into a pure tone | A property delegate without `getValue`/`setValue` |
| A test asserting on an unreachable state | |

Design and algorithm bugs are reachable with harnesses, prototypes and validators. Compile
errors are not reachable at all without a compiler. Push small and let CI answer the second
question.

## Known gaps

- **Never run on a device.** It compiles and the tests pass, but no screen has been rendered on
  real hardware: layout, the membrane on a real GPU, and the feel of the three-second hold are
  all unverified.
- No unboxing ceremony for unlocked principles. A physics drop and a tear-to-open gesture is
  excellent the first three times and an obstacle by the fortieth; if built, the full ceremony
  should fire on first unlock only.
- No gyroscope-driven caustics. Achievable, but a continuous sensor feeding a full-screen
  fragment shader is the most expensive thing this app could do to a battery.
- No notifications or daily reminder scheduling.
- `Access` progress is reported as tier and evidence count, not measured distance — no
  location permission is requested, by choice. Real km would need GPS and a privacy story.
- No export of the journal or history.
- One active campaign at a time. Running two in parallel is not supported.
- Rep logging is self-reported and unverifiable, which is the correct trade for not asking
  for location, contacts or any other permission.
- Instrumented UI tests are not written; only JVM unit tests exist.
- Typography uses the platform serif and monospace rather than a licensed display face.
