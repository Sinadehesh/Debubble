#!/usr/bin/env python3
"""Build the interactive room page, with the shipped assets embedded.

Reads the same files the app does — the seven room shells, the item atlas and items.json —
and inlines them as data URIs, so the page cannot drift from what Android draws. Total
payload is a few KB, which is the thing pixel art buys you.

Run:  python3 android/tools/pixel/make_page.py
"""

import base64
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "pixel"
OUT = pathlib.Path(__file__).resolve().parent.parent / "preview" / "room.html"


def b64(path):
    return "data:image/webp;base64," + base64.b64encode(path.read_bytes()).decode()


def main():
    manifest = json.loads((ASSETS / "items.json").read_text())
    data = {
        "room": manifest["room"],
        "items": manifest["items"],
        "atlas": b64(ASSETS / "items.webp"),
        "stages": [],
    }
    # Stage labels and thresholds come from the engine, so the page and the app agree.
    hab = (ROOT / "app" / "src" / "main" / "java" / "com" / "debubble" / "app"
           / "engine" / "Habitat.kt").read_text()
    import re
    found = re.findall(
        r"threshold\s*=\s*(\d+),\s*label\s*=\s*\"([^\"]+)\",\s*note\s*=\s*\"([^\"]+)\"",
        hab, re.S)
    if len(found) != len(manifest["stages"]):
        print(f"Habitat.kt has {len(found)} stages, assets have {len(manifest['stages'])}")
        return 1
    for (threshold, label, note), key in zip(found, manifest["stages"]):
        data["stages"].append({
            "key": key, "threshold": int(threshold), "label": label, "note": note,
            "img": b64(DRAWABLE / f"room_{key}.webp"),
        })

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(TEMPLATE.replace("__DATA__", json.dumps(data, separators=(",", ":"))))
    print(f"wrote {OUT} ({OUT.stat().st_size // 1024} KB)")
    for s in data["stages"]:
        print(f"  {s['threshold']:>3} rungs  {s['label']}")
    return 0


