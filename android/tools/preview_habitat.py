#!/usr/bin/env python3
"""Render the habitat as a browser page, so the room can be looked at without a device.

Generated from engine/Habitat.kt and ui/components/Habitat.kt rather than hand-copied: the
stage colours, thresholds, labels, notes and openness values are parsed out of the Kotlin, so
a preview that disagrees with the app is a parse failure rather than something nobody notices.

The drawing is a transcription of HabitatBackdrop's Canvas code. One honest difference: the
star field uses a seeded PRNG, and Kotlin's Random(7) and a JavaScript one do not produce the
same sequence — so the stars sit in different places than they will on a phone. Everything
that carries meaning (the walls, the ceiling, the far wall, the window, the door, the horizon,
the skyline, the fade) is positioned by the same arithmetic.

Run:  python3 android/tools/preview_habitat.py
"""

import json
import pathlib
import re
import sys

HERE = pathlib.Path(__file__).resolve().parent
SRC = HERE.parent / "app" / "src" / "main" / "java" / "com" / "debubble" / "app"
OUT = HERE / "preview" / "habitat.html"

STAGE_RE = re.compile(
    r"threshold\s*=\s*(\d+),\s*"
    r"label\s*=\s*\"([^\"]+)\",\s*"
    r"note\s*=\s*\"([^\"]+)\",\s*"
    r".*?ground\s*=\s*0x(FF[0-9A-Fa-f]{6}),\s*"
    r"horizon\s*=\s*0x(FF[0-9A-Fa-f]{6}),\s*"
    r"structure\s*=\s*0x(FF[0-9A-Fa-f]{6}),\s*"
    r"openness\s*=\s*([0-9.]+)f",
    re.S,
)


def stages():
    src = (SRC / "engine" / "Habitat.kt").read_text()
    out = []
    for m in STAGE_RE.finditer(src):
        threshold, label, note, ground, horizon, structure, openness = m.groups()
        out.append(
            {
                "threshold": int(threshold),
                "label": label,
                "note": note,
                "ground": "#" + ground[2:],
                "horizon": "#" + horizon[2:],
                "structure": "#" + structure[2:],
                "openness": float(openness),
            }
        )
    return out


