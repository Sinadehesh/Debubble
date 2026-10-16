#!/usr/bin/env python3
"""Render the redesigned dashboard as a browser page, with the real shipped artwork.

The look of this app has been rejected three times, and the loop of "write 2000 lines of
Kotlin, push, wait for CI, find out it is still ugly" is the reason. This renders the actual
screen at phone width, using the actual WebP files from res/drawable-nodpi and the actual
stage data from engine/Habitat.kt, so the design can be judged before it is built on.

It is a mock, not the app. The Compose layout is the source of truth; this is a faithful
transcription of it, and the things it cannot show honestly (the ring's animation, real
touch targets, system insets) are labelled as such on the page.

Run:  python3 android/tools/preview_dashboard.py
"""

import base64
import json
import pathlib
import re
import sys

HERE = pathlib.Path(__file__).resolve().parent
APP = HERE.parent / "app" / "src" / "main"
ART = APP / "res" / "drawable-nodpi"
OUT = HERE / "preview" / "dashboard.html"

STAGE_RE = re.compile(
    r"threshold\s*=\s*(\d+),\s*label\s*=\s*\"([^\"]+)\",\s*note\s*=\s*\"([^\"]+)\",\s*"
    r".*?ground\s*=\s*0x(FF[0-9A-Fa-f]{6}),.*?openness\s*=\s*([0-9.]+)f",
    re.S,
)

# Which file belongs to which stage, and whether it was generated or derived. Mirrors
# artFor() in ui/components/Habitat.kt and the table in docs/habitat-prompts.md.
FILES = [
    ("habitat_sealed", "generated"),
    ("habitat_window", "generated"),
    ("habitat_door", "generated"),
    ("habitat_street", "derived"),
    ("habitat_open", "derived"),
    ("habitat_horizon", "derived"),
    ("habitat_unwalled", "generated"),
]


def stages():
    src = (APP / "java" / "com" / "debubble" / "app" / "engine" / "Habitat.kt").read_text()
    out = []
    for i, m in enumerate(STAGE_RE.finditer(src)):
        threshold, label, note, ground, openness = m.groups()
        name, origin = FILES[i]
        art = ART / f"{name}.webp"
        if not art.is_file():
            raise SystemExit(f"missing artwork: {art}")
        out.append({
            "threshold": int(threshold),
            "label": label,
            "note": note,
            "ground": "#" + ground[2:],
            "openness": float(openness),
            "origin": origin,
            "file": f"{name}.webp",
            "data": "data:image/webp;base64,"
                    + base64.b64encode(art.read_bytes()).decode("ascii"),
        })
    return out


