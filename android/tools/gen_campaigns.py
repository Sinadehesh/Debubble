#!/usr/bin/env python3
"""
Generates the campaign-specific challenge pools.

  assets/campaigns/pool.json

Two kinds of entry, distinguished only by how many goals they list:

  - A single-goal challenge belongs to one campaign.
  - A **bridge** lists two, and is only ever served to someone running both. Bridges
    are not a gimmick: someone chasing a partner and a social circle at the same time
    is doing one thing, not two, and serving them two separate errands on the same
    evening is how an app gets deleted. "Host something and invite two people you do
    not know well" satisfies both and costs one evening.

Every entry carries a budget tier. The engine filters on it strictly, so a user on
Free never sees an entry that needs money — no greyed-out teasing, it simply is not
in their pool.

Run: python3 android/tools/gen_campaigns.py
"""

import json
import os
from itertools import combinations

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "campaigns")

FREE, CHEAP, PREMIUM = 0, 1, 2
GOALS = ["INTERESTING", "FRIENDS", "PARTNER", "INTIMACY", "CRAFT"]

# (goals, do, why, minutes, cost, tier, exposure, pillar, themes)
POOL = [
    # ============================================================== PARTNER
    # Vulnerability, non-neediness, working out who you are actually looking for,
    # and being physically comfortable in the rooms where people meet.
    (["PARTNER"], "Say one true thing about yourself today that you would normally keep back.",
     "Vulnerability is not confession. It is one honest sentence offered without checking whether it landed well.",
     10, 0, FREE, 5, "SOCIAL", ["vulnerability"]),
    (["PARTNER"], "Write down five things you actually want in a person. Not the polite list — the real one.",
     "Most people looking for someone have never defined who. You cannot search for an unspecified thing.",
     20, 0, FREE, 2, "ACTIVITY", ["demographics", "clarity"]),
    (["PARTNER"], "Write down where a person like that would actually spend their time, and pick one to go to this week.",
     "Attraction is a location problem before it is a charisma problem. The right room does most of the work.",
     20, 0, FREE, 3, "ACTIVITY", ["demographics", "strategy"]),
    (["PARTNER"], "Go to that place and stay a full hour, whether or not you speak to anyone.",
     "Turning up repeatedly is the whole method. Talking to someone is a bonus on the first visit.",
     60, 0, FREE, 6, "ACCESS", ["demographics", "exposure"]),
    (["PARTNER"], "Have a conversation where you do not adjust a single opinion to make someone like you more.",
     "Neediness is not wanting someone. It is editing yourself in real time to keep them. They can always tell.",
     15, 0, FREE, 6, "SOCIAL", ["neediness"]),
    (["PARTNER"], "Let a message sit unanswered for two hours because you were busy, not as a tactic.",
     "The difference between calm and games is whether you were actually doing something else. Go and do something else.",
     10, 0, FREE, 4, "ACTIVITY", ["neediness"]),
    (["PARTNER"], "State a preference on a date or in a conversation that risks being the wrong answer.",
     "Being agreeable is not attractive, it is invisible. Polarising slightly is how anyone remembers you.",
     15, 0, FREE, 7, "SOCIAL", ["polarization"]),
    (["PARTNER"], "Ask someone out with a specific plan, a day and a time.",
     "Vague invitations are a way of not being refused. They are also a way of not being accepted.",
     10, 0, FREE, 9, "SOCIAL", ["directness", "rejection"]),
    (["PARTNER"], "Spend twenty minutes in a mirror working out which of your clothes actually fit your frame.",
     "Style is not spending. It is fit, upkeep, and a small number of decisions made once.",
     20, 0, FREE, 2, "ACTIVITY", ["style"]),
    (["PARTNER"], "Go somewhere with music and stay for one whole song without leaving the floor.",
     "Physical ease in a room where bodies move is a real signal and a trainable one. Nobody is watching you.",
     30, 0, FREE, 7, "ACCESS", ["physicality", "dancing"]),
    (["PARTNER"], "Take a beginner class in something physical — dancing, climbing, martial arts.",
     "A structured room is the cheapest way to be bad at something physical in public without it meaning anything.",
     60, 15, CHEAP, 6, "ACTIVITY", ["physicality", "dancing"]),
    (["PARTNER"], "Tell someone you are interested in them, plainly, and accept whatever comes back.",
     "The plain version is rarer than you think and it works more than you think. It also ends the not-knowing.",
     15, 0, FREE, 10, "SOCIAL", ["directness", "vulnerability"]),
    (["PARTNER"], "Get turned down today, deliberately. Ask for something with a real chance of a no.",
     "A refusal you went looking for is training. A refusal you stumbled into is a wound. Same event, different frame.",
     25, 0, FREE, 8, "SOCIAL", ["rejection"]),
    (["PARTNER"], "Have a full conversation with someone you find attractive without trying to secure anything.",
     "The pressure you feel is you trying to close. Remove the close and you will find you are much better at this.",
     20, 0, FREE, 7, "SOCIAL", ["neediness", "presence"]),

    # ============================================================== FRIENDS
    (["FRIENDS"], "Message one person you have not spoken to in over a month. No apology for the gap.",
     "The apology makes it awkward and invites them to reassure you. Just start talking.",
     8, 0, FREE, 3, "SOCIAL", ["initiating"]),
    (["FRIENDS"], "Invite one person to something specific this week. A time, a place, a plan.",
     "'We should catch up' is not an invitation. It is a way of feeling like you tried.",
     10, 0, FREE, 5, "SOCIAL", ["initiating"]),
    (["FRIENDS"], "Be the one who suggests the plan in a group chat that has gone quiet.",
     "Every group has one person who starts things. Groups where nobody does it quietly die.",
     10, 0, FREE, 4, "SOCIAL", ["initiating", "hosting"]),
    (["FRIENDS"], "Host something small at home. Two people, one evening, nothing elaborate.",
     "Hosting is the fastest route from acquaintance to friend, and the bar is far lower than you are imagining.",
     120, 15, CHEAP, 7, "SOCIAL", ["hosting"]),
    (["FRIENDS"], "Introduce two people you know who do not know each other.",
     "Being the connector makes you central to a group without needing to be the loudest person in it.",
     15, 0, FREE, 5, "SOCIAL", ["community", "hosting"]),
    (["FRIENDS"], "Go to the same recurring thing twice in a fortnight — a class, a club, a pub quiz.",
     "Friendship is repeated unplanned contact. Recurring events manufacture exactly that.",
     90, 10, CHEAP, 5, "ACCESS", ["community"]),
    (["FRIENDS"], "Remember one detail from a conversation and follow up on it a week later.",
     "This single habit does more for friendship than any amount of charm. It says: you were actually there.",
     8, 0, FREE, 3, "SOCIAL", ["depth"]),
    (["FRIENDS"], "Ask someone for a small favour, and thank them properly for it.",
     "Favours build friendship in both directions. Never asking keeps people at a polite distance.",
     10, 0, FREE, 5, "SOCIAL", ["depth", "reciprocity"]),
    (["FRIENDS"], "Tell one friend something that is actually going on with you.",
     "Groups stay shallow until someone goes first. It is usually not going to be them.",
     15, 0, FREE, 6, "SOCIAL", ["depth", "vulnerability"]),
    (["FRIENDS"], "Turn up to something you were invited to and did not want to go to.",
     "Attendance is the whole tax on having people. You pay it before you feel like it, not after.",
     120, 0, FREE, 5, "SOCIAL", ["community"]),
    (["FRIENDS"], "Start a recurring thing yourself. Same day, same place, every fortnight. Tell three people.",
     "Owning the standing invitation makes you the centre of a small community by default.",
     45, 0, FREE, 7, "SOCIAL", ["hosting", "community"]),
    (["FRIENDS"], "Have a conversation with someone in your building or on your street.",
     "Proximity is the most underused source of friendship there is, and it costs nothing to travel to.",
     15, 0, FREE, 5, "SOCIAL", ["community", "initiating"]),
    (["FRIENDS"], "Cook for someone.",
     "Feeding people is the oldest version of hosting and it works on everyone.",
     90, 20, CHEAP, 6, "SOCIAL", ["hosting"]),
    (["FRIENDS"], "Reach out to someone the day after you last saw them, with one specific thing you enjoyed.",
     "The follow-up is what converts a nice evening into a relationship. Almost nobody does it.",
     8, 0, FREE, 4, "SOCIAL", ["initiating", "depth"]),

    # ========================================================== INTERESTING
    (["INTERESTING"], "Go somewhere in your own town you have never been, today.",
     "A life with stories in it is mostly a life with unfamiliar places in it. Start with the free ones.",
     45, 0, FREE, 3, "ACCESS", ["novelty"]),
    (["INTERESTING"], "Say yes to the next invitation before you have finished reading it.",
     "The deliberation is where the interesting things die. Beat it by a few seconds.",
     10, 0, FREE, 5, "ACTIVITY", ["novelty", "action"]),
    (["INTERESTING"], "Do one thing this week you would have to explain to someone.",
     "If nothing you do needs explaining, there is nothing to say when someone asks what you have been up to.",
     90, 10, CHEAP, 5, "ACTIVITY", ["novelty", "stories"]),
    (["INTERESTING"], "Take a different route to somewhere you go every week.",
     "Novelty is a habit, and it is cheapest to practise on the routes you already walk.",
     20, 0, FREE, 2, "ACCESS", ["novelty"]),
    (["INTERESTING"], "Write down the last three genuinely interesting things that happened to you. If you cannot fill three, that is the finding.",
     "This is a measurement, not a judgement. You cannot fix a rate you have not measured.",
     15, 0, FREE, 1, "ACTIVITY", ["stories", "audit"]),
    (["INTERESTING"], "Go to an event on your own.",
     "Going alone means you actually attend to the thing, and it removes the requirement that someone else be free.",
     120, 15, CHEAP, 7, "ACCESS", ["solo", "novelty"]),
    (["INTERESTING"], "Learn one specific fact about where you live that most people there do not know.",
     "Specific local knowledge is the cheapest form of being interesting, and it is always relevant.",
     25, 0, FREE, 1, "ACTIVITY", ["depth", "stories"]),
    (["INTERESTING"], "Spend an afternoon somewhere with no plan and no phone navigation.",
     "Plans produce outcomes. Wandering produces stories. You need some of both.",
     150, 0, FREE, 5, "ACCESS", ["novelty", "solo"]),
    (["INTERESTING"], "Talk to someone whose life looks nothing like yours, and ask about it.",
     "Most people's range of acquaintance is a single demographic. Widening it is the fastest way to widen yourself.",
     20, 0, FREE, 6, "SOCIAL", ["range"]),
    (["INTERESTING"], "Book something now that happens in a month.",
     "A future you are looking forward to changes the present. Having nothing booked is its own condition.",
     20, 25, CHEAP, 3, "ACTIVITY", ["novelty", "anticipation"]),
    (["INTERESTING"], "Do the thing you keep saying you would love to do 'one day'.",
     "One day is not a day of the week. Pick the actual date.",
     180, 60, PREMIUM, 6, "ACTIVITY", ["novelty", "action"]),
    (["INTERESTING"], "Take a train to a town you have never visited and spend the day there.",
     "Range is measurable and it compounds. Every unfamiliar place makes the next one easier.",
     300, 30, CHEAP, 6, "ACCESS", ["range", "novelty"]),
    (["INTERESTING"], "Tell someone about something you did this week, out loud, as a story with an ending.",
     "Having done things is half. Being able to tell them is the half most people skip.",
     15, 0, FREE, 4, "SOCIAL", ["stories"]),
    (["INTERESTING"], "Change one fixed thing about your week for a fortnight.",
     "Routine is not the enemy. An unexamined routine that has quietly shrunk your world is.",
     30, 0, FREE, 4, "ACTIVITY", ["novelty", "audit"]),

    # ============================================================= INTIMACY
    (["INTIMACY"], "Name one thing you want, out loud, to one person.",
     "Wanting things silently and hoping they are guessed is the most common failure here, by a distance.",
     10, 0, FREE, 6, "SOCIAL", ["asking", "vulnerability"]),
    (["INTIMACY"], "Write down what you actually want that you have never said to anyone.",
     "You cannot say it to someone else until you can write it down alone.",
     20, 0, FREE, 3, "ACTIVITY", ["clarity", "vulnerability"]),
    (["INTIMACY"], "Say no to something you would normally go along with.",
     "Consent runs both directions. Someone who cannot decline cannot really accept either.",
     10, 0, FREE, 6, "SOCIAL", ["boundaries"]),
    (["INTIMACY"], "Ask someone what they want, and then actually be quiet.",
     "Most people are never asked this directly. The silence afterwards is what makes the answer possible.",
     15, 0, FREE, 6, "SOCIAL", ["asking", "listening"]),
    (["INTIMACY"], "Hold eye contact with someone for a full five seconds longer than is comfortable.",
     "Physical confidence is built in seconds like these, not in preparation.",
     10, 0, FREE, 7, "SOCIAL", ["physicality"]),
    (["INTIMACY"], "Give one specific, honest compliment about something that is not appearance.",
     "Specific and non-physical is the version that lands as attention rather than as an approach.",
     8, 0, FREE, 4, "SOCIAL", ["warmth"]),
    (["INTIMACY"], "Have one conversation about what you both actually expect, before assuming.",
     "The awkward conversation before is always shorter than the one after.",
     25, 0, FREE, 8, "SOCIAL", ["directness", "boundaries"]),
    (["INTIMACY"], "Spend twenty minutes on your body with no goal — stretching, walking, moving.",
     "Comfort in your own body is upstream of comfort with anyone else's.",
     20, 0, FREE, 2, "ACTIVITY", ["physicality"]),
    (["INTIMACY"], "Say the thing you were about to soften, without softening it.",
     "The softening is where the meaning goes. Say the plain version once and watch what happens.",
     10, 0, FREE, 7, "SOCIAL", ["directness"]),
    (["INTIMACY"], "Tell someone about something you find difficult, without apologising for saying it.",
     "The apology tells them the thing you shared was a burden. It was not.",
     15, 0, FREE, 7, "SOCIAL", ["vulnerability"]),
    (["INTIMACY"], "Ask for feedback about something and do not defend yourself while you hear it.",
     "Being able to hear it without flinching is what makes the next honest conversation possible.",
     20, 0, FREE, 7, "SOCIAL", ["listening", "resilience"]),
    (["INTIMACY"], "Initiate a plan with someone you are already close to, rather than waiting.",
     "Initiating is a skill, and it does not switch on by itself once things are established.",
     15, 0, FREE, 5, "SOCIAL", ["asking"]),
    (["INTIMACY"], "Notice one thing you assumed the other person wanted, and ask instead.",
     "Assumption is comfortable and almost always slightly wrong.",
     10, 0, FREE, 5, "SOCIAL", ["asking", "listening"]),
    (["INTIMACY"], "Say what you liked, specifically, after something good happens.",
     "Naming it afterwards is how it happens again. Silence is how it does not.",
     10, 0, FREE, 6, "SOCIAL", ["directness", "warmth"]),

    # ================================================================ CRAFT
    (["CRAFT"], "Spend thirty uninterrupted minutes on the thing. Phone in another room.",
     "Uninterrupted is the whole variable. Thirty real minutes beats three fragmented hours.",
     30, 0, FREE, 1, "ACTIVITY", ["practice"]),
    (["CRAFT"], "Find out what the standard beginner mistake is, and check whether you are making it.",
     "Twenty minutes of finding out saves months of practising something wrong.",
     25, 0, FREE, 1, "ACTIVITY", ["practice", "learning"]),
    (["CRAFT"], "Show your work to one person.",
     "Work nobody has seen improves slowly. It is the showing that does it, not the feedback.",
     15, 0, FREE, 5, "SOCIAL", ["showing"]),
    (["CRAFT"], "Finish something badly rather than leaving it unfinished.",
     "Finished bad work teaches. Unfinished good work does not exist.",
     60, 0, FREE, 3, "ACTIVITY", ["practice", "shipping"]),
    (["CRAFT"], "Practise the specific part you are worst at, not the part you enjoy.",
     "Practising your strengths is entertainment. Practising the weak part is the thing that moves.",
     40, 0, FREE, 2, "ACTIVITY", ["practice"]),
    (["CRAFT"], "Find one other person who does this and talk to them about it.",
     "A craft with nobody to talk about it with is a hobby that ends quietly in about four months.",
     30, 0, FREE, 5, "SOCIAL", ["community"]),
    (["CRAFT"], "Buy the one piece of equipment that has genuinely been holding you back.",
     "Only one. Buying gear is the most popular way of avoiding practice.",
     40, 60, PREMIUM, 3, "ACTIVITY", ["equipment"]),
    (["CRAFT"], "Set a fixed weekly slot for it and defend it for a month.",
     "Consistency beats intensity here more than almost anywhere else.",
     20, 0, FREE, 2, "ACTIVITY", ["practice", "habit"]),
    (["CRAFT"], "Post or share one piece of work publicly.",
     "Public is a different threshold from private, and it changes what you make.",
     25, 0, FREE, 7, "SOCIAL", ["showing", "shipping"]),
    (["CRAFT"], "Go to a class, meet-up or workshop for it.",
     "One room of people who are better than you is worth a month of tutorials.",
     120, 25, CHEAP, 6, "ACTIVITY", ["community", "learning"]),
    (["CRAFT"], "Copy something good, deliberately, all the way through.",
     "Copying is how everyone learns and the only stage people are embarrassed to admit to.",
     60, 0, FREE, 2, "ACTIVITY", ["learning", "practice"]),
    (["CRAFT"], "Teach one thing you know to someone who does not.",
     "Teaching is the fastest audit of what you actually understand.",
     30, 0, FREE, 5, "SOCIAL", ["showing", "community"]),
    (["CRAFT"], "Give yourself a real deadline for a real thing, and tell someone about it.",
     "A deadline nobody knows about is a wish.",
     20, 0, FREE, 4, "SOCIAL", ["shipping"]),
    (["CRAFT"], "Do it on a day you do not feel like it.",
     "The days you do not feel like it are the entire difference between someone who does this and someone who did.",
     40, 0, FREE, 3, "ACTIVITY", ["habit", "practice"]),

    # ============================================================== BRIDGES
    # Served only when both named campaigns are running.
    (["FRIENDS", "PARTNER"], "Host something small and invite two people you do not know well.",
     "One evening satisfies both: you are hosting, and you are meeting people outside your usual circle.",
     150, 20, CHEAP, 7, "SOCIAL", ["hosting", "demographics"]),
    (["FRIENDS", "PARTNER"], "Go to a social event with someone you know, and talk to three people they brought.",
     "A shared friend is the warmest possible introduction and the easiest room to widen.",
     120, 10, CHEAP, 6, "SOCIAL", ["community", "demographics"]),
    (["FRIENDS", "INTERESTING"], "Take someone with you to a place neither of you has been.",
     "The unfamiliar place gives you something to talk about, which is what makes new friendships survive the second meeting.",
     120, 15, CHEAP, 5, "ACCESS", ["novelty", "initiating"]),
    (["FRIENDS", "INTERESTING"], "Start a group thing around something you actually like doing.",
     "The best recurring events are built on a real interest. Nobody keeps turning up to a generic one.",
     60, 0, FREE, 7, "SOCIAL", ["hosting", "novelty"]),
    (["PARTNER", "INTERESTING"], "Go to something you would enjoy alone, in a room where you might meet someone.",
     "Doing something you would want to do anyway removes the desperation and puts you in the right room. Both at once.",
     120, 20, CHEAP, 6, "ACCESS", ["demographics", "novelty"]),
    (["PARTNER", "INTERESTING"], "Learn one physical thing in a class with other people in it.",
     "Dancing, climbing, anything. Physical confidence, a new skill, and a recurring room full of people.",
     90, 20, CHEAP, 6, "ACTIVITY", ["physicality", "novelty"]),
    (["PARTNER", "INTIMACY"], "Say plainly what you want from the thing you are already in.",
     "Both campaigns fail the same way — silently, from something never said out loud.",
     20, 0, FREE, 8, "SOCIAL", ["directness", "asking"]),
    (["PARTNER", "INTIMACY"], "Have the conversation you have been rehearsing, today, unrehearsed.",
     "The rehearsal is avoidance wearing preparation's coat.",
     25, 0, FREE, 8, "SOCIAL", ["directness", "vulnerability"]),
    (["FRIENDS", "INTIMACY"], "Tell one person something true that you have been holding.",
     "Depth in friendship and depth in intimacy are the same muscle, and it is worked the same way.",
     20, 0, FREE, 7, "SOCIAL", ["vulnerability", "depth"]),
    (["CRAFT", "FRIENDS"], "Bring someone into the thing you are learning — invite them along or teach them a bit.",
     "Shared projects make friendships that survive people moving away. Very little else does.",
     90, 0, FREE, 5, "SOCIAL", ["community", "showing"]),
    (["CRAFT", "INTERESTING"], "Take the craft somewhere new — a different place, a different setting.",
     "Practice and range at once, and the change of setting usually improves the work.",
     120, 10, CHEAP, 4, "ACCESS", ["practice", "novelty"]),
    (["CRAFT", "PARTNER"], "Go to a class or meet-up for your craft, and talk to two people there.",
     "The room already shares your interest. That is the hardest part of meeting someone, solved for free.",
     120, 25, CHEAP, 6, "SOCIAL", ["community", "demographics"]),
    (["INTERESTING", "INTIMACY"], "Do something slightly outside your comfort zone with someone you are close to.",
     "Shared novelty is what keeps closeness from flattening into routine.",
     120, 20, CHEAP, 5, "ACTIVITY", ["novelty", "physicality"]),
    (["CRAFT", "INTIMACY"], "Show your unfinished work to someone whose opinion you actually care about.",
     "Being seen mid-effort is more exposing than being seen finished, and it is the exposure that matters.",
     25, 0, FREE, 7, "SOCIAL", ["showing", "vulnerability"]),
]