HTML = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>The Room</title>
<style>
  :root {
    --void: #0E1116; --surface: #181D26; --surface-high: #232B38;
    --border: #6B7789; --faint: #4A5462;
    --primary: #F5F7FA; --secondary: #B4BECD; --muted: #8593A6;
    --gold: #FFC94D; --access: #4DA3FF; --activity: #3DDC97; --social: #FF6B81;
    --radius: 14px; --radius-lg: 20px;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, sans-serif;
  }
  * { box-sizing: border-box; }
  body {
    margin: 0; background: var(--void); color: var(--primary);
    -webkit-font-smoothing: antialiased;
  }
  .wrap { max-width: 1180px; margin: 0 auto; padding: 32px 16px 72px; }
  header { margin-bottom: 26px; }
  h1 { font-size: 27px; letter-spacing: -0.5px; margin: 0 0 8px; }
  .lede { color: var(--secondary); font-size: 15px; line-height: 1.55; max-width: 62ch; margin: 0; }
  .note { color: var(--muted); font-size: 13px; line-height: 1.5; max-width: 70ch; margin: 12px 0 0; }

  /* ---- the scrubber ---- */
  .panel {
    background: var(--surface); border: 1px solid var(--border);
    border-radius: var(--radius-lg); padding: 18px;
  }
  .scrub { margin: 26px 0 34px; }
  .scrub-head {
    display: flex; justify-content: space-between; align-items: baseline;
    gap: 14px; flex-wrap: wrap; margin-bottom: 4px;
  }
  .scrub-head strong { font-size: 20px; }
  .rungs { color: var(--muted); font-size: 13px; font-variant-numeric: tabular-nums; }
  .scrub p { color: var(--secondary); font-size: 14px; line-height: 1.55; margin: 8px 0 16px; }
  input[type=range] { width: 100%; accent-color: var(--gold); height: 28px; }
  .ticks {
    display: flex; justify-content: space-between; color: var(--faint);
    font-size: 11px; margin-top: -4px;
  }
  .ticks span { flex: 1; text-align: center; }
  .ticks span:first-child { text-align: left; }
  .ticks span:last-child { text-align: right; }
  .ticks .on { color: var(--gold); }

  .stage-view { display: grid; grid-template-columns: 300px 1fr; gap: 22px; align-items: start; }
  @media (max-width: 760px) { .stage-view { grid-template-columns: 1fr; } }
  canvas { display: block; width: 100%; border-radius: var(--radius); }
  .phone { background: var(--void); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; }
  .phone .below { padding: 14px; }
  .card {
    background: var(--surface); border: 1px solid var(--border);
    border-radius: var(--radius); padding: 12px 13px; margin-bottom: 9px;
  }
  .card b { display: block; font-size: 14px; font-weight: 600; }
  .card small { color: var(--muted); font-size: 12px; }
  .dots { display: flex; gap: 7px; margin-bottom: 10px; }
  .dot { width: 8px; height: 8px; border-radius: 50%; }

  /* ---- the grid of all seven ---- */
  h2 { font-size: 15px; color: var(--muted); font-weight: 600; margin: 40px 0 14px; }
  .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); gap: 14px; }
  .tile { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; }
  .tile canvas { border-radius: 0; }
  .tile .meta { padding: 11px 13px 13px; }
  .tile .meta b { font-size: 14px; }
  .tile .meta p { color: var(--secondary); font-size: 12.5px; line-height: 1.5; margin: 5px 0 0; }
  .tile .meta .th { color: var(--muted); font-size: 11.5px; margin-top: 7px; font-variant-numeric: tabular-nums; }
  .tile.current { border-color: var(--gold); }
  .tile.current .meta b::after { content: " · you are here"; color: var(--gold); font-weight: 500; font-size: 11.5px; }

  table { border-collapse: collapse; width: 100%; font-size: 13px; margin-top: 10px; }
  th, td { text-align: left; padding: 7px 10px; border-bottom: 1px solid #242B36; }
  th { color: var(--muted); font-weight: 600; font-size: 11.5px; }
  td { font-variant-numeric: tabular-nums; color: var(--secondary); }
  td.swatch-cell { width: 1%; white-space: nowrap; }
  .swatch { display: inline-block; width: 16px; height: 16px; border-radius: 4px; border: 1px solid var(--border); vertical-align: -3px; margin-right: 7px; }
</style>
</head>
<body>
<div class="wrap">

<header>
  <h1>The Room</h1>
  <p class="lede">
    Seven stages, opened by clearing rungs. Drag the slider to walk through them, or read the
    whole ladder below. This is a transcription of <code>HabitatBackdrop</code>'s drawing code;
    the colours, thresholds and wording are parsed straight out of <code>Habitat.kt</code>.
  </p>
  <p class="note">
    One honest difference from a phone: the star field uses a seeded PRNG, and Kotlin's
    <code>Random(7)</code> and JavaScript's do not produce the same sequence, so the stars sit
    elsewhere. Everything that carries meaning — walls, ceiling, far wall, window, door,
    horizon, skyline, the fade to flat ground — is placed by the same arithmetic.
  </p>
</header>

<section class="panel scrub">
  <div class="scrub-head">
    <strong id="s-label"></strong>
    <span class="rungs" id="s-rungs"></span>
  </div>
  <p id="s-note"></p>
  <input type="range" id="slider" min="0" max="6" value="0" step="1">
  <div class="ticks" id="ticks"></div>

  <div class="stage-view" style="margin-top:20px">
    <div class="phone">
      <canvas id="hero" width="300" height="300"></canvas>
      <div class="below">
        <div class="dots">
          <span class="dot" style="background:var(--access)"></span>
          <span class="dot" style="background:var(--activity)"></span>
          <span class="dot" style="background:var(--social)"></span>
        </div>
        <div class="card"><b>Today</b><small>Three challenges, as they appear on the dashboard</small></div>
        <div class="card"><b id="s-card"></b><small id="s-caption"></small></div>
      </div>
    </div>
    <div>
      <table>
        <tr><th>Token</th><th>Value</th><th>What it does</th></tr>
        <tr><td class="swatch-cell"><span class="swatch" id="sw-g"></span>ground</td><td id="v-g"></td><td>Under every tabbed screen</td></tr>
        <tr><td class="swatch-cell"><span class="swatch" id="sw-h"></span>horizon</td><td id="v-h"></td><td>The bright band, and the light through the opening</td></tr>
        <tr><td class="swatch-cell"><span class="swatch" id="sw-s"></span>structure</td><td id="v-s"></td><td>Walls, ceiling, floor lines, stars, skyline</td></tr>
        <tr><td>openness</td><td id="v-o"></td><td>Drives the geometry, not the ordinal</td></tr>
        <tr><td>threshold</td><td id="v-t"></td><td>Rungs cleared to reach it</td></tr>
      </table>
      <p class="note" style="margin-top:14px">
        The ground barely changes across seven stages, and that is deliberate. In a dark theme
        with these inks the usable brightness range is narrow — muted text needs 4.5:1 and a
        panel border needs 3.0:1 against whatever is behind it, and both fall as the ground
        lifts. <code>check_habitat.py</code> solved for the ceiling; the last stage sits just
        under it. So the geometry carries the change, because geometry is not rationed.
      </p>
    </div>
  </div>
</section>

<h2>All seven</h2>
<div class="grid" id="grid"></div>

</div>

<script>
const STAGES = __STAGES__;

/* A seeded PRNG so the sky does not reshuffle on every redraw, matching the intent of
   remember(stage) in the Compose version (though not Kotlin's exact sequence). */
function rng(seed) {
  let s = seed >>> 0;
  return function () {
    s = (s * 1664525 + 1013904223) >>> 0;
    return s / 4294967296;
  };
}

function hexA(hex, a) {
  const n = parseInt(hex.slice(1), 16);
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${a})`;
}

/* Transcription of HabitatBackdrop. Every constant below appears in
   ui/components/Habitat.kt with the same value. */
function draw(cv, st) {
  const ctx = cv.getContext('2d');
  const dpr = window.devicePixelRatio || 1;
  const w = cv.clientWidth, h = cv.clientHeight;
  cv.width = w * dpr; cv.height = h * dpr;
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.clearRect(0, 0, w, h);

  const open = st.openness;
  const yH = h * (0.58 - 0.10 * open);
  const vx = w * 0.5;

  const g = ctx.createLinearGradient(0, 0, 0, h);
  g.addColorStop(0, st.ground);
  g.addColorStop(Math.min(0.95, Math.max(0.05, yH / h)), st.horizon);
  g.addColorStop(1, st.ground);
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, w, h);

  const line = (x1, y1, x2, y2, a, lw) => {
    if (a <= 0.01) return;
    ctx.strokeStyle = hexA(st.structure, a);
    ctx.lineWidth = lw || 1;
    ctx.beginPath(); ctx.moveTo(x1, y1); ctx.lineTo(x2, y2); ctx.stroke();
  };

  // Floor: present at every stage.
  for (let i = -3; i <= 3; i++) line(vx, yH, vx + i * w * 0.30, h, 0.30);
  for (let k = 1; k <= 4; k++) {
    const y = yH + (h - yH) * Math.pow(k / 5, 2.1);
    line(0, y, w, y, 0.16);
  }

  // Walls and ceiling: slide outward and fade as the room opens.
  const wall = 1 - open;
  if (wall > 0.01) {
    for (const side of [-1, 1]) {
      const xo = vx + side * w * (0.14 + 0.46 * open);
      line(xo, -h * 0.08, xo, h, 0.55 * wall, 2);
      line(xo, 0, vx, yH, 0.40 * wall);
      line(xo, h, vx, yH, 0.26 * wall);
    }
    const yc = h * 0.06 - h * 0.25 * open;
    line(0, yc, w, yc, 0.34 * wall);
  }

  // The far wall, and what is cut into it.
  const far = Math.max(0, 1 - open * 2.2);
  if (far > 0.01) {
    const fw = w * 0.30, fh = h * 0.26;
    ctx.strokeStyle = hexA(st.structure, 0.45 * far);
    ctx.lineWidth = 1;
    ctx.strokeRect(vx - fw / 2, yH - fh, fw, fh);
    if (open > 0.08 && open < 0.26) {
      const ww = fw * 0.34, wh = fh * 0.30;           // a window
      ctx.fillStyle = hexA(st.horizon, 0.85);
      ctx.fillRect(vx - ww / 2, yH - fh * 0.70, ww, wh);
    } else if (open >= 0.26) {
      const dw = fw * 0.30;                            // a door
      ctx.fillStyle = hexA(st.horizon, 0.90);
      ctx.fillRect(vx - dw / 2, yH - fh * 0.82, dw, fh * 0.82);
    }
  }

  // What turns out to have been outside the whole time.
  const sky = Math.min(1, Math.max(0, (open - 0.45) / 0.55));
  const r1 = rng(7);
  const count = Math.trunc(26 * Math.max(0, (open - 0.45) / 0.55));
  ctx.fillStyle = hexA(st.structure, 0.35 + 0.45 * sky);
  for (let i = 0; i < count; i++) {
    const fx = r1(), fy = r1(), rr = 0.5 + r1() * 0.8;
    ctx.beginPath();
    ctx.arc(fx * w, fy * yH * 0.82, rr, 0, Math.PI * 2);
    ctx.fill();
  }
  if (open >= 0.62) {
    const tall = Math.min(1, Math.max(0, (open - 0.62) / 0.38));
    const r2 = rng(11);
    ctx.fillStyle = hexA(st.structure, 0.30);
    let x = 0;
    while (x < 1) {
      const bw = 0.03 + r2() * 0.06;
      const bh = 0.012 + r2() * 0.043;
      const height = h * bh * tall;
      ctx.fillRect(x * w, yH - height, bw * w, height);
      x += bw + 0.005 + r2() * 0.03;
    }
  }
  if (open >= 0.45) line(0, yH, w, yH, 0.45);

  // Back to flat ground before any text starts.
  const fade = ctx.createLinearGradient(0, 0, 0, h);
  fade.addColorStop(0.58, hexA(st.ground, 0));
  fade.addColorStop(1, hexA(st.ground, 1));
  ctx.fillStyle = fade;
  ctx.fillRect(0, 0, w, h);
}

/* ---- the scrubber ---- */
const slider = document.getElementById('slider');
const hero = document.getElementById('hero');

function caption(i) {
  const next = STAGES[i + 1];
  if (!next) return 'The room is fully open. Nothing left to unseal.';
  const gap = next.threshold - STAGES[i].threshold;
  return `${gap} more rungs to ${next.label.toLowerCase()}.`;
}

function show(i) {
  const st = STAGES[i];
  document.getElementById('s-label').textContent = st.label;
  document.getElementById('s-rungs').textContent =
    st.threshold === 0 ? 'from the first day' : `${st.threshold} rungs cleared`;
  document.getElementById('s-note').textContent = st.note;
  document.getElementById('s-card').textContent = st.label;
  document.getElementById('s-caption').textContent = caption(i);
  document.getElementById('v-g').textContent = st.ground;
  document.getElementById('v-h').textContent = st.horizon;
  document.getElementById('v-s').textContent = st.structure;
  document.getElementById('v-o').textContent = st.openness.toFixed(2);
  document.getElementById('v-t').textContent = st.threshold;
  document.getElementById('sw-g').style.background = st.ground;
  document.getElementById('sw-h').style.background = st.horizon;
  document.getElementById('sw-s').style.background = st.structure;
  document.querySelectorAll('#ticks span').forEach((s, k) =>
    s.classList.toggle('on', k === i));
  document.querySelectorAll('.tile').forEach((t, k) =>
    t.classList.toggle('current', k === i));
  draw(hero, st);
}

document.getElementById('ticks').innerHTML =
  STAGES.map(s => `<span>${s.threshold}</span>`).join('');
slider.max = STAGES.length - 1;
slider.addEventListener('input', () => show(+slider.value));

/* ---- all seven ---- */
document.getElementById('grid').innerHTML = STAGES.map((s, i) => `
  <div class="tile">
    <canvas data-i="${i}" width="230" height="190"></canvas>
    <div class="meta">
      <b>${s.label}</b>
      <p>${s.note}</p>
      <div class="th">${s.threshold === 0 ? 'Start' : s.threshold + ' rungs'} · openness ${s.openness.toFixed(2)}</div>
    </div>
  </div>`).join('');

function drawAll() {
  document.querySelectorAll('.tile canvas').forEach(cv => draw(cv, STAGES[+cv.dataset.i]));
  show(+slider.value);
}
drawAll();
window.addEventListener('resize', drawAll);
</script>
</body>
</html>
"""


def main():
    found = stages()
    if len(found) != 7:
        print(f"parsed {len(found)} stages from Habitat.kt, expected 7")
        return 1
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(HTML.replace("__STAGES__", json.dumps(found, indent=2)))
    print(f"wrote {OUT}")
    for s in found:
        print(f"  {s['label']:13} {s['threshold']:>3} rungs  {s['ground']}  open {s['openness']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