HTML = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Dashboard Redesign</title>
<style>
  :root {
    --void:#0E1116; --surface:#181D26; --surface-high:#232B38; --well:#11151C;
    --border:#6B7789; --faint:#4A5462;
    --primary:#F5F7FA; --secondary:#B4BECD; --muted:#8593A6;
    --access:#4DA3FF; --activity:#3DDC97; --social:#FF6B81;
    --ember:#FFB020; --gold:#FFC94D;
    --r:14px; --rl:20px;
    font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",system-ui,sans-serif;
  }
  *{box-sizing:border-box}
  body{margin:0;background:#07090C;color:var(--primary);-webkit-font-smoothing:antialiased}
  .page{max-width:1200px;margin:0 auto;padding:30px 16px 70px}
  h1{font-size:26px;letter-spacing:-.5px;margin:0 0 8px}
  .lede{color:var(--secondary);font-size:15px;line-height:1.55;max-width:66ch;margin:0 0 6px}
  .warn{color:var(--ember);font-size:13.5px;line-height:1.55;max-width:70ch;margin:10px 0 0}
  .cols{display:grid;grid-template-columns:392px 1fr;gap:30px;align-items:start;margin-top:26px}
  @media(max-width:860px){.cols{grid-template-columns:1fr}}

  /* ---------- the phone ---------- */
  .phone{width:392px;border:10px solid #1a1f29;border-radius:38px;overflow:hidden;
         background:var(--void);box-shadow:0 30px 70px rgba(0,0,0,.6)}
  .screen{height:780px;overflow-y:auto;background:var(--void);scrollbar-width:thin}
  .screen::-webkit-scrollbar{width:0}

  .herobar{display:flex;align-items:center;gap:11px;padding:16px 20px 12px}
  .ava{width:38px;height:38px;border-radius:12px;border:1px solid var(--border);
       background:var(--surface-high);flex:none;position:relative}
  .ava::after{content:"";position:absolute;inset:9px 11px;border-radius:50%;
              border:2px solid var(--access)}
  .herobar .t{flex:1;min-width:0}
  .herobar b{display:block;font-size:15px}
  .herobar small{color:var(--muted);font-size:12px}
  .pill{border:1px solid var(--border);border-radius:999px;padding:4px 10px;
        font-size:11.5px;color:var(--secondary);white-space:nowrap}
  .pill.gold{color:var(--gold);border-color:var(--gold)}

  .hero{position:relative;width:100%;aspect-ratio:3/2;display:block}
  .hero img{width:100%;height:100%;object-fit:cover;display:block}
  .hero .scrim{position:absolute;inset:0}

  .stageblock{padding:0 20px}
  .stagerow{display:flex;justify-content:space-between;align-items:flex-end;gap:12px}
  .stagerow h2{font-size:25px;letter-spacing:-.3px;margin:0;font-weight:700}
  .cap{color:var(--muted);font-size:12px}
  .track{height:5px;border-radius:3px;background:var(--well);
         border:1px solid var(--faint);margin:9px 0 7px;overflow:hidden}
  .track i{display:block;height:100%;background:var(--gold)}

  .ring{width:100%;aspect-ratio:1.7;padding:0 8px;margin-top:14px}
  .ring canvas{width:100%;height:100%;display:block}

  .strip{display:flex;gap:8px;padding:0 20px;margin-top:4px}
  .strip div{flex:1;border:1px solid var(--border);border-radius:var(--r);
             background:var(--surface);padding:9px 10px;display:flex;align-items:center;gap:7px}
  .dot{width:8px;height:8px;border-radius:50%;flex:none}
  .strip small{display:block;color:var(--muted);font-size:11px}
  .strip b{display:block;font-size:13px;font-weight:600}

  .sect{display:flex;justify-content:space-between;align-items:baseline;
        padding:22px 20px 10px}
  .sect h3{font-size:12px;color:var(--muted);text-transform:none;margin:0;font-weight:600}
  .sect span{font-size:12px;color:var(--muted)}
  .card{margin:0 20px 9px;border:1px solid var(--border);border-radius:var(--r);
        background:var(--surface);padding:13px 14px;display:flex;gap:11px;align-items:flex-start}
  .card .bar{width:3px;border-radius:2px;align-self:stretch;flex:none}
  .card b{display:block;font-size:14.5px;font-weight:600;line-height:1.4}
  .card small{display:block;color:var(--muted);font-size:12px;margin-top:4px}
  .tabs{display:flex;border-top:1px solid #1d232e;background:var(--void);
        position:sticky;bottom:0}
  .tabs div{flex:1;text-align:center;padding:12px 0 16px;font-size:11px;color:var(--muted)}
  .tabs div.on{color:var(--access)}

  /* ---------- the controls ---------- */
  .panel{background:var(--surface);border:1px solid var(--border);
         border-radius:var(--rl);padding:18px}
  .panel h3{margin:0 0 10px;font-size:14px}
  input[type=range]{width:100%;accent-color:var(--gold);height:28px}
  .ticks{display:flex;justify-content:space-between;color:var(--faint);font-size:11px}
  .ticks span{flex:1;text-align:center}
  .ticks span:first-child{text-align:left}.ticks span:last-child{text-align:right}
  .ticks .on{color:var(--gold)}
  .meta{color:var(--secondary);font-size:13.5px;line-height:1.6;margin:12px 0 0}
  .tag{display:inline-block;font-size:11px;padding:2px 8px;border-radius:999px;
       border:1px solid var(--border);color:var(--muted);margin-left:7px}
  .tag.derived{color:var(--ember);border-color:var(--ember)}
  .grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(188px,1fr));
        gap:11px;margin-top:16px}
  .grid figure{margin:0;border:1px solid var(--border);border-radius:var(--r);
               overflow:hidden;background:var(--surface);cursor:pointer}
  .grid figure.on{border-color:var(--gold)}
  .grid img{width:100%;display:block;aspect-ratio:3/2;object-fit:cover}
  .grid figcaption{padding:8px 10px 10px;font-size:12px}
  .grid figcaption small{display:block;color:var(--muted);font-size:11px;margin-top:3px}
  .note{color:var(--muted);font-size:12.5px;line-height:1.6;margin-top:16px}
  code{background:var(--well);padding:1px 5px;border-radius:4px;font-size:12px}
