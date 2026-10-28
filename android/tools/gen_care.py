#!/usr/bin/env python3
"""Write the care library: the daily routines that keep the body, the room and the heart clean.

These are the most important tasks in the app, so they are held to the strictest rules in it.

WHY ROUTINES AND NOT A LADDER

The three pillar ladders serve something new every time, and for exposure that is right:
inhibitory learning needs a prediction to be wrong, and a prediction can only be wrong about
something you have not done yet. Habits are the opposite. Automaticity is built by repeating
the *same* behaviour in the *same* context (Lally et al., 2010, European Journal of Social
Psychology; Wood & Rünger, 2016, Annual Review of Psychology), so a care task that changed
every day would never become a habit at all. Each domain therefore has a small library of
routines; the user keeps one as theirs, at one cue, and the thing that varies is its size.

THE SHAPE OF EVERY ROUTINE

  tiny / standard / deep   Three sizes of the same act. Tiny is two minutes or less (Fogg's
                           Tiny Habits; Clear's two-minute rule) and counts fully toward the
                           habit, because repetition is what is being built, not volume.
                           Behavioural activation's graded task assignment is the same idea
                           from the clinic: when energy is low, the step gets smaller, it does
                           not get skipped.
  why / source             The mechanism in plain words, and where it comes from. Modest where
                           the evidence is modest, and it says so.
  ease                     One change to the surroundings that makes the act easier. Friction
                           is a stronger lever on behaviour than intention (Wood & Neal), and
                           this is the field people actually act on.
  anchors                  Moments that happen anyway, for the if-then plan (Gollwitzer &
                           Sheeran, 2006, meta-analysis, d = .65). Each must pass the same
                           specificity test the app applies to the user's own cues.
  celebrate                Fogg: the feeling has to land immediately, so it is an act, not a
                           badge.

Run:  python3 android/tools/gen_care.py
"""

import json
import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
OUT = HERE.parent / "app" / "src" / "main" / "assets" / "care" / "routines.json"

# The rules the engine runs on, written into the asset so the web preview runs the same
# numbers. CareTest asserts these match the Kotlin constants, so neither can drift alone.
RULES = {
    # Days without care before a domain slips one level. A body needs daily care, a room
    # and a heart accumulate more slowly.
    "daysPerLevel": {"BODY": 1, "ROOM": 2, "HEART": 2},
    "maxDirt": 3,
    # Care-days in a domain at which something new arrives for the room. The last is the
    # median days-to-automaticity in Lally et al. (2010). Timing is fixed and shown, so it
    # reads as a goal rather than a slot machine; which item arrives is the surprise.
    "rewardAt": [1, 3, 6, 10, 15, 21, 30, 45, 66],
    "automaticityDays": 66,
}

DOMAINS = [
    {
        "id": "BODY",
        "name": "Body",
        "blurb": "Washing, water, light, movement, sleep, food. The basics are the first "
                 "thing to go when life gets heavy, and the first thing that helps.",
        "levels": ["Fresh", "Fine", "Running low", "Needs care"],
        "checkin": [
            "Washed, fed and rested",
            "Mostly looked after",
            "Skipped a few basics",
            "It has been a while",
        ],
    },
    {
        "id": "ROOM",
        "name": "Room",
        "blurb": "The space you live in. Clutter quietly costs attention and calm, and a "
                 "reset takes minutes, not a weekend.",
        "levels": ["Sparkling", "Lived-in", "Cluttered", "Messy"],
        "checkin": [
            "Clear and calm",
            "A few things out",
            "Piles building up",
            "Hard to see the floor",
        ],
    },
    {
        "id": "HEART",
        "name": "Heart",
        "blurb": "What you are carrying. Feelings need tending the way rooms do: named, "
                 "aired, and shared.",
        "levels": ["Sunny", "Okay", "Cloudy", "Stormy"],
        "checkin": [
            "Light and clear",
            "A bit on my mind",
            "Heavy, carrying stuff",
            "A lot going on",
        ],
    },
]


