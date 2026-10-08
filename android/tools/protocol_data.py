"""Shared protocol vocabulary for the three ladders.

Anchors and safety behaviours are drawn from fixed catalogues rather than written fresh 300
times, for two reasons. An anchor has to name a real instant in an ordinary day, and there are
only so many of those — inventing more would mean inventing less reliable ones. And a safety
behaviour only counts as one if it is specific enough to actually drop, which again means a
small vocabulary used precisely, not a large one used loosely.

Anchors are first person and present tense so they drop straight into the plan screen's frame:
"When <anchor>, I will <response>." They must also pass the app's own Cues.isSpecific test,
which gen_curriculum.py asserts.
"""

# ----------------------------------------------------------------- anchors (precise moments)
#
# Each one is an instant that is going to happen anyway. "After breakfast" is not here, because
# it is a region of time; "I put my plate in the sink" is, because it is a moment you can be
# standing in.
ANCHORS = {
    # getting up / leaving
    "wake":     "I first get out of bed tomorrow morning",
    "alarm":    "I turn my alarm off tomorrow",
    "shoes":    "I put my shoes on tomorrow morning",
    "keys":     "I pick my keys up to leave",
    "lock":     "I lock my front door behind me",
    "shut":     "I hear my front door shut behind me",
    "coat_on":  "I put my coat on to go out",
    # coming home
    "coat_off": "I take my coat off after coming in",
    "bag":      "I put my bag down at home",
    "kettle":   "I switch the kettle on after coming in",
    # the day's hinges
    "desk":     "I sit down where I work tomorrow",
    "laptop":   "I close my laptop at the end of the day",
    "lunch":    "I finish eating lunch tomorrow",
    "plate":    "I put my plate in the sink this evening",
    "lights":   "I turn the lights on this evening",
    "shower":   "I turn the shower off",
    "teeth":    "I put my toothbrush back down tonight",
    "charge":   "I put my phone on charge for the night",
    "phone_dn": "I next put my phone face down",
    # out in the world
    "queue":    "I next join the back of a queue",
    "till":     "I next reach the front of a till",
    "change":   "I am next handed my change or my receipt",
    "coffee":   "I next pay for a coffee",
    "seat":     "I next sit down on a bus or a train",
    "stop":     "the doors next open at my stop",
    "wait":     "I am next standing at a stop or a platform",
    "lift":     "I next get into a lift with someone already in it",
    "pass":     "I next pass someone on the pavement",
    "corner":   "I next reach the corner of my own street",
    "door_pub": "I next put my hand on a door someone else is about to come through",
    # people
    "known":    "I next see someone whose face I recognise",
    "greeted":  "someone next says hello to me first",
    "asked":    "someone next asks me how I am",
    "alone_w":  "I am next alone with someone I know",
    "msg":      "I next unlock my phone and see an unread message",
}

# ----------------------------------------------------- safety behaviours to remove (Craske)
#
# A feared thing survived while clutching a safety signal teaches that the safety signal was
# load-bearing. Each of these names one, specifically enough to be put down.
DROPS = {
    "phone":    "Phone stays in your pocket. Holding it to look busy is the thing to drop.",
    "music":    "No headphones. Hearing the place is part of it.",
    "rehearse": "Do not rehearse the sentence. Say the first version that arrives.",
    "exit":     "Do not prepare an exit line before you start.",
    "quiet":    "Do not wait for it to go quiet. Busy enough to be real is the point.",
    "alone":    "Go alone. Bringing someone turns it into a different task.",
    "mirror":   "Do not check your reflection first.",
    "research": "Do not look it up beforehand. Arrive uninformed.",
    "map":      "Map stays in your pocket. Getting it slightly wrong is included.",
    "today":    "Not when you feel readier. Today's version is the task.",
    "draft":    "Do not write it and sit on it. Send the first version.",
    "soften":   "No softening clause at the end. Stop talking after the ask.",
    "explain":  "Do not explain yourself. No reasons, no justification — the sentence stands alone.",
    "warmup":   "No easier version first to warm up with.",
    "check":    "Do not check how you are doing while you are doing it.",
    "record":   "Do not film it or photograph it. No proof, no audience.",
    "friendly": "Nobody present who already likes you.",
    "undo":     "No escape arranged in advance. No pre-booked way out.",
    "edge":     "Nothing to take the edge off beforehand.",
    "tell":     "Do not announce it first. Announcing it is not doing it.",
}
