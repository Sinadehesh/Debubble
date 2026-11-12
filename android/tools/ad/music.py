"""The advert's music: eight bars of soft chiptune, written here, so there is nothing to license.

The app's own sounds are synthesised (audio/Synth.kt), so the advert's music is too: a
triangle bass, a round pulse lead, a quiet arpeggio and brushed noise drums, at 104 BPM in
F major, I-V-vi-IV and then I-V-IV-I, ending on a held F chord for the end card.

Run:  python3 music.py out.wav
"""

import sys
import wave

import numpy as np

RATE = 44100
BPM = 104
BEAT = 60.0 / BPM
BAR = BEAT * 4
BARS = 8
TAIL = 1.6                          # the last chord rings under the end card


def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def tri(f, t):
    return 2 * np.abs(2 * ((f * t) % 1.0) - 1) - 1


def pulse(f, t, duty):
    return np.where((f * t) % 1.0 < duty, 1.0, -1.0)


def env(n, attack, decay, sustain, release, hold):
    """A linear ADSR over n samples, with the note held for `hold` seconds."""
    t = np.arange(n) / RATE
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    d = np.where(t < attack, 1.0, sustain + (1 - sustain) * np.exp(-(t - attack) / max(decay, 1e-4)))
    r = np.where(t < hold, 1.0, np.clip(1 - (t - hold) / max(release, 1e-4), 0, 1))
    return a * d * r


def lowpass(x, cutoff):
    """One-pole low-pass: takes the fizz off square waves so they sit softly."""
    k = np.exp(-2 * np.pi * cutoff / RATE)
    y = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc = (1 - k) * x[i] + k * acc
        y[i] = acc
    return y


def place(track, start, sig):
    i = int(start * RATE)
    j = min(len(track), i + len(sig))
    track[i:j] += sig[:j - i]


def note(f, length, kind, vol, duty=0.25, attack=0.005, decay=0.12, sustain=0.6, release=0.08):
    n = int((length + release) * RATE)
    t = np.arange(n) / RATE
    if kind == "tri":
        w = tri(f, t)
    else:
        w = pulse(f, t, duty)
    return vol * w * env(n, attack, decay, sustain, release, length)


# F major: I V vi IV. Roots in the bass octave, and the chord tones for the arpeggio.
CHORDS = [
    (41, [65, 69, 72]),    # F   (F A C)
    (36, [64, 67, 72]),    # C   (E G C)
    (38, [65, 69, 74]),    # Dm  (F A D)
    (34, [65, 70, 74]),    # Bb  (F Bb D)
]

# Bar by bar: F C Dm Bb, then F C Bb F, so it comes home to F (IV to I) for the end card.
PROGRESSION = [0, 1, 2, 3, 0, 1, 3, 0]

# The tune, as (bar, beat, midi, beats long). A rising four-note call, answered, twice;
# the second time it climbs to the top F, the way the room opens.
TUNE = [
    (0, 0, 72, 1), (0, 1, 74, 0.5), (0, 1.5, 76, 0.5), (0, 2, 77, 1.5),
    (1, 0, 76, 1), (1, 1, 72, 1), (1, 2, 67, 2),
    (2, 0, 69, 1), (2, 1, 72, 0.5), (2, 1.5, 74, 0.5), (2, 2, 76, 1.5),
    (3, 0, 74, 1), (3, 1, 72, 1), (3, 2, 70, 1), (3, 3, 69, 1),
    (4, 0, 72, 1), (4, 1, 74, 0.5), (4, 1.5, 76, 0.5), (4, 2, 77, 1.5),
    (5, 0, 79, 1), (5, 1, 76, 1), (5, 2, 72, 2),
    (6, 0, 74, 1), (6, 1, 77, 0.5), (6, 1.5, 81, 0.5), (6, 2, 79, 1.5),
    (7, 0, 77, 1), (7, 1, 76, 0.5), (7, 1.5, 74, 0.5), (7, 2, 77, 2),
]


def noise(n, seed):
    return np.random.default_rng(seed).uniform(-1, 1, n)


