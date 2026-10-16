# Habitat artwork — generation prompts

Seven images, one per `Stage` in `engine/Habitat.kt`. Generated through the Canva connector
with `generate-image`, aspect ratio `LANDSCAPE_3_2`, then exported at 1536×1024 and processed
into `res/drawable-nodpi/*.webp` by `android/tools/prepare_habitat_art.py`.

## Status

| Stage | File | Origin |
|---|---|---|
| Sealed room | `habitat_sealed.webp` | generated |
| One window | `habitat_window.webp` | generated |
| Door open | `habitat_door.webp` | generated |
| The street | `habitat_street.webp` | **derived** |
| Open ground | `habitat_open.webp` | **derived** |
| Horizon | `habitat_horizon.webp` | **derived** |
| No walls | `habitat_unwalled.webp` | generated |

The three derived images were composited from "Door open" and "No walls" because the Canva
account hit its credit quota partway through the series. They are a stopgap. To replace them:
run the prompts below, export at 1536×1024, save as `s4.png` / `s5.png` / `s6.png` in the
source directory, and re-run `prepare_habitat_art.py` — it prefers a real render whenever one
is present.

## The shared skeleton

Every prompt ends with the same paragraph. Consistency across the seven matters more than any
individual frame, and the grid floor is what carries it — it is also the one element the app's
own copy says is there at every stage.

> The floor is a glowing thin-line perspective grid in luminous steel blue receding to a
> single central vanishing point. No furniture, no people, no text, no letters, no numbers.
> Near-black navy ground (#0E1116), deep slate blue (#1D283B), luminous steel-blue line work
> (#44608C), with a faint cold glow where the lines meet. Flat vector line art with subtle
> atmospheric haze. Cinematic, architectural, calm.

## Per stage

**1 · Sealed room** — Abstract minimalist architectural artwork, one-point perspective. A
SEALED EMPTY ROOM with no doors and no windows. Four blank walls and a ceiling close the room
in completely — the walls are drawn as thin luminous edge lines meeting the grid floor, boxing
it in tightly on all sides. The far wall is solid and unbroken. No openings of any kind.
Enclosed, airless, symmetrical, claustrophobic but beautiful. *+ skeleton*

> A first attempt without the grid floor came back as a near-black rectangle, luma range
> 1–21. Conceptually right, useless as artwork. The grid floor is what makes "sealed" legible.

**2 · One window** — …An enclosed room with ONE SMALL BRIGHT WINDOW high on the far wall — a
single small rectangle of cold white-blue light, the only opening, with a soft shaft of light
spilling from it down onto the floor. The side walls and ceiling still close the room in
completely. *+ skeleton*

**3 · Door open** — …An enclosed room whose far wall has a TALL OPEN DOORWAY standing open in
the centre — floor to near-ceiling, filled with bright cold white-blue light flooding through
it and spilling in a wide bright wedge across the floor toward the viewer. The side walls and
ceiling still enclose the room. *+ skeleton*

**4 · The street** — …A room whose SIDE WALLS HAVE FALLEN AWAY — only a thin ceiling plane and
a low remnant of the far wall are left, and the space is wide open to left and right,
revealing faint distance beyond the edges. The grid floor runs out past where the walls used
to be. Opening up. *+ skeleton*

**5 · Open ground** — …NO WALLS REMAIN — only a faint dissolving ceiling plane high overhead,
fading out. Wide open ground stretches away in every direction and a faint horizon line is
just beginning to appear in the far distance. Spacious. *+ skeleton*

**6 · Horizon** — …NO WALLS AND NO CEILING AT ALL — a clear bright luminous HORIZON LINE runs
the full width of the image in the far distance, glowing cold white-blue, with the first
scattered faint stars appearing in the dark sky above it. Vast. *+ skeleton*

**7 · No walls** — …NO WALLS AT ALL — a vast open plain under a wide star field, a bright
luminous horizon line far in the distance with a faint distant city skyline sitting on it.
Expansive, cold, luminous, liberating. *+ skeleton*

## Rules the images have to keep

- **Nothing is ever drawn on top of them.** An earlier design put the stage name over the art
  behind a gradient scrim; `prepare_habitat_art.py` measured it and found Muted text at 1.01:1
  against the light wedge in "Door open" — the same colour as its background. The scrim needed
  to fix that would have buried the best part of the image. Text lives underneath the hero,
  on solid ground.
- **They must end on the page colour.** The scrim that blends the bottom edge has to reach
  full opacity *before* the last row; a gradient that only completes at 1.0 never gets there,
  and every hero ends on a visible hairline. The checker enforces this.
- **No text in the image itself.** Every prompt says so three ways, because image models add
  labels unprompted and a misspelt word baked into a shipped asset is not fixable in code.