</style>
</head>
<body><div class="page">

<h1>Dashboard, redesigned</h1>
<p class="lede">
  The real WebP files from <code>res/drawable-nodpi</code>, the real stage data from
  <code>engine/Habitat.kt</code>, at phone width. Drag the slider or click a frame to change
  stage.
</p>
<p class="warn">
  Three of the seven images are <strong>derived, not generated</strong> — the Canva account
  hit its credit quota partway through the series, so "The street", "Open ground" and
  "Horizon" are composited from the two real renders either side of them. They are marked
  below.
</p>

<div class="cols">
  <div class="phone"><div class="screen">
    <div class="herobar">
      <div class="ava"></div>
      <div class="t"><b>Level 6</b><small>Day 23</small></div>
      <span class="pill gold">4 days</span>
      <span class="pill" id="p-rungs">26 cleared</span>
    </div>

    <div class="hero">
      <img id="hero-img" alt="">
      <div class="scrim" id="hero-scrim"></div>
    </div>

    <div class="stageblock">
      <div class="stagerow">
        <h2 id="p-label"></h2>
        <span class="cap" id="p-count"></span>
      </div>
      <div class="track"><i id="p-track"></i></div>
      <div class="cap" id="p-caption"></div>
    </div>

    <div class="ring"><canvas id="ring" width="360" height="212"></canvas></div>

    <div class="strip">
      <div><span class="dot" style="background:var(--access)"></span>
           <span><small>Access</small><b style="color:var(--access)">Lv 12</b></span></div>
      <div><span class="dot" style="background:var(--activity)"></span>
           <span><small>Activity</small><b style="color:var(--activity)">Lv 9</b></span></div>
      <div><span class="dot" style="background:var(--social)"></span>
           <span><small>Social</small><b style="color:var(--social)">Lv 5</b></span></div>
    </div>

    <div class="sect"><h3>Today</h3><span>2 left</span></div>
    <div class="card"><span class="bar" style="background:var(--access)"></span>
      <span><b>Turn down one street you have never turned down. One block, then home.</b>
      <small>When I next reach the corner of my own street · 14 min · Nerve 2/10</small></span></div>
    <div class="card"><span class="bar" style="background:var(--activity)"></span>
      <span><b>End your shower with thirty seconds of cold.</b>
      <small>When I turn the shower off · 1 min · Nerve 2/10</small></span></div>
    <div class="card" style="opacity:.55"><span class="bar" style="background:var(--social)"></span>
      <span><b>Ask one stranger a question with a factual answer.</b>
      <small>Done today</small></span></div>

    <div class="tabs"><div class="on">Today</div><div>Learn</div><div>Notes</div><div>You</div></div>
  </div></div>

  <div>
    <div class="panel">
      <h3>Stage <span id="s-num"></span> of 7<span class="tag" id="s-origin"></span></h3>
      <input type="range" id="slider" min="0" max="6" value="2" step="1">
      <div class="ticks" id="ticks"></div>
      <p class="meta" id="s-note"></p>
      <p class="note">
        Honest about what this page cannot show: the ring below the hero is a static
        approximation (the real one animates and responds to touch), there are no system
        insets, and nothing here is a real touch target. Everything above the ring — the
        artwork, the scrim, the type scale, the spacing — is a faithful transcription of the
        Compose code.
      </p>
    </div>
    <div class="grid" id="grid"></div>
  </div>
</div>

</div>
<script>
const STAGES = __STAGES__;
const slider = document.getElementById('slider');

/* The scrim's stops must match HabitatHero in ui/components/Habitat.kt and SCRIM_TOP /
   SCRIM_END in prepare_habitat_art.py. It completes at 96%, not 100%: a gradient that only
   reaches full opacity at the last row never gets there, and the hero ends on a hairline. */
function scrim(ground) {
  return `linear-gradient(to bottom, transparent 0%, transparent 70%, ${ground} 96%, ${ground} 100%)`;
}