def r(domain, id, title, tiny, standard, deep, mins, why, source, ease, anchors, celebrate,
      starter=False):
    return {
        "id": id, "domain": domain, "title": title,
        "tiny": tiny, "standard": standard, "deep": deep,
        "tinyMin": mins[0], "standardMin": mins[1], "deepMin": mins[2],
        "why": why, "source": source, "ease": ease,
        "anchors": anchors, "celebrate": celebrate, "starter": starter,
    }


ROUTINES = [
    # ================================================================== BODY
    r("BODY", "wash", "Wash up",
      "Brush your teeth and splash your face with cold water.",
      "Have a shower. Getting in is the whole battle, and quick counts.",
      "Shower, put on clean clothes, and change your pillowcase.",
      (2, 10, 25),
      "When energy is low, looking after the body is usually the first thing to go, and "
      "going without quietly feeds the low feeling. Behavioural activation rebuilds these "
      "routine activities first, because action tends to come before motivation, not after.",
      "Martell, Dimidjian & Herman-Dunn, 2010, Behavioral Activation for Depression; "
      "Cuijpers et al., 2007, Clinical Psychology Review (meta-analysis)",
      "Put your towel and tomorrow's clothes out tonight, so the first step is already done.",
      ["I get out of bed in the morning", "I get home in the evening",
       "I put my phone on charge for the night"],
      "Look in the mirror and say \"better\". You are.",
      starter=True),

    r("BODY", "water", "Water first",
      "Drink one full glass of water.",
      "A glass now, and a filled bottle that comes with you for the day.",
      "Water now, a bottle with you, and a real meal eaten sitting down.",
      (1, 3, 20),
      "Even mild dehydration, around 1.5% of body weight, lowered mood and made tasks feel "
      "harder in controlled studies. It is the cheapest fix in this whole app.",
      "Armstrong et al., 2012, Journal of Nutrition",
      "Leave a full glass where you will see it first: by the bed, or by the kettle.",
      ["I get out of bed in the morning", "I switch the kettle on in the morning",
       "I sit down where I work"],
      "Put the glass down with a clink. One done."),

    r("BODY", "light", "Morning light",
      "Open the curtains and stand in the window light for two minutes.",
      "Ten minutes outside in daylight within an hour of waking.",
      "A twenty-minute walk outside in the morning, phone in your pocket.",
      (2, 10, 20),
      "Morning light is the strongest signal your body clock gets: it sets when you feel "
      "alert and when you get sleepy that night. Bright light treatment helps depression, "
      "not only the seasonal kind.",
      "Golden et al., 2005, American Journal of Psychiatry (meta-analysis)",
      "Open the curtains before you pick up your phone. Leave your shoes by the door.",
      ["I get out of bed in the morning", "I put my coat on to go out",
       "I finish my first drink of the day"],
      "Face the light, eyes closed, one slow breath. That is the reward."),

    r("BODY", "move", "Move a little",
      "Move for two minutes: dance, stretch, march on the spot. Anything.",
      "A fifteen-minute walk at a pace that warms you up.",
      "Thirty minutes: a brisk walk, a jog, yoga, or some strength work.",
      (2, 15, 30),
      "Exercise works as a treatment for depression. A 2024 analysis of 218 trials found "
      "walking or jogging, yoga and strength training all helped. Harder helped more, but "
      "every kind counted.",
      "Noetel et al., 2024, BMJ (network meta-analysis)",
      "Choose a song now for it. Put your trainers where you will trip over them.",
      ["I get home in the evening", "I close my laptop at the end of the day",
       "I finish eating lunch"],
      "Shake your hands out and say \"that counts\". It does."),

    r("BODY", "sleep", "Wind down",
      "Plug your phone in across the room before bed.",
      "Lights out within half an hour of the same time as last night.",
      "Screens off an hour before, lights low, same bedtime and same wake time.",
      (1, 5, 60),
      "How regular your sleep is matters as much as how long it is. In a study of over "
      "60,000 people, a steady sleep schedule predicted long-term health more strongly than "
      "sleep length did.",
      "Windred et al., 2024, Sleep",
      "Set a bedtime alarm, not only a wake-up one.",
      ["I put my phone on charge for the night", "I put my toothbrush down at night",
       "I turn the living room lights off"],
      "Pull the covers up and say \"good night, me\"."),

    r("BODY", "fuel", "Eat something real",
      "Eat one piece of fruit or a handful of vegetables.",
      "A proper meal, sitting down, not in front of a screen.",
      "Cook something simple from scratch and keep a portion for tomorrow.",
      (2, 20, 45),
      "In a randomised trial, people with depression who improved their diet recovered "
      "more often than those given social support alone. In daily diaries, days with more "
      "fruit and vegetables came before days of better mood.",
      "Jacka et al., 2017, BMC Medicine (SMILES trial); "
      "White, Horwath & Conner, 2013, British Journal of Health Psychology",
      "Put a fruit bowl where your keys go.",
      ["I finish my first drink of the day", "I get home in the evening",
       "I sit down for lunch"],
      "Say \"fed\" out loud. Bodies like being listened to."),

    # ================================================================== ROOM
    r("ROOM", "reset", "The five-pile reset",
      "Pick up five things and put them where they live.",
      "Ten minutes, one room, in five piles: rubbish, dishes, laundry, things with a home, "
      "things without one.",
      "The whole room in the five piles, then wipe every surface.",
      (2, 10, 40),
      "Clutter competes for attention: brain imaging shows objects in a crowded scene "
      "fighting for representation in visual cortex. People who described their homes as "
      "cluttered and unfinished also had flatter daily stress-hormone curves, a pattern "
      "linked to poorer health.",
      "Method: KC Davis, 2022, How to Keep House While Drowning. Evidence: McMains & "
      "Kastner, 2011, Journal of Neuroscience; Saxbe & Repetti, 2010, Personality and "
      "Social Psychology Bulletin",
      "Start with rubbish, the easiest decision there is. Set a timer and stop when it rings.",
      ["I get home in the evening", "I put my plate in the sink this evening",
       "I close my laptop at the end of the day"],
      "Stand in the doorway and look for three seconds. You did that.",
      starter=True),

    r("ROOM", "bed", "Make the bed",
      "Pull the duvet up and straighten the pillow.",
      "Make the bed properly and clear everything off it.",
      "Strip it and put clean sheets on.",
      (1, 3, 15),
      "Nobody has trialled bed-making itself; the case for it is that it is the smallest "
      "finished thing in a day. In 12,000 diary entries, the days people felt best were the "
      "days they made progress, even small progress.",
      "Amabile & Kramer, 2011, The Progress Principle",
      "Do it before you leave the bedroom, while you are still standing next to it.",
      ["I get out of bed in the morning", "I turn my alarm off in the morning",
       "I get dressed in the morning"],
      "Pat the pillow twice. Done is done."),

    r("ROOM", "dishes", "Dishes home",
      "Carry every cup and plate in your room to the kitchen.",
      "Wash or load everything in the sink.",
      "Clear the sink, wipe the counters, and take the bin out.",
      (2, 10, 25),
      "Clutter at home was linked to a weaker sense of home as a refuge, and through that "
      "to lower wellbeing. Cups and plates are the clutter that comes back every day, so "
      "they are the best place to start.",
      "Roster, Ferrari & Jurkat, 2016, Journal of Environmental Psychology",
      "Keep one tray by your bed so every cup goes back in a single trip.",
      ["I put my plate in the sink this evening", "I switch the kettle on in the morning",
       "I finish eating lunch"],
      "Dry your hands with a flourish. The sink is yours again."),

    r("ROOM", "surface", "One clear surface",
      "Clear one small surface: the bedside table, or one shelf.",
      "Clear and wipe your desk or table completely.",
      "Every flat surface in the room, clear and wiped.",
      (2, 10, 30),
      "A clear surface gives your eyes somewhere to rest. When a scene is crowded, objects "
      "compete for the brain's limited attention, so clearing the place you work or eat "
      "reduces that competition where you spend the most time.",
      "McMains & Kastner, 2011, Journal of Neuroscience",
      "Pick the surface you look at most. Things without a home go in one box, sorted later.",
      ["I sit down where I work", "I close my laptop at the end of the day",
       "I get home in the evening"],
      "Run your hand across the clear bit. Smooth."),

    r("ROOM", "laundry", "Laundry loop",
      "Put every piece of dirty clothing into one basket.",
      "Start one wash.",
      "Wash, dry, fold and put away one full load.",
      (2, 5, 45),
      "Chores get put off when they feel huge and vague. Across hundreds of procrastination "
      "studies, the more aversive and unclear a task, the longer it waits; one concrete next "
      "step is what lowers the bar to starting.",
      "Steel, 2007, Psychological Bulletin (meta-analysis)",
      "Put the basket exactly where clothes land now. Move the basket, not the habit.",
      ["I get undressed at night", "I get home in the evening", "I turn the shower off"],
      "Throw the last sock in like a basketball. Swish."),

    r("ROOM", "air", "Air it out",
      "Open a window wide for two minutes.",
      "Window open for ten minutes while you take the rubbish out.",
      "Window open, rubbish out, and a quick sweep or hoover of the floor.",
      (2, 10, 25),
      "Stale air is not only unpleasant. In controlled experiments, people scored lower on "
      "decision-making tests when indoor CO2 was higher, and better when ventilation improved.",
      "Satish et al., 2012, Environmental Health Perspectives; "
      "Allen et al., 2016, Environmental Health Perspectives",
      "Open it the moment you walk into the room, before anything else.",
      ["I get out of bed in the morning", "I get home in the evening",
       "I walk into my room after work"],
      "One big breath of the new air. Ahh."),

    r("ROOM", "closing", "Closing shift",
      "Put tomorrow's first thing by the door: keys, bag, or bottle.",
      "Ten minutes of closing duties: kitchen reset, tomorrow's clothes laid out.",
      "Closing duties, plus a five-minute plan for tomorrow.",
      (1, 10, 20),
      "Habits are cued by your surroundings. Setting tomorrow's space up tonight makes the "
      "right action the easy one in the morning, and changing the context is one of the most "
      "reliable ways to change what people do.",
      "Wood & Rünger, 2016, Annual Review of Psychology",
      "Same point every night, like a shop closing up.",
      ["I put my toothbrush down at night", "I turn the living room lights off",
       "I put my phone on charge for the night"],
      "Flick the light off and say \"closed\". Like a shopkeeper."),

    # ================================================================== HEART
    r("HEART", "name", "Name it",
      "Finish the sentence \"Right now I feel ___\". One word is enough.",
      "Three lines: what you feel, where you feel it in your body, and what it is asking for.",
      "Fifteen minutes of writing about what is really on your mind, without editing.",
      (1, 5, 15),
      "Putting a feeling into words calms it. In brain scans, naming an emotion lowered "
      "activity in the amygdala, the brain's alarm system. Writing about what troubles you "
      "has smaller but real benefits over time.",
      "Lieberman et al., 2007, Psychological Science; "
      "Frattaroli, 2006, Psychological Bulletin (meta-analysis)",
      "Keep one note open for this. Typing a single word counts.",
      ["I put my phone on charge for the night", "I sit down after getting home",
       "I finish eating lunch"],
      "Hand on your chest: \"got it\".",
      starter=True),

    r("HEART", "sigh", "Slow breathing",
      "Three cyclic sighs: two breaths in through the nose, one long breath out.",
      "Five minutes of cyclic sighing, eyes closed if you like.",
      "Five minutes of cyclic sighing, then a ten-minute walk without your phone.",
      (1, 5, 15),
      "In a month-long randomised trial, five minutes a day of cyclic sighing improved mood "
      "and slowed resting breathing more than mindfulness meditation did. The long exhale is "
      "the part that settles you.",
      "Balban et al., 2023, Cell Reports Medicine",
      "Do it sitting exactly where you are. No app, no mat.",
      ["I sit down after getting home", "I switch the kettle on in the morning",
       "I lie down in bed at night"],
      "Let your shoulders drop one more centimetre. There."),

    r("HEART", "dump", "Brain dump",
      "Write down the one thing nagging you most.",
      "List everything on your mind, then write the very next step for one item.",
      "The full list, a next step for each, and the top three put in your calendar.",
      (2, 10, 25),
      "Unfinished tasks keep intruding on your thoughts, but making a specific plan for them "
      "stopped the intrusions, even before the task was done. Writing a to-do list at bedtime "
      "also helped people fall asleep faster.",
      "Masicampo & Baumeister, 2011, Journal of Personality and Social Psychology; "
      "Scullin et al., 2018, Journal of Experimental Psychology: General",
      "Same notebook or note every time, so everything has one home.",
      ["I close my laptop at the end of the day", "I put my phone on charge for the night",
       "I sit down where I work"],
      "Close the notebook firmly. It is out of your head now."),

    r("HEART", "reach", "Reach out",
      "Send someone a one-line message: \"Saw this and thought of you.\"",
      "Send a voice note, or make a short call to someone you like.",
      "Make a real plan to see someone: a day, a time and a place.",
      (2, 10, 15),
      "People consistently underestimate how much others appreciate hearing from them. And "
      "strong social ties are linked to longer life about as strongly as not smoking is.",
      "Liu, Rim, Min & Min, 2023, Journal of Personality and Social Psychology; "
      "Holt-Lunstad, Smith & Layton, 2010, PLoS Medicine",
      "Pin three people to the top of your messages, so the choice is already made.",
      ["I sit down on the bus or train", "I finish eating lunch", "I get home in the evening"],
      "Put the phone down and smile at it. Someone just got a nice surprise."),

    r("HEART", "kind", "Kind voice",
      "Hand on your chest, and say: \"This is hard, and I am doing my best.\"",
      "Write to yourself the way you would write to a friend in exactly your situation.",
      "That letter, then one kind thing for yourself you would normally save for someone else.",
      (1, 10, 30),
      "Being kind to yourself after a setback makes you more likely to try again, not less. "
      "In experiments, people encouraged to be self-compassionate after a failure studied "
      "longer and were more motivated to improve.",
      "Breines & Chen, 2012, Personality and Social Psychology Bulletin",
      "Use the words you would say to a friend, not the ones you usually say to yourself.",
      ["I notice I am being hard on myself", "I sit down after getting home",
       "I lie down in bed at night"],
      "Give yourself the nod you would give a friend."),

    r("HEART", "good", "Three good things",
      "Name one thing that went okay today, out loud or in writing.",
      "Write three good things from today, and why each one happened.",
      "Write a thank-you letter to someone who helped you. Send it if you want to.",
      (1, 10, 25),
      "Writing three good things and their causes each night raised happiness and lowered "
      "depressive symptoms for months in a randomised trial. Later studies find the effect "
      "is smaller than first reported, but it is real and it is cheap.",
      "Seligman, Steen, Park & Peterson, 2005, American Psychologist",
      "Do it in bed with the light off. Say them to the ceiling.",
      ["I lie down in bed at night", "I put my phone on charge for the night",
       "I finish eating dinner"],
      "Smile at the third one. You noticed it."),

    r("HEART", "worry", "Worry window",
      "When a worry turns up, write it down and tell it \"later\".",
      "Fifteen minutes of deliberate worrying, same time and place, then stop.",
      "Your worry window, then one small action on the worry you can actually act on.",
      (1, 15, 25),
      "Postponing worry to one fixed daily window, instead of fighting it all day, reduced "
      "worry and anxiety in a controlled trial. The worry gets its time; it just stops "
      "getting all of yours.",
      "McGowan & Behar, 2013, Behavior Modification",
      "Choose the window now: same chair, same time, and not in bed.",
      ["I notice I am worrying", "I finish eating dinner", "I sit down after getting home"],
      "Shut the window: \"that is enough for today\"."),

    r("HEART", "savour", "Something you enjoy",
      "Stop for thirty seconds and really notice one nice thing: a taste, a sound, the light.",
      "Do one small thing only because you enjoy it, and do it on purpose.",
      "Plan something to look forward to this week and put it in the calendar.",
      (1, 20, 15),
      "Scheduling enjoyable things is the core of behavioural activation, one of the "
      "best-supported treatments for depression. Low mood says wait until you feel like it; "
      "the evidence says do it first, and the feeling tends to follow.",
      "Cuijpers, van Straten & Warmerdam, 2007, Clinical Psychology Review (meta-analysis)",
      "Write a list of ten tiny pleasures now, while you can think of them.",
      ["I finish eating lunch", "I get home in the evening",
       "I close my laptop at the end of the day"],
      "Say \"mm\" like you mean it."),
]