def render():
    total = BARS * BAR + TAIL
    n = int(total * RATE)
    bass = np.zeros(n)
    lead = np.zeros(n)
    arp = np.zeros(n)
    drums = np.zeros(n)

    for bar in range(BARS):
        root, tones = CHORDS[PROGRESSION[bar]]
        last = bar == BARS - 1
        t0 = bar * BAR
        # Bass: root on 1, the octave on the "and" of 2, root on 3, fifth on 4.
        pattern = [(0, root, 1.0), (1.5, root + 12, 0.5), (2, root, 1.0), (3, root + 7, 1.0)]
        if last:
            pattern = [(0, root, 4.0 + TAIL / BEAT)]
        for beat, m, beats in pattern:
            place(bass, t0 + beat * BEAT, note(midi(m), beats * BEAT * 0.92, "tri", 0.55,
                                                decay=0.3, sustain=0.75, release=0.06))
        # Arpeggio: chord tones up and down in sixteenths, an octave up, very quiet.
        if not last:
            seq = tones + [tones[2] + 12] + tones[::-1][1:] + [tones[0] + 12]
            for k in range(16):
                m = seq[k % len(seq)] + 12
                place(arp, t0 + k * BEAT / 4, note(midi(m), BEAT / 4 * 0.7, "pulse", 0.10,
                                                   duty=0.125, decay=0.05, sustain=0.3, release=0.03))
        else:
            for k, m in enumerate(tones + [tones[0] + 12]):
                place(arp, t0 + k * BEAT / 4, note(midi(m + 12), 4 * BEAT + TAIL - k * BEAT / 4, "pulse",
                                                   0.09, duty=0.125, decay=0.6, sustain=0.35, release=0.9))
        # Drums: a soft kick on 1 and 3, a brushed snare on 2 and 4, hats on the offbeats.
        if not last:
            for beat in (0, 2):
                k = int(0.16 * RATE)
                tt = np.arange(k) / RATE
                f = 110 * np.exp(-tt * 28) + 45
                kick = 0.65 * np.sin(2 * np.pi * np.cumsum(f) / RATE) * np.exp(-tt * 22)
                place(drums, t0 + beat * BEAT, kick)
            for beat in (1, 3):
                k = int(0.12 * RATE)
                tt = np.arange(k) / RATE
                place(drums, t0 + beat * BEAT, 0.16 * noise(k, bar * 10 + beat) * np.exp(-tt * 30))
            for beat in (0.5, 1.5, 2.5, 3.5):
                k = int(0.04 * RATE)
                tt = np.arange(k) / RATE
                place(drums, t0 + beat * BEAT, 0.07 * noise(k, bar * 20 + int(beat * 2)) * np.exp(-tt * 90))
        else:
            k = int(0.9 * RATE)
            tt = np.arange(k) / RATE
            place(drums, t0, 0.12 * noise(k, 99) * np.exp(-tt * 5))          # a soft cymbal

    for bar, beat, m, beats in TUNE:
        hold = beats * BEAT * 0.9
        if bar == BARS - 1 and beat == 2:
            hold = 2 * BEAT + TAIL * 0.7
        place(lead, bar * BAR + beat * BEAT, note(midi(m), hold, "pulse", 0.20, duty=0.25,
                                                   attack=0.01, decay=0.25, sustain=0.55, release=0.18))

    lead = lowpass(lead, 3200)
    arp = lowpass(arp, 4200)
    mix = 0.9 * bass + lead + arp + drums
    # A little room: two echoes of the lead and arpeggio, quieter each time.
    echo = np.zeros(n)
    for delay, gain in ((0.75 * BEAT, 0.22), (1.5 * BEAT, 0.10)):
        d = int(delay * RATE)
        echo[d:] += gain * (lead + arp)[:-d]
    mix += echo
    # Fade in over the first few milliseconds and out over the tail.
    mix[: int(0.01 * RATE)] *= np.linspace(0, 1, int(0.01 * RATE))
    fade = int(TAIL * RATE)
    mix[-fade:] *= np.linspace(1, 0, fade) ** 1.5
    mix /= max(1e-9, np.max(np.abs(mix)))
    return 0.85 * mix


def write(path, mix):
    stereo = np.stack([mix, mix], axis=1)
    data = (np.clip(stereo, -1, 1) * 32767).astype("<i2")
    with wave.open(str(path), "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(data.tobytes())


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "music.wav"
    write(out, render())
    print("wrote", out, f"{BARS * BAR + TAIL:.2f}s")