/* A static stand-in for BubbleRing: three overlapping organic perimeters, one per pillar. */
function drawRing(cv) {
  const ctx = cv.getContext('2d');
  const dpr = window.devicePixelRatio || 1;
  const w = cv.clientWidth, h = cv.clientHeight;
  cv.width = w * dpr; cv.height = h * dpr;
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.clearRect(0, 0, w, h);
  const blobs = [
    ['#4DA3FF', w * 0.42, h * 0.52, Math.min(w, h) * 0.40],
    ['#3DDC97', w * 0.56, h * 0.42, Math.min(w, h) * 0.31],
    ['#FF6B81', w * 0.57, h * 0.60, Math.min(w, h) * 0.25],
  ];
  for (const [col, cx, cy, r] of blobs) {
    ctx.beginPath();
    for (let i = 0; i <= 96; i++) {
      const a = (i / 96) * Math.PI * 2;
      const rr = r * (1 + 0.055 * Math.sin(a * 3) + 0.03 * Math.sin(a * 5 + 1.1));
      const x = cx + Math.cos(a) * rr, y = cy + Math.sin(a) * rr;
      i ? ctx.lineTo(x, y) : ctx.moveTo(x, y);
    }
    ctx.closePath();
    ctx.fillStyle = col + '14'; ctx.fill();
    ctx.strokeStyle = col; ctx.lineWidth = 1.6; ctx.stroke();
  }
}

function caption(i) {
  const next = STAGES[i + 1];
  if (!next) return 'The room is fully open. Nothing left to unseal.';
  const gap = next.threshold - STAGES[i].threshold;
  return `${gap} more rungs to ${next.label.toLowerCase()}.`;
}

function show(i) {
  const s = STAGES[i];
  document.getElementById('hero-img').src = s.data;
  document.getElementById('hero-img').alt = s.label + '. ' + s.note;
  document.getElementById('hero-scrim').style.background = scrim(s.ground);
  document.getElementById('p-label').textContent = s.label;
  document.getElementById('p-count').textContent = s.threshold + ' rungs';
  document.getElementById('p-rungs').textContent = s.threshold + ' cleared';
  document.getElementById('p-track').style.width = Math.round(100 * i / 6) + '%';
  document.getElementById('p-caption').textContent = caption(i);
  document.getElementById('s-num').textContent = i + 1;
  document.getElementById('s-note').textContent = s.note;
  const tag = document.getElementById('s-origin');
  tag.textContent = s.origin === 'derived' ? 'derived, not generated' : 'generated';
  tag.className = 'tag' + (s.origin === 'derived' ? ' derived' : '');
  document.querySelectorAll('#ticks span').forEach((e, k) => e.classList.toggle('on', k === i));
  document.querySelectorAll('.grid figure').forEach((e, k) => e.classList.toggle('on', k === i));
  document.body.style.setProperty('--void', s.ground);
}

document.getElementById('ticks').innerHTML =
  STAGES.map(s => `<span>${s.threshold}</span>`).join('');
document.getElementById('grid').innerHTML = STAGES.map((s, i) => `
  <figure data-i="${i}">
    <img src="${s.data}" alt="${s.label}">
    <figcaption>${s.label}<small>${s.threshold === 0 ? 'Start' : s.threshold + ' rungs'} ·
      ${s.origin}</small></figcaption>
  </figure>`).join('');
document.querySelectorAll('.grid figure').forEach(f =>
  f.addEventListener('click', () => { slider.value = f.dataset.i; show(+f.dataset.i); }));
slider.addEventListener('input', () => show(+slider.value));

drawRing(document.getElementById('ring'));
show(+slider.value);
window.addEventListener('resize', () => drawRing(document.getElementById('ring')));
</script>
</body></html>
"""


def main():
    found = stages()
    if len(found) != 7:
        print(f"parsed {len(found)} stages, expected 7")
        return 1
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(HTML.replace("__STAGES__", json.dumps(found)))
    kb = OUT.stat().st_size // 1024
    print(f"wrote {OUT} ({kb} KB, artwork embedded)")
    for s in found:
        print(f"  {s['label']:13} {s['threshold']:>3} rungs  {s['origin']:9} {s['file']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