# Mirrors Cues.isSpecific in engine/Intention.kt. The app tests the user's cues with it, so
# the cues the app suggests had better pass it too.
VAGUE = ["when i can", "when i get a chance", "sometime", "soon", "later",
         "if i feel", "when i feel", "eventually", "at some point", "when ready"]


def is_specific(cue):
    c = cue.strip().lower()
    return len(c) >= 8 and not any(v in c for v in VAGUE)


def validate():
    problems = []
    ids = [x["id"] for x in ROUTINES]
    if len(ids) != len(set(ids)):
        problems.append("duplicate routine ids")
    domains = {d["id"] for d in DOMAINS}
    for d in DOMAINS:
        if len(d["levels"]) != RULES["maxDirt"] + 1 or len(d["checkin"]) != RULES["maxDirt"] + 1:
            problems.append(f"{d['id']}: needs {RULES['maxDirt'] + 1} levels and check-in answers")
        rs = [x for x in ROUTINES if x["domain"] == d["id"]]
        if len(rs) < 5:
            problems.append(f"{d['id']}: only {len(rs)} routines; a library needs choice")
        if sum(1 for x in rs if x["starter"]) != 1:
            problems.append(f"{d['id']}: needs exactly one starter routine")
    for x in ROUTINES:
        tag = x["id"]
        if x["domain"] not in domains:
            problems.append(f"{tag}: unknown domain")
        if x["tinyMin"] > 2:
            problems.append(f"{tag}: tiny is {x['tinyMin']} min; the two-minute rule is the point")
        if not (x["tinyMin"] <= x["standardMin"]):
            problems.append(f"{tag}: standard is shorter than tiny")
        for f in ("tiny", "standard", "deep", "why", "source", "ease", "celebrate", "title"):
            if not x[f].strip():
                problems.append(f"{tag}: empty {f}")
        if len(set((x["tiny"], x["standard"], x["deep"]))) != 3:
            problems.append(f"{tag}: sizes are not three different acts")
        if len(x["why"]) > 330:
            problems.append(f"{tag}: why is {len(x['why'])} chars; keep it readable on a phone")
        if len(x["anchors"]) < 2:
            problems.append(f"{tag}: offer at least two cues")
        for a in x["anchors"]:
            if not is_specific(a):
                problems.append(f"{tag}: cue fails the specificity test: {a!r}")
            if a[0].isupper() and not a.startswith("I "):
                problems.append(f"{tag}: cue should read after 'When ': {a!r}")
        if any(ch.isdigit() for ch in x["source"]) is False:
            problems.append(f"{tag}: source has no year")
    return problems


def main():
    problems = validate()
    if problems:
        for p in problems:
            print("FAIL", p)
        sys.exit(1)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    doc = {"rules": RULES, "domains": DOMAINS, "routines": ROUTINES}
    OUT.write_text(json.dumps(doc, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    by = {d["id"]: sum(1 for x in ROUTINES if x["domain"] == d["id"]) for d in DOMAINS}
    print(f"wrote {OUT.relative_to(HERE.parent.parent)}: {len(ROUTINES)} routines {by}, "
          f"{OUT.stat().st_size // 1024} KB")


if __name__ == "__main__":
    main()