def build():
    seen = set()
    out = []
    for i, (goals, do, why, minutes, cost, tier, exp, pillar, themes) in enumerate(POOL):
        assert all(g in GOALS for g in goals), f"unknown goal in {goals}"
        assert len(goals) == len(set(goals)), f"duplicate goal in {goals}"
        assert 1 <= len(goals) <= 2, "an entry belongs to one campaign or bridges two"
        assert pillar in ("ACCESS", "ACTIVITY", "SOCIAL"), f"bad pillar {pillar}"
        assert 1 <= exp <= 10, f"exposure out of range: {do}"
        assert (cost == 0) == (tier == FREE), f"cost and tier disagree: {do}"
        assert do not in seen, f"duplicate directive: {do}"
        seen.add(do)
        cid = ("bridge_" if len(goals) == 2 else "") + "_".join(g.lower() for g in goals) + f"_{i}"
        out.append({
            "id": cid,
            "goals": goals,
            "do": do,
            "why": why,
            "min": minutes,
            "cost": cost,
            "tier": tier,
            "exp": exp,
            "pillar": pillar,
            "themes": themes,
        })

    singles = [c for c in out if len(c["goals"]) == 1]
    bridges = [c for c in out if len(c["goals"]) == 2]

    # Every campaign needs its own supply, and every campaign needs something free.
    for g in GOALS:
        mine = [c for c in singles if c["goals"][0] == g]
        assert len(mine) >= 12, f"{g} has only {len(mine)} challenges"
        assert any(c["tier"] == FREE for c in mine), f"{g} has nothing on the free tier"
        assert min(c["exp"] for c in mine) <= 3, f"{g} has no gentle entry point"

    # Every pair someone could run together needs at least one bridge, or the feature
    # silently does nothing for that combination.
    for a, b in combinations(GOALS, 2):
        pair = {a, b}
        assert any(set(c["goals"]) == pair for c in bridges), f"no bridge for {a} + {b}"

    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, "pool.json"), "w") as f:
        json.dump({"challenges": out}, f, indent=1, ensure_ascii=False)

    print(f"pool.json  {len(out)} challenges ({len(singles)} single, {len(bridges)} bridge)")
    for g in GOALS:
        n = sum(1 for c in singles if c["goals"][0] == g)
        free = sum(1 for c in singles if c["goals"][0] == g and c["tier"] == FREE)
        print(f"    {g:12} {n:3}   free: {free}")
    print(f"    bridges covering {len(list(combinations(GOALS, 2)))} possible pairs")


if __name__ == "__main__":
    build()