TEMPLATE = r"""<title>Debubble Room</title>
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Press+Start+2P&family=Nunito:wght@400;600;700&display=swap">
<style>
/* Layout: one column — room, then the tray you drag out of, then the progress control.
   Committed single theme. This is a sunny room seen through a dark console surround; a
   light version of that is not the same object, so there are no theme blocks, only
   explicit colour. */
:root{
  --ink:#1B1522; --ink-2:#251D2E; --ink-3:#332940;
  --line:#473A55; --text:#F6EFE4; --dim:#B8A7BE;
  --wood:#C88B4E; --glow:#FFBF4D; --leaf:#63C04C; --clay:#C2603A; --sky:#86CCF2;
  --display:"Press Start 2P",ui-monospace,monospace;
  --body:"Nunito",-apple-system,BlinkMacSystemFont,"Segoe UI",system-ui,sans-serif;
  color-scheme:dark;
}
*{box-sizing:border-box}
body{margin:0;background:var(--ink);color:var(--text);font-family:var(--body);font-size:15px;line-height:1.6}
.wrap{max-width:860px;margin:0 auto;padding-block:26px 64px;
      padding-left:max(16px,env(safe-area-inset-left,0px));
      padding-right:max(16px,env(safe-area-inset-right,0px))}
h1{font-family:var(--display);font-size:17px;line-height:1.5;margin:0 0 14px;color:var(--glow);text-wrap:balance}
p.lede{margin:0 0 22px;color:var(--dim);max-width:62ch}
p.lede b{color:var(--text);font-weight:700}

.stage{display:flex;align-items:baseline;justify-content:space-between;gap:12px;flex-wrap:wrap;margin-bottom:10px}
.stage h2{font-family:var(--display);font-size:13px;margin:0;color:var(--text)}
.stage .rungs{font-family:var(--display);font-size:10px;color:var(--glow)}
.note{color:var(--dim);font-size:14px;margin:0 0 14px;min-height:2.6em}

.frame{background:var(--ink-2);border:2px solid var(--line);border-radius:10px;padding:10px;position:relative}
canvas{display:block;width:100%;height:auto;image-rendering:pixelated;border-radius:4px;touch-action:none;cursor:grab}
canvas.dragging{cursor:grabbing}
.hint{position:absolute;inset:auto 0 14px;text-align:center;font-size:12px;color:#fff;
      text-shadow:0 1px 3px rgba(0,0,0,.9);pointer-events:none;opacity:.9}

.bar{display:flex;gap:10px;align-items:center;flex-wrap:wrap;margin:16px 0 8px}
button{font-family:var(--body);font-weight:700;font-size:14px;color:var(--text);
       background:var(--ink-3);border:2px solid var(--line);border-radius:8px;
       padding:9px 15px;cursor:pointer;min-height:44px}
button:hover{border-color:var(--glow)}
button:focus-visible{outline:3px solid var(--glow);outline-offset:2px}
.count{color:var(--dim);font-size:13.5px;margin-left:auto;font-variant-numeric:tabular-nums}

h3{font-family:var(--display);font-size:11px;margin:26px 0 12px;color:var(--dim)}
.tray{display:grid;grid-template-columns:repeat(auto-fill,minmax(96px,1fr));gap:9px}
.slot{background:var(--ink-2);border:2px solid var(--line);border-radius:10px;padding:9px 6px 8px;
      text-align:center;cursor:grab;touch-action:none;display:flex;flex-direction:column;
      align-items:center;gap:6px;min-height:104px;justify-content:flex-end}
.slot:hover{border-color:var(--glow)}
.slot canvas{width:auto;max-height:52px;image-rendering:pixelated;pointer-events:none}
.slot .nm{font-size:12px;font-weight:600;line-height:1.25}
.slot.locked{opacity:.42;cursor:not-allowed;border-style:dashed}
.slot.locked:hover{border-color:var(--line)}
.slot .req{font-family:var(--display);font-size:8px;color:var(--glow);line-height:1.6}
.slot.nowall .req{color:var(--sky)}

.prog{margin-top:28px;background:var(--ink-2);border:2px solid var(--line);border-radius:10px;padding:16px}
.prog label{display:block;font-size:13.5px;color:var(--dim);margin-bottom:10px}
.prog output{font-family:var(--display);font-size:11px;color:var(--glow)}
input[type=range]{width:100%;accent-color:var(--glow);height:36px}
.ticks{display:flex;justify-content:space-between;font-family:var(--display);font-size:8px;color:#6D5C78}

.foot{margin-top:30px;color:var(--dim);font-size:13.5px;max-width:66ch}
.foot code{background:var(--ink-3);padding:2px 6px;border-radius:4px;font-size:12.5px}
</style>

<div class="wrap">
  <h1>Your room</h1>
  <p class="lede">
    It opens up as you clear rungs, and you furnish it with things you have earned.
    <b>Drag anything out of the tray into the room.</b> Drag it again to move it, or drag it
    back out to put it away. Lower in the room means nearer to you.
  </p>

  <div class="stage">
    <h2 id="s-label"></h2>
    <span class="rungs" id="s-rungs"></span>
  </div>
  <p class="note" id="s-note"></p>

  <div class="frame">
    <canvas id="room" width="640" height="400"></canvas>
    <div class="hint" id="hint">Drag something in from below</div>
  </div>

  <div class="bar">
    <button id="clear" type="button">Clear the room</button>
    <button id="demo" type="button">Furnish it for me</button>
    <span class="count" id="count"></span>
  </div>

  <h3>Your things</h3>
  <div class="tray" id="tray"></div>

  <div class="prog">
    <label for="rungs">Rungs cleared <output id="out"></output></label>
    <input type="range" id="rungs" min="0" max="260" step="1" value="24">
    <div class="ticks" id="ticks"></div>
  </div>

  <p class="foot">
    Everything here is the artwork the app ships: the seven room shells from
    <code>res/drawable-nodpi</code> and one <code>items.webp</code> atlas, inlined. The whole
    set is about 6&nbsp;KB. Placement, depth order and the unlock rules are the same ones the
    app uses.
  </p>
</div>

<script>
const D = __DATA__;
const R = D.room;

/* ---------- assets ---------- */
const atlas = new Image(); atlas.src = D.atlas;
const shells = D.stages.map(s => { const i = new Image(); i.src = s.img; return i; });
const byId = Object.fromEntries(D.items.map(i => [i.id, i]));

/* ---------- state ---------- */
let rungs = 24;
let placed = [];                 // {id, x, baseline}
let drag = null;                 // {id, from, ox, oy, px, py, inside}

const cv = document.getElementById('room');
const ctx = cv.getContext('2d');
ctx.imageSmoothingEnabled = false;

function stageIndex(n){
  let k = 0;
  D.stages.forEach((s, i) => { if (n >= s.threshold) k = i; });
  return k;
}
function unlocked(item){
  // Only rungs are simulated here; the app also grants on level and friction, which track
  // roughly with rungs, so the slider stands in for all three.
  return rungs >= item.unlock.amount;
}
function wallExists(){ return D.stages[stageIndex(rungs)].key !== 'unwalled'; }
function placeable(item){ return unlocked(item) && (item.anchor === 'floor' || wallExists()); }

/* ---------- drawing ---------- */
function scale(){ return Math.max(2, Math.floor(cv.width / R.w)); }

function drawSprite(c, item, x, y, alpha){
  c.save();
  if (alpha != null) c.globalAlpha = alpha;
  c.drawImage(atlas, item.x, item.y, item.w, item.h, x, y, item.w, item.h);
  c.restore();
}

function render(){
  const z = scale();
  cv.width = R.w * z; cv.height = R.h * z;
  ctx.imageSmoothingEnabled = false;
  ctx.setTransform(z, 0, 0, z, 0, 0);
  ctx.clearRect(0, 0, R.w, R.h);

  const si = stageIndex(rungs);
  if (shells[si].complete) ctx.drawImage(shells[si], 0, 0, R.w, R.h);

  // Wall things first, then floor things back to front, so nearer items cover further ones.
  const wall = placed.filter(p => byId[p.id].anchor === 'wall');
  const floor = placed.filter(p => byId[p.id].anchor === 'floor')
                      .sort((a, b) => a.baseline - b.baseline);
  for (const p of [...wall, ...floor]) {
    const it = byId[p.id];
    drawSprite(ctx, it, p.x, p.baseline - it.h, null);
  }

  if (drag && drag.inside) {
    const it = byId[drag.id];
    drawSprite(ctx, it, drag.px, drag.py - it.h, 0.65);
  }
  ctx.setTransform(1, 0, 0, 1, 0, 0);

  document.getElementById('hint').hidden = placed.length > 0 || !!drag;
  document.getElementById('count').textContent =
    placed.length === 0 ? 'nothing placed yet'
    : placed.length + (placed.length === 1 ? ' thing placed' : ' things placed');
}

/* ---------- pointer ---------- */
function toRoom(ev){
  const r = cv.getBoundingClientRect();
  return {
    x: Math.round((ev.clientX - r.left) / r.width * R.w),
    y: Math.round((ev.clientY - r.top) / r.height * R.h),
  };
}
function clampTo(item, x, y){
  const minB = item.anchor === 'floor' ? R.floorY + 2 : item.h + 2;
  const maxB = item.anchor === 'floor' ? R.h - 1 : R.floorY - R.skirt - 1;
  return {
    x: Math.max(0, Math.min(R.w - item.w, x - Math.floor(item.w / 2))),
    baseline: Math.max(minB, Math.min(maxB, y)),
  };
}
function hitTest(pt){
  const order = [...placed].reverse();
  for (const p of order) {
    const it = byId[p.id];
    if (pt.x >= p.x && pt.x < p.x + it.w && pt.y >= p.baseline - it.h && pt.y <= p.baseline) {
      return p;
    }
  }
  return null;
}

function beginDrag(id, from, ev){
  drag = { id, from, px: 0, py: 0, inside: false };
  moveDrag(ev);
  cv.classList.add('dragging');
}
function moveDrag(ev){
  if (!drag) return;
  const r = cv.getBoundingClientRect();
  const inside = ev.clientX >= r.left && ev.clientX <= r.right
              && ev.clientY >= r.top && ev.clientY <= r.bottom;
  drag.inside = inside;
  if (inside) {
    const pt = toRoom(ev);
    const c = clampTo(byId[drag.id], pt.x, pt.y);
    drag.px = c.x; drag.py = c.baseline;
  }
  render();
}
function endDrag(){
  if (!drag) return;
  if (drag.inside) {
    if (drag.from !== null) placed[drag.from] = { id: drag.id, x: drag.px, baseline: drag.py };
    else placed.push({ id: drag.id, x: drag.px, baseline: drag.py });
  } else if (drag.from !== null) {
    placed.splice(drag.from, 1);              // dragged out of the room: put it away
  }
  drag = null;
  cv.classList.remove('dragging');
  render();
}

cv.addEventListener('pointerdown', ev => {
  const hit = hitTest(toRoom(ev));
  if (!hit) return;
  cv.setPointerCapture(ev.pointerId);
  beginDrag(hit.id, placed.indexOf(hit), ev);
  ev.preventDefault();
});
window.addEventListener('pointermove', moveDrag);
window.addEventListener('pointerup', endDrag);
window.addEventListener('pointercancel', endDrag);

/* ---------- the tray ---------- */
function thumb(item){
  const c = document.createElement('canvas');
  const z = 3;
  c.width = item.w * z; c.height = item.h * z;
  const x = c.getContext('2d');
  x.imageSmoothingEnabled = false;
  const paint = () => x.drawImage(atlas, item.x, item.y, item.w, item.h, 0, 0, c.width, c.height);
  atlas.complete ? paint() : atlas.addEventListener('load', paint, { once: true });
  return c;
}

function buildTray(){
  const tray = document.getElementById('tray');
  tray.replaceChildren();
  for (const item of D.items) {
    const slot = document.createElement('div');
    const ok = placeable(item);
    const noWall = unlocked(item) && item.anchor === 'wall' && !wallExists();
    slot.className = 'slot' + (ok ? '' : ' locked') + (noWall ? ' nowall' : '');
    slot.append(thumb(item));
    const nm = document.createElement('div');
    nm.className = 'nm'; nm.textContent = item.label;
    slot.append(nm);
    const req = document.createElement('div');
    req.className = 'req';
    req.textContent = noWall ? 'no wall left'
      : ok ? '' : item.unlock.amount + ' ' + item.unlock.currency;
    slot.append(req);
    if (ok) {
      slot.addEventListener('pointerdown', ev => { beginDrag(item.id, null, ev); ev.preventDefault(); });
    }
    tray.append(slot);
  }
}

/* ---------- progress ---------- */
function syncStage(){
  const s = D.stages[stageIndex(rungs)];
  document.getElementById('s-label').textContent = s.label;
  document.getElementById('s-rungs').textContent = s.threshold === 0 ? 'from day one' : s.threshold + ' rungs';
  document.getElementById('s-note').textContent = s.note;
  document.getElementById('out').textContent = rungs;
  // Anything hanging on a wall that no longer exists comes down.
  if (!wallExists()) placed = placed.filter(p => byId[p.id].anchor === 'floor');
  buildTray();
  render();
}

const slider = document.getElementById('rungs');
slider.addEventListener('input', () => { rungs = +slider.value; syncStage(); });
document.getElementById('ticks').innerHTML =
  D.stages.map(s => '<span>' + s.threshold + '</span>').join('');
document.getElementById('clear').addEventListener('click', () => { placed = []; render(); });
document.getElementById('demo').addEventListener('click', () => {
  const pick = D.items.filter(placeable);
  placed = [];
  let i = 0;
  for (const it of pick) {
    const floor = it.anchor === 'floor';
    const c = clampTo(it, 14 + (i * 37) % (R.w - 20), floor ? 70 + (i * 13) % 28 : 20 + (i * 9) % 22);
    placed.push({ id: it.id, x: c.x, baseline: c.baseline });
    i++;
  }
  render();
});

let ready = 0;
const go = () => { if (++ready >= 2) { syncStage(); } };
atlas.complete ? go() : atlas.addEventListener('load', go, { once: true });
let loaded = 0;
shells.forEach(s => {
  const done = () => { if (++loaded === shells.length) go(); };
  s.complete ? done() : s.addEventListener('load', done, { once: true });
});
window.addEventListener('resize', render);
</script>
"""

if __name__ == "__main__":
    sys.exit(main())
