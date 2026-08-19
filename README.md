# DeBubble

Behavioural expansion engine. Three pillars, 100 tiers each, one challenge per pillar per day.

| | |
| --- | --- |
| [`index.html`](index.html) | Design prototype and specification — every screen running live in one file, no build step. |
| [`android/`](android/README.md) | The Android app. Kotlin + Compose, 300 challenges + 5 goal campaigns, adaptive engine. **Not yet compiled** — see its README. |

## The design prototype

`index.html` is the complete frontend spec: a working interactive prototype of every screen plus
the design specification around it. Single file, no build step, no dependencies, no network
requests. Open it directly, or publish it as-is.

## What's in it

| Section | Contents |
| --- | --- |
| Hero | Live device running the app from cold open |
| 01 Live build | Full state machine, jumpable step rail, live engine readout |
| 02 Screens | Baseline Calibration, Dashboard, Action Screen, Completion & Micro-Journal, Profile & Anti-Score — each pinned frame is the real component, with layout / hierarchy / interaction annotations |
| 03 Flow | Cold open → first completed Level 1 challenge, nine steps, with the state each writes |
| 04 Engine | Sampled tier ladders for all three pillars, the escalation curve, and the calibration → entry-tier formulas |
| 05 System | Palette, type roles, motion and haptic specs, component inventory, voice, surface rules |

## The three pillars

| Pillar | Dimension | Accent |
| --- | --- | --- |
| Access | Geographic & environmental | Electric blue `#2F6BFF` |
| Activity | Experiential & habitual | Acid green `#B4FF25` |
| Social | Interpersonal & connection | Vibrant crimson `#FF2D55` |
| — | Friction / Courage (reserved) | Ember `#FF8A1F` |

Ember is used only for the Friction Score, never for warnings or errors, so the colour reads
permanently as credit rather than fault.

## The progression engine

Onboarding takes four answers — transport modes, furthest range in 30 days, share of the week
that repeated, and stranger-resistance — and computes an entry tier per pillar:

```
access   t₀ = clamp(1 + floor(radius_km / 6) + transport_modes,          1, 14)
activity t₀ = clamp(1 + floor((100 − routine) / 11) + novelty_recency,   1, 14)
social   t₀ = clamp(1 + floor((10 − anxiety) * 0.8) + floor(convos / 4), 1, 14)
```

Difficulty rises on `(t/100)^1.9`, so the first thirty tiers are almost flat. Tiers advance per
pillar on completion, never on the calendar — a user can be at Access 040 and Social 003, and the
Bubble Map renders exactly that asymmetry. Repeated friction at a tier steps the user back rather
than stalling them; three clean completions unlock a double step.

## Implementation notes

- **Bubble Map** — Canvas 2D, three additively-blended spheres (`globalCompositeOperation:
  "lighter"`) on a polar grid. Radius binds to tier; overlaps light up where pillars reinforce.
  Scenes are registered in one shared rAF loop and paused off-screen via `IntersectionObserver`.
- **One component tree** — every device on the page is the same `makeDevice()` instance rendering
  from the same state shape. The spec frames are pinned to a state, not redrawn as pictures.
- **Commit gesture** — 900ms long-press with a stroked ring; releasing early snaps to zero.
  Haptics via `navigator.vibrate`.
- **Reduced motion** — drift, burst, and radius easing disable under `prefers-reduced-motion`;
  the ring still fills.
- **Dark by intent, not by default.** Additive light needs a black ground to read as energy, so
  the page commits to one theme and paints every colour explicitly rather than inheriting a host
  background.

No webfonts: the CSP on the publish target blocks font CDNs, so type personality comes from
treatment — a heavy grotesque at −.045em for anything the user must *do*, and a wide-tracked
uppercase mono for everything the system *reports*.
