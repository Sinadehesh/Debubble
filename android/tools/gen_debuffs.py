#!/usr/bin/env python3
"""
Generates the Systems Audit catalogue and its remediation pools.

Two files come out of this:

  assets/audit/debuffs.json      48 micro-debuffs in four categories
  assets/audit/remediation.json  three graded challenges for each of them

Two rules govern every entry, and they are what stop this from being a list of
things wrong with the user:

1. **Everything here is behaviour, not verdict.** "I look away when someone looks
   at me" is a habit with a first rep. "I am ugly" is a sentence about a person,
   it has no first rep, and once someone ticks it the app has stored their worst
   belief about themselves and started agreeing with it. Where the brief asked for
   a fixed trait, the entry here is the behaviour that trait produces — avoiding
   photographs, dressing to disappear, shrinking in a room — which routes to the
   same grooming, style and humour work and can actually be finished.

2. **Every debuff has a first challenge someone could do today.** If the easiest
   remediation for an entry is still frightening, the entry is pitched wrong. Rung
   one is usually under five minutes and often involves no other person at all.

Run: python3 android/tools/gen_debuffs.py
"""

import json
import os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "audit")

# Budget tiers. 0 = costs nothing, ever. 1 = the price of a coffee. 2 = a real purchase.
FREE, CHEAP, PREMIUM = 0, 1, 2

CATEGORIES = [
    {
        "id": "kinesics",
        "name": "Physical presence",
        "blurb": "How you hold yourself, before anyone has heard you speak.",
        "note": "This is the fastest category to move. Posture and gaze are habits, "
                "and habits answer to reps.",
    },
    {
        "id": "conversation",
        "name": "Conversation",
        "blurb": "What happens once you are talking.",
        "note": "Almost everything here is a pattern you learned to stay safe. It "
                "worked. It is also why conversations stall.",
    },
    {
        "id": "presentation",
        "name": "Presentation",
        "blurb": "Grooming, clothes, and the signal they send before you arrive.",
        "note": "The cheapest points in the whole app are here. Fit and upkeep are "
                "almost entirely effort, not money or genetics.",
    },
    {
        "id": "mindset",
        "name": "Mindset",
        "blurb": "What runs in your head before, during and after.",
        "note": "The slowest category, and the one the others feed. Expect this to "
                "shift last, from evidence rather than from arguing with yourself.",
    },
]

# id, label (first person, plain), detail, tags used by the router
DEBUFFS = [
    # ---------------------------------------------------------------- kinesics
    ("posture_slump", "kinesics", "I stand and sit hunched over",
     "Shoulders forward, chest closed, chin down.", ["posture", "presence"]),
    ("avoids_eye_contact", "kinesics", "I look away when someone looks at me",
     "Eye contact never starts, or I break it first every time.", ["eye_contact", "presence"]),
    ("speaks_too_quietly", "kinesics", "People ask me to repeat myself",
     "I get through a sentence and nobody has heard the end of it.", ["voice", "presence"]),
    ("mumbles", "kinesics", "I trail off at the end of sentences",
     "The start is fine. The last few words dissolve.", ["voice", "diction"]),
    ("fidgets", "kinesics", "My hands are always doing something",
     "Phone, sleeve, keys, face. Never still.", ["stillness", "presence"]),
    ("closed_body", "kinesics", "I stand with my arms crossed or in my pockets",
     "Arms folded, hands hidden, angled away from the room.", ["posture", "openness"]),
    ("walks_head_down", "kinesics", "I walk looking at the ground",
     "Eyes on the pavement from door to door.", ["posture", "eye_contact"]),
    ("takes_up_no_space", "kinesics", "I make myself small in a room",
     "Back to the wall, knees together, out of the way.", ["presence", "space"]),
    ("rushed_movement", "kinesics", "I move fast and jerky when I am nervous",
     "Quick, clipped, like I am trying to get it over with.", ["stillness", "pace"]),
    ("no_gestures", "kinesics", "My hands stay frozen while I talk",
     "Everything happens from the neck up.", ["gesture", "expression"]),
    ("flat_expression", "kinesics", "My face does not move much",
     "People tell me they cannot read me, or that I look annoyed.", ["expression", "warmth"]),
    ("stiff_greeting", "kinesics", "I find hellos and handshakes awkward",
     "The first three seconds of every meeting are a mess.", ["greeting", "presence"]),

    # ------------------------------------------------------------ conversation
    ("conversational_narcissist", "conversation", "I bring most topics back to me",
     "Someone tells me something and I answer with my version of it.", ["listening", "questions"]),
    ("interrupts", "conversation", "I talk over people without meaning to",
     "I start my sentence before theirs has finished.", ["listening", "pace"]),
    ("over_apologizes", "conversation", "I say sorry when nothing is wrong",
     "Sorry for asking, sorry for existing, sorry for the delay.", ["directness", "self_worth"]),
    ("cant_hold_silence", "conversation", "I fill every pause immediately",
     "Two seconds of quiet and I say anything to end it.", ["stillness", "pace"]),
    ("people_pleaser", "conversation", "I cannot say no",
     "I agree to things I do not want and resent it later.", ["boundaries", "directness"]),
    ("overshares_early", "conversation", "I tell people too much, too fast",
     "Twenty minutes in and I have handed over the worst year of my life.", ["calibration", "pace"]),
    ("monotone", "conversation", "My voice stays on one note",
     "Same pitch, same speed, whatever I am saying.", ["voice", "expression"]),
    ("no_questions", "conversation", "I do not ask people anything about themselves",
     "I answer what I am asked and the conversation dies there.", ["questions", "curiosity"]),
    ("interview_mode", "conversation", "I ask questions like a checklist",
     "Where are you from, what do you do, and then nothing.", ["questions", "depth"]),
    ("nervous_laugh", "conversation", "I laugh at things that are not funny",
     "It comes out to fill space and it sounds like it.", ["voice", "stillness"]),
    ("cant_tell_stories", "conversation", "My stories land flat",
     "I get to the end and realise there was no end.", ["stories", "expression"]),
    ("agrees_with_everything", "conversation", "I agree even when I do not",
     "It is easier than having the other conversation.", ["opinions", "directness"]),
    ("no_opinions", "conversation", "I say 'I do not mind' to everything",
     "Where to eat, what to watch, what I think. No input.", ["opinions", "directness"]),
    ("exits_badly", "conversation", "I do not know how to end a conversation",
     "I stay too long, or leave mid-sentence.", ["exits", "calibration"]),

    # ------------------------------------------------------------ presentation
    ("ill_fitting_clothes", "presentation", "My clothes do not fit properly",
     "Too big, too long, bought without trying on.", ["fit", "style"]),
    ("no_personal_style", "presentation", "I dress to not be noticed",
     "Neutral, safe, nothing that says anything.", ["style", "identity"]),
    ("grooming_neglect", "presentation", "I let hair and nails go too long",
     "I deal with it when it becomes a problem, not before.", ["grooming", "upkeep"]),
    ("hygiene_slips", "presentation", "I skip the basics when nobody is watching",
     "Days at home turn into days without the routine.", ["hygiene", "upkeep"]),
    ("worn_out_clothes", "presentation", "My clothes are worn out and I have not replaced them",
     "Faded, bobbled, stretched. Still in rotation.", ["upkeep", "fit"]),
    ("one_outfit", "presentation", "I wear the same thing every day",
     "One combination that works, on repeat.", ["style", "identity"]),
    ("avoids_photos", "presentation", "I get out of every photo",
     "I step behind someone or offer to take it.", ["self_image", "style"]),
    ("avoids_mirrors", "presentation", "I do not look at myself properly",
     "A glance to check nothing is wrong, never an actual look.", ["self_image", "grooming"]),
    ("neglected_shoes", "presentation", "My shoes are the worst thing I own",
     "Everything else is fine and then there are the shoes.", ["fit", "upkeep"]),
    ("looks_tired", "presentation", "I look tired all the time",
     "Sleep, water and daylight are all somewhere near zero.", ["upkeep", "energy"]),

    # ----------------------------------------------------------------- mindset
    ("fear_of_rejection", "mindset", "A 'no' ruins my week",
     "One refusal and I am done trying for days.", ["rejection", "resilience"]),
    ("assumes_judgement", "mindset", "I assume people are judging me",
     "Every room is a panel and I am being scored.", ["reframing", "spotlight"]),
    ("neediness", "mindset", "I need people to like me straight away",
     "I adjust myself in real time to get approval.", ["neediness", "self_worth"]),
    ("catastrophizes", "mindset", "I replay small moments for days",
     "One awkward sentence, rerun for a week.", ["reframing", "rumination"]),
    ("mind_reading", "mindset", "I decide what people think without asking",
     "They went quiet, so I know exactly what it meant.", ["reframing", "assumptions"]),
    ("pre_rejects_self", "mindset", "I do not try, so I cannot lose",
     "I talk myself out of it before anyone gets the chance to.", ["rejection", "action"]),
    ("compares_constantly", "mindset", "I measure myself against everyone",
     "Every room has a ranking and I know where I am in it.", ["comparison", "self_worth"]),
    ("outcome_dependent", "mindset", "One bad interaction defines my whole day",
     "It goes badly at 10am and the day is written off.", ["resilience", "reframing"]),
    ("waits_to_be_chosen", "mindset", "I wait for people to approach me",
     "I am available, visible, and never the one who starts it.", ["initiative", "action"]),
    ("feels_like_imposition", "mindset", "I feel like I am imposing on people",
     "Asking for anything feels like taking something.", ["self_worth", "directness"]),
    ("perfection_paralysis", "mindset", "I will not start until I am ready",
     "The bar for starting is set where the bar for finishing should be.", ["action", "standards"]),
    ("no_self_narrative", "mindset", "I cannot say anything interesting about myself",
     "Asked what I am into, I go blank.", ["identity", "stories"]),
]

# debuff id -> three graded remediations. Rung 1 is doable today, alone, for free.
# (directive, why, minutes, cost, tier, exposure, pillar)
REMEDIATION = {
    "posture_slump": [
        ("Set an alarm for three random times today. Each time it goes off, pull your shoulders back and down and hold it for thirty seconds.",
         "You cannot fix posture by deciding to. You fix it by interrupting it, over and over, until the corrected position stops feeling like a costume.", 5, 0, FREE, 1, "ACTIVITY"),
        ("Walk one full street with your shoulders back, chest open and chin level. Notice how much you want to collapse back down.",
         "The urge to slump is not muscle weakness, it is a habit of taking up less room. Feel it, keep walking.", 10, 0, FREE, 3, "ACTIVITY"),
        ("Stand at the back of a room, in the open, for five minutes. No wall, no phone, no corner.",
         "Slumping and hiding are the same instinct. This is the version of it you can practise deliberately.", 10, 0, FREE, 5, "SOCIAL"),
    ],
    "avoids_eye_contact": [
        ("Hold eye contact with a cashier or barista until they look away first. Once.",
         "A transaction is the safest possible place to practise: it has a script, a fixed length and a guaranteed end.", 5, 0, FREE, 3, "SOCIAL"),
        ("Have one conversation today where you hold eye contact for the whole of your own sentences.",
         "Most people break contact while talking, not while listening. That is the half that reads as evasive.", 10, 0, FREE, 5, "SOCIAL"),
        ("Ask a stranger something and hold their eyes through their entire answer, nodding, without looking away once.",
         "This is the rep that actually changes how you are read. It will feel like far too much. It is not.", 10, 0, FREE, 7, "SOCIAL"),
    ],
    "speaks_too_quietly": [
        ("Read three paragraphs out loud, alone, at a volume that feels about 30 per cent too loud.",
         "Your calibration is off, not your voice. What sounds like shouting from inside your head is normal from a metre away.", 6, 0, FREE, 1, "ACTIVITY"),
        ("Order something today at a volume where the person does not have to lean in. Notice they do not react.",
         "The proof is that nothing happens. Nobody flinches, nobody stares. That is the information you are missing.", 5, 0, FREE, 3, "SOCIAL"),
        ("Ask a question in a group of four or more people, loud enough that everyone hears it the first time.",
         "Being heard the first time is the whole skill. Repeating yourself teaches the room that you can be talked over.", 15, 0, FREE, 6, "SOCIAL"),
    ],
    "mumbles": [
        ("Record yourself saying three sentences on your phone. Listen back. Note exactly where you drop off.",
         "You almost certainly do not know what you sound like. Two minutes of evidence beats a year of guessing.", 6, 0, FREE, 1, "ACTIVITY"),
        ("Say six sentences out loud today landing hard on the final word of each one.",
         "Trailing off is a way of retracting what you just said. Landing the last word is how you stop apologising for the sentence.", 8, 0, FREE, 3, "ACTIVITY"),
        ("Have a conversation where you deliberately finish every sentence at full volume, even the ones you regret starting.",
         "Especially the ones you regret. That is where the mumbling lives.", 15, 0, FREE, 5, "SOCIAL"),
    ],
    "fidgets": [
        ("Sit still for three minutes with your hands flat on your legs. No phone in reach.",
         "You have to know what still feels like before you can find it under pressure.", 5, 0, FREE, 1, "ACTIVITY"),
        ("Have one conversation today with your phone in your pocket the entire time.",
         "The phone is the most visible tell you have. Removing it fixes half of this on its own.", 10, 0, FREE, 3, "SOCIAL"),
        ("Spend twenty minutes in public with nothing in your hands and nothing to look at.",
         "Fidgeting is a way of not being present. This is the exposure that treats it.", 20, 0, FREE, 5, "ACCESS"),
    ],
    "closed_body": [
        ("Spend ten minutes today with your hands out of your pockets and your arms uncrossed. Wherever you are.",
         "You will not know what to do with your hands. That feeling is the point, and it passes in about a week.", 10, 0, FREE, 2, "ACTIVITY"),
        ("Stand through one whole conversation with your body square to the other person.",
         "Angling away is how you signal that you are half-gone. Squaring up is uncomfortable and reads as interested.", 10, 0, FREE, 4, "SOCIAL"),
        ("Sit in a public place for fifteen minutes with open posture — arms uncrossed, hands visible, feet apart.",
         "Open posture in a room full of strangers is the drill. Doing it while nobody is watching does not transfer.", 15, 0, FREE, 5, "ACCESS"),
    ],
    "walks_head_down": [
        ("Walk to the end of your street and back with your eyes on the horizon, not the ground.",
         "You know the pavement is there. Looking at it is a habit of avoiding the possibility of a face.", 8, 0, FREE, 2, "ACCESS"),
        ("Walk fifteen minutes anywhere, looking at the faces of people coming towards you.",
         "You do not have to smile or speak. Just look. That is the entire task.", 15, 0, FREE, 4, "ACCESS"),
        ("Walk a busy street and make brief eye contact with ten people. Count them.",
         "Counting turns it from an ordeal into a rep. Ten is enough to notice that nothing bad happened.", 20, 0, FREE, 6, "ACCESS"),
    ],
    "takes_up_no_space": [
        ("Sit somewhere public without folding yourself up. Feet apart, arms on the rests, for five minutes.",
         "You are allowed the seat you paid for. This is what that feels like physically.", 8, 0, FREE, 3, "ACCESS"),
        ("Stand in the middle of a room rather than at its edge for a full ten minutes.",
         "The wall is a hiding place. Stepping off it is the smallest version of being seen.", 12, 0, FREE, 5, "SOCIAL"),
        ("Walk into a busy place and take a seat in the centre of it, not the corner.",
         "Where you sit is a statement you are making without noticing. Make a different one.", 20, 0, FREE, 6, "ACCESS"),
    ],
    "rushed_movement": [
        ("Do one ordinary thing today at half speed. Making a drink, walking to the door, unlocking your phone.",
         "Speed under pressure is a way of getting out. Slow is a skill, and it is trainable on the boring stuff first.", 5, 0, FREE, 1, "ACTIVITY"),
        ("Walk somewhere today deliberately slower than everyone around you.",
         "Being overtaken and not speeding up is the rep. Notice the pull to match their pace.", 12, 0, FREE, 3, "ACCESS"),
        ("Have a conversation where you pause for one full second before each answer.",
         "A second of silence before you speak reads as considered. It feels like an hour from the inside.", 12, 0, FREE, 5, "SOCIAL"),
    ],
    "no_gestures": [
        ("Describe something out loud, alone, using your hands the whole way through. Anything — a route, a recipe.",
         "Gesture is not decoration; it is how you think out loud. Practising alone removes the audience problem.", 6, 0, FREE, 1, "ACTIVITY"),
        ("Tell someone about something you did this week and let your hands move while you do it.",
         "You will feel theatrical. You will not look theatrical. That gap is the whole issue.", 10, 0, FREE, 3, "SOCIAL"),
        ("Explain something to someone with your hands out of your pockets for the entire explanation.",
         "Pockets are where gesture goes to die. Take the option away and your hands work it out.", 12, 0, FREE, 4, "SOCIAL"),
    ],
    "flat_expression": [
        ("Stand at a mirror and run through five expressions: interested, surprised, amused, confused, warm.",
         "This is not vanity. Most people who read as cold have simply never watched their own face do anything.", 6, 0, FREE, 2, "ACTIVITY"),
        ("React visibly to three things people say to you today. Eyebrows, a nod, an actual smile.",
         "Reaction is what tells someone you are receiving them. Without it they assume you are not.", 10, 0, FREE, 3, "SOCIAL"),
        ("Have a conversation where you deliberately let your face show what you think before you say it.",
         "Face first, words second, is the natural order. Reversing it is what makes people seem guarded.", 15, 0, FREE, 5, "SOCIAL"),
    ],
    "stiff_greeting": [
        ("Practise one greeting out loud five times: eye contact, name, and a short sentence.",
         "The first three seconds are scriptable. Everyone who seems smooth has a script; theirs is just worn in.", 5, 0, FREE, 1, "ACTIVITY"),
        ("Greet three people today with a clear hello and their name if you know it.",
         "Using the name is what turns a noise into a greeting. It costs nothing and it is remembered.", 8, 0, FREE, 3, "SOCIAL"),
        ("Introduce yourself to someone new, first, with your hand out and your name said clearly.",
         "Going first is the hard half. The rest is a handshake you have already practised.", 12, 0, FREE, 6, "SOCIAL"),
    ],
    "conversational_narcissist": [
        ("In your next conversation, ask one follow-up question about something they said, before saying anything about yourself.",
         "One follow-up is the smallest possible version of listening. It is also the thing most people never do.", 8, 0, FREE, 2, "SOCIAL"),
        ("Have a five-minute conversation where you ask at least three questions and never once relate the topic back to yourself.",
         "You will feel like you contributed nothing. They will come away thinking it went well. Sit with that mismatch — it is the lesson.", 12, 0, FREE, 4, "SOCIAL"),
        ("Talk to someone for fifteen minutes and afterwards write down three facts about their life you did not know before.",
         "If you cannot fill three lines, you were not listening, you were waiting.", 20, 0, FREE, 5, "SOCIAL"),
    ],
    "interrupts": [
        ("For one conversation, wait until the other person has fully stopped before you begin.",
         "The pause you are jumping into is usually them thinking. Interrupting it stops the good part.", 10, 0, FREE, 2, "SOCIAL"),
        ("Count to two in your head after each thing someone says today, before you reply.",
         "Two seconds is not a delay, it is the normal rhythm you have been cutting into.", 10, 0, FREE, 3, "SOCIAL"),
        ("Let someone finish a long story without adding a single thing to it.",
         "Not a related anecdote, not a correction. Just the story, theirs, whole.", 15, 0, FREE, 4, "SOCIAL"),
    ],
    "over_apologizes": [
        ("Count how many times you say sorry today. Write down the number and nothing else.",
         "You cannot change a reflex you have not measured. The number is usually a surprise.", 5, 0, FREE, 1, "ACTIVITY"),
        ("Make one request today without saying sorry, and without explaining why you are asking.",
         "The justification is the apology in a coat. Ask the thing and stop talking.", 8, 0, FREE, 4, "SOCIAL"),
        ("Replace three apologies with a thank-you today. 'Sorry I am late' becomes 'thanks for waiting.'",
         "Same acknowledgement, none of the self-deduction, and it gives the other person something instead of taking.", 10, 0, FREE, 4, "SOCIAL"),
    ],
    "cant_hold_silence": [
        ("Let one silence in one conversation run for three full seconds without filling it.",
         "Three seconds is nothing on a clock and enormous in a conversation. That gap is exactly what you need to get used to.", 8, 0, FREE, 3, "SOCIAL"),
        ("Ask someone a real question and say nothing at all until they answer.",
         "Filling the pause after your own question is how you answer it for them. Wait.", 10, 0, FREE, 4, "SOCIAL"),
        ("Sit with someone you know for five minutes without either of you needing to talk.",
         "Comfortable silence with one person is the proof that silence is not danger.", 12, 0, FREE, 5, "SOCIAL"),
    ],
    "people_pleaser": [
        ("Say no to one small thing today. A request, an invitation, a favour you do not want to do.",
         "Start where the stakes are low enough that the sky obviously will not fall.", 5, 0, FREE, 4, "SOCIAL"),
        ("Say no to something without offering an alternative or an excuse. 'I cannot do that' is a complete answer.",
         "The excuse invites negotiation. A plain no does not, and people respect it more than you expect.", 8, 0, FREE, 6, "SOCIAL"),
        ("State a preference that you know differs from the group's, out loud, and hold it.",
         "This is the version that actually costs something, and the version that gets you treated as a person with a shape.", 15, 0, FREE, 7, "SOCIAL"),
    ],
    "overshares_early": [
        ("Have one conversation where you answer questions and add nothing unprompted.",
         "Not coldness — calibration. You are learning where the other person's interest actually stops.", 10, 0, FREE, 2, "SOCIAL"),
        ("Talk to someone new and keep everything you say to things you would say in front of a colleague.",
         "That filter is not dishonesty. It is the pace at which trust is normally built.", 15, 0, FREE, 4, "SOCIAL"),
        ("Notice one moment today where you want to tell someone something heavy, and hold it for a week instead.",
         "Depth lands when it is earned. The same sentence a month later is intimacy; today it is a burden.", 10, 0, FREE, 5, "SOCIAL"),
    ],
    "monotone": [
        ("Read a paragraph out loud three times: once bored, once excited, once conspiratorial.",
         "You have the range. You have just stopped using it, probably because using it once got a reaction you did not want.", 6, 0, FREE, 1, "ACTIVITY"),
        ("Tell someone about something you actually like today, and let your voice do what it wants.",
         "Start with real enthusiasm. Performing interest you do not have is a much harder skill.", 10, 0, FREE, 3, "SOCIAL"),
        ("Tell a story out loud to someone with a deliberate change of pace and volume in the middle.",
         "Pace and volume are what make people lean in. Flat delivery makes good material sound like a weather report.", 15, 0, FREE, 5, "SOCIAL"),
    ],
    "no_questions": [
        ("Ask one person one question about themselves today. Any question.",
         "One. The bar is deliberately on the floor, because the pattern is what matters, not the depth.", 5, 0, FREE, 2, "SOCIAL"),
        ("Ask three questions in one conversation and let each answer lead to the next one.",
         "Questions that come out of the last answer are the difference between a conversation and a form.", 12, 0, FREE, 4, "SOCIAL"),
        ("Have a conversation built entirely on curiosity — every turn of yours is a question or a reaction.",
         "This is a drill, not a way to live. But it teaches you how much people will give you if you just ask.", 20, 0, FREE, 5, "SOCIAL"),
    ],
    "interview_mode": [
        ("Ask one 'how' or 'why' question today instead of a 'what' or 'where'.",
         "Facts end. Reasons open. It is a one-word change with a completely different result.", 6, 0, FREE, 2, "SOCIAL"),
        ("When someone answers a question, respond with what you thought about their answer before asking the next one.",
         "The reaction is what turns interrogation into conversation. Without it you are collecting data.", 10, 0, FREE, 3, "SOCIAL"),
        ("Have a conversation where you follow one topic all the way down instead of moving to a new one.",
         "Depth on one subject beats breadth across ten. Changing topic is usually you escaping.", 15, 0, FREE, 4, "SOCIAL"),
    ],
    "nervous_laugh": [
        ("Notice every time you laugh today when nothing was funny. Just count it.",
         "It is a reflex for filling space. Counting makes it visible, which is most of the work.", 5, 0, FREE, 1, "ACTIVITY"),
        ("Replace one nervous laugh with a two-second pause.",
         "The pause does the same job — it buys you a moment — without telling everyone you are uncomfortable.", 8, 0, FREE, 3, "SOCIAL"),
        ("Get through one whole conversation laughing only at things you found genuinely funny.",
         "Your real laugh is worth something. Spending it on nothing devalues it.", 12, 0, FREE, 4, "SOCIAL"),
    ],
    "cant_tell_stories": [
        ("Write down one thing that happened to you this year in five sentences: setup, problem, turn, end, why it stuck.",
         "Stories are a structure, not a talent. Write one down once and you own the shape forever.", 10, 0, FREE, 1, "ACTIVITY"),
        ("Tell that story out loud to one person. Keep the structure. Do not apologise for it at the end.",
         "The apology at the end — 'anyway, you had to be there' — is what actually kills stories.", 12, 0, FREE, 4, "SOCIAL"),
        ("Tell a story to a group of three or more and stop talking the moment you hit the ending.",
         "Stopping cleanly is the hardest part. Trailing past the end is what makes people check their phones.", 15, 0, FREE, 6, "SOCIAL"),
    ],
    "agrees_with_everything": [
        ("Disagree with one small thing today. A film, a restaurant, the weather.",
         "Low stakes on purpose. You are testing whether disagreement destroys anything. It does not.", 5, 0, FREE, 3, "SOCIAL"),
        ("Say 'I see it differently' once, and then explain how, without softening it into agreement.",
         "The softening is the habit. Notice how badly you want to add 'but you are probably right'.", 10, 0, FREE, 5, "SOCIAL"),
        ("Hold a disagreement for a full conversation without conceding to end the discomfort.",
         "Conceding to end discomfort is not kindness, it is exit. People can tell, and it costs you their respect.", 15, 0, FREE, 6, "SOCIAL"),
    ],
    "no_opinions": [
        ("Pick the restaurant, the film, or the route today. Do not ask what anyone else prefers first.",
         "Deciding is a muscle. 'I do not mind' is what it looks like when it has not been used in years.", 5, 0, FREE, 3, "SOCIAL"),
        ("Give a real answer to one 'what do you think?' today, with a reason attached.",
         "The reason is what makes it an opinion rather than a noise.", 8, 0, FREE, 4, "SOCIAL"),
        ("State a preference that might make you look bad — a film you love that is not respectable, a thing you hate that everyone likes.",
         "Taste that costs nothing signals nothing. The slightly embarrassing preference is the one that makes you a person.", 12, 0, FREE, 6, "SOCIAL"),
    ],
    "exits_badly": [
        ("Learn one exit line and say it out loud until it sounds normal. 'I am going to go and say hello to a few people — good to meet you.'",
         "Everyone who leaves conversations gracefully is using a line. They are not improvising it.", 5, 0, FREE, 1, "ACTIVITY"),
        ("End one conversation today deliberately, while it is still going well.",
         "Leaving on a high is the whole skill. Staying until it dies is what makes the next one harder to start.", 8, 0, FREE, 4, "SOCIAL"),
        ("Leave three conversations at an event, cleanly, and start three more.",
         "Circulation is a numbers game and the exit is the bottleneck. Fix the exit and the rest follows.", 25, 0, FREE, 6, "SOCIAL"),
    ],
    "ill_fitting_clothes": [
        ("Try on everything in your wardrobe and put anything that does not fit into a separate pile. Do not throw it away yet.",
         "Fit is the single highest-return thing in this entire category, and it is free to audit.", 30, 0, FREE, 1, "ACTIVITY"),
        ("Take one thing you like but that does not fit to a tailor and have it altered.",
         "A cheap garment that fits reads better than an expensive one that does not. This is not opinion; it is what people actually see.", 30, 15, CHEAP, 3, "ACTIVITY"),
        ("Buy one item in your correct size, tried on in a shop, not ordered online.",
         "The number on the label is not your size. The garment on your body is.", 60, 40, PREMIUM, 4, "ACTIVITY"),
    ],
    "no_personal_style": [
        ("Save five photographs of people whose clothes you would actually want to wear.",
         "You cannot build a style out of nothing. Start by admitting what you like when nobody is judging.", 15, 0, FREE, 1, "ACTIVITY"),
        ("Identify what those five have in common — colour, shape, era, formality — and write down the three rules.",
         "Style is a small set of rules applied consistently. Nothing more mysterious than that.", 15, 0, FREE, 1, "ACTIVITY"),
        ("Wear one deliberate thing that is not neutral, out of the house, all day.",
         "Being slightly noticeable is the discomfort here, and it is exactly what dressing to disappear was avoiding.", 20, 0, FREE, 5, "ACCESS"),
    ],
    "grooming_neglect": [
        ("Cut your nails and deal with your hairline today. Twenty minutes.",
         "This is the cheapest visible improvement available to any human being, and it is entirely under your control.", 20, 0, FREE, 1, "ACTIVITY"),
        ("Book the haircut you have been putting off.",
         "Putting it off is not laziness, it is usually about being looked at closely for twenty minutes. Book it anyway.", 10, 20, CHEAP, 4, "ACTIVITY"),
        ("Set a repeating grooming schedule — nails weekly, hair every four weeks — and put it in your calendar.",
         "Upkeep beats effort. A schedule means it never becomes a project again.", 10, 0, FREE, 1, "ACTIVITY"),
    ],
    "hygiene_slips": [
        ("Do the full routine today even though you are not going anywhere. Shower, teeth, clean clothes.",
         "The days nobody sees you are the ones that set the baseline for the days they do.", 20, 0, FREE, 1, "ACTIVITY"),
        ("Do the full routine every day this week, including the days you stay in.",
         "Seven days is enough to move it from a decision to a default.", 20, 0, FREE, 2, "ACTIVITY"),
        ("Replace one worn-out basic — toothbrush, towel, deodorant, bedding.",
         "The tools degrade quietly. Replacing them is a real improvement disguised as admin.", 20, 15, CHEAP, 1, "ACTIVITY"),
    ],
    "worn_out_clothes": [
        ("Find the three most worn-out things you still wear and take them out of rotation today.",
         "You have stopped seeing them. Everyone else has not.", 15, 0, FREE, 1, "ACTIVITY"),
        ("Replace one of the three with something plain and new.",
         "One good plain item beats five faded ones. Start with whatever you wear most.", 45, 30, CHEAP, 3, "ACTIVITY"),
        ("Rebuild one full outfit that fits, is unworn and that you would be happy to be photographed in.",
         "One complete outfit removes the excuse. There is now always something to wear.", 90, 80, PREMIUM, 4, "ACTIVITY"),
    ],
    "one_outfit": [
        ("Wear a different combination of things you already own today.",
         "You probably own more combinations than you use. This costs nothing to test.", 10, 0, FREE, 2, "ACTIVITY"),
        ("Build three separate outfits from your existing wardrobe and photograph each one.",
         "The photographs are the point — they remove the morning decision, which is what collapsed you to one outfit.", 30, 0, FREE, 2, "ACTIVITY"),
        ("Wear each of the three out of the house on separate days this week.",
         "Rotation is a signal of self-attention. It is noticed without being noticed.", 15, 0, FREE, 3, "ACCESS"),
    ],
    "avoids_photos": [
        ("Take one photograph of yourself today. Nobody has to see it. Do not delete it immediately.",
         "The flinch when you see your own face is a habit like any other, and it fades with exposure.", 5, 0, FREE, 2, "ACTIVITY"),
        ("Be in one photograph with other people and do not ask to take it instead.",
         "Offering to be the photographer is the polite version of hiding. Everyone reads it correctly.", 8, 0, FREE, 4, "SOCIAL"),
        ("Take a decent photograph of yourself, deliberately — good light, an outfit you chose — and keep it.",
         "Not vanity. Having one image of yourself you do not hate makes a surprising number of other things easier.", 20, 0, FREE, 5, "ACTIVITY"),
    ],
    "avoids_mirrors": [
        ("Look at yourself in a mirror for sixty seconds. Not to check anything. Just look.",
         "You have been glancing to make sure nothing is wrong, which is not the same as ever having seen yourself.", 5, 0, FREE, 2, "ACTIVITY"),
        ("Look in the mirror and write down three things that are genuinely working and one you can change this week.",
         "Both halves matter. Only the second half is a to-do list; the first half is the part you have been skipping.", 10, 0, FREE, 3, "ACTIVITY"),
        ("Get dressed in front of a full-length mirror and adjust things until it actually looks right.",
         "Most bad outfits are two adjustments away from good ones, and you cannot see either without the mirror.", 15, 0, FREE, 3, "ACTIVITY"),
    ],
    "neglected_shoes": [
        ("Clean your most-worn pair properly. Twenty minutes with a cloth.",
         "Shoes are the first thing a certain kind of person checks and the last thing most people maintain.", 20, 0, FREE, 1, "ACTIVITY"),
        ("Replace the laces and insoles of your main pair.",
         "Under a tenner, and it makes an old pair read as looked-after rather than finished.", 20, 10, CHEAP, 1, "ACTIVITY"),
        ("Buy one pair of plain shoes that fit properly and suit most of what you own.",
         "One versatile pair fixes more outfits than any other single purchase.", 60, 70, PREMIUM, 3, "ACTIVITY"),
    ],
    "looks_tired": [
        ("Go outside for fifteen minutes of daylight within an hour of waking up.",
         "Morning light is the cheapest available intervention on sleep, mood and how your face looks by evening.", 15, 0, FREE, 1, "ACCESS"),
        ("Put your phone outside the bedroom tonight and go to bed thirty minutes earlier.",
         "Thirty minutes is small enough to actually do and large enough to show.", 10, 0, FREE, 2, "ACTIVITY"),
        ("Hold a consistent sleep and wake time for seven days, including the weekend.",
         "Consistency does more than duration. The weekend is where most people undo the week.", 20, 0, FREE, 3, "ACTIVITY"),
    ],
    "fear_of_rejection": [
        ("Ask for something small you expect to be refused. A discount, a favour, an upgrade.",
         "Collect a no on purpose, in a place where it costs nothing. The point is to survive one deliberately.", 10, 0, FREE, 5, "SOCIAL"),
        ("Collect three refusals today. Count them as the target — you are trying to reach three.",
         "Inverting the goal is the trick. When the no is the score, it stops being the punishment.", 25, 0, FREE, 7, "SOCIAL"),
        ("Ask for something you actually want, from someone who might say no.",
         "The graded practice was for this. A real ask, with something real at stake.", 20, 0, FREE, 9, "SOCIAL"),
    ],
    "assumes_judgement": [
        ("In one public place, count how many people are actually looking at you. Write the number down.",
         "It is almost always zero. Your estimate is not evidence; the count is.", 10, 0, FREE, 2, "ACCESS"),
        ("Do one slightly odd thing in public — ask an unusual question, wear something loud — and note what happens.",
         "Nothing happens. That is the finding, and you have to gather it yourself for it to count.", 15, 0, FREE, 5, "ACCESS"),
        ("Write down what you assume people thought after one interaction, then list the evidence for each assumption.",
         "The evidence column comes out empty. Seeing it empty on paper does more than being told.", 12, 0, FREE, 2, "ACTIVITY"),
    ],
    "neediness": [
        ("Have one conversation where you do not try to be liked. Say the true thing rather than the smooth one.",
         "Neediness is not wanting connection, it is editing yourself in real time to secure it. Stop editing for ten minutes.", 12, 0, FREE, 5, "SOCIAL"),
        ("Do not follow up on one message that has gone unanswered for a day.",
         "The second message is almost always for you, not them. Sitting with the silence is the rep.", 5, 0, FREE, 5, "SOCIAL"),
        ("Make a plan you would enjoy alone, and go, without inviting anyone.",
         "Having somewhere to be that does not depend on anyone is what actually dissolves this. It is not a mindset trick.", 60, 0, FREE, 4, "ACCESS"),
    ],
    "catastrophizes": [
        ("Write down the moment you have been replaying. One sentence. Then write what the other person was probably thinking about.",
         "They were thinking about themselves. Everyone is. That is not cynicism, it is the arithmetic of attention.", 8, 0, FREE, 1, "ACTIVITY"),
        ("Set a ten-minute timer to think about it, properly and fully. When it ends, stop and do something else.",
         "Rumination expands to fill available time. Boxing it is more effective than trying not to do it.", 12, 0, FREE, 2, "ACTIVITY"),
        ("Go back to a place where something awkward happened and do the ordinary version of it again.",
         "The memory only stays enormous while it stays untested. Returning shrinks it faster than any amount of thinking.", 25, 0, FREE, 6, "ACCESS"),
    ],
    "mind_reading": [
        ("Notice one moment today where you decided what someone thought. Write down what you concluded.",
         "You cannot challenge the habit until you catch it happening.", 6, 0, FREE, 1, "ACTIVITY"),
        ("Ask one person directly what they meant, instead of deciding.",
         "Asking feels enormous and lands as completely normal. That gap is the whole distortion.", 8, 0, FREE, 5, "SOCIAL"),
        ("For a whole day, treat every ambiguous reaction as neutral rather than negative.",
         "The default is a choice you made a long time ago. Try the other one for a day and compare.", 15, 0, FREE, 3, "ACTIVITY"),
    ],
    "pre_rejects_self": [
        ("Do one thing today that you would normally talk yourself out of. Anything, however small.",
         "The pattern matters more than the stakes. You are proving the talking-out-of can be overridden.", 10, 0, FREE, 4, "ACTIVITY"),
        ("Apply, ask or turn up for one thing you assume you are not good enough for.",
         "Letting someone else make the decision is the entire exercise. You have been doing their job for them.", 25, 0, FREE, 7, "SOCIAL"),
        ("Say yes to something within ten seconds, before the argument against it can assemble.",
         "The argument is fast but it is not instant. Ten seconds beats it.", 15, 0, FREE, 6, "ACTIVITY"),
    ],
    "compares_constantly": [
        ("Notice three comparisons today and write down what you were actually measuring.",
         "It is almost never the thing you think. Usually it is one narrow trait, ranked against someone's edited outside.", 8, 0, FREE, 1, "ACTIVITY"),
        ("Spend a day off the feeds that trigger it most.",
         "You are not weak-willed. The comparison is the product being sold to you.", 10, 0, FREE, 3, "ACTIVITY"),
        ("Write down what you can actually do now that you could not do a year ago.",
         "Comparison against your own past is the only version of this that produces anything useful.", 12, 0, FREE, 1, "ACTIVITY"),
    ],
    "outcome_dependent": [
        ("After the next thing that goes badly, do one more small social thing the same day.",
         "The write-off is the damage, not the event. Doing one more thing keeps the day open.", 15, 0, FREE, 5, "SOCIAL"),
        ("Set a target of attempts today rather than results. Three conversations, regardless of how they go.",
         "Counting attempts moves the score onto the half you control. That is the entire fix.", 25, 0, FREE, 6, "SOCIAL"),
        ("Have a day with a deliberately bad interaction in it, and finish the day's plan anyway.",
         "You are training the recovery, not the interaction. Recovery is the rarer skill.", 30, 0, FREE, 7, "SOCIAL"),
    ],
    "waits_to_be_chosen": [
        ("Send one message today to someone you have not spoken to in a while. You go first.",
         "Being available is not the same as being present. Someone has to start, and it has never been you.", 5, 0, FREE, 3, "SOCIAL"),
        ("Start one conversation in person with someone you did not have to talk to.",
         "In person is a different skill from messaging, and it is the one that has atrophied.", 12, 0, FREE, 6, "SOCIAL"),
        ("Invite two people to something specific — a time, a place, a plan.",
         "'We should do something' is not an invitation. A time and a place is.", 20, 0, FREE, 7, "SOCIAL"),
    ],
    "feels_like_imposition": [
        ("Ask someone for something small today. Directions, the time, a recommendation.",
         "Watch how willingly people help. Being asked is not a burden; it is usually a small pleasure.", 5, 0, FREE, 3, "SOCIAL"),
        ("Ask someone for a real favour, without over-explaining or offering to make up for it.",
         "The over-explaining is the imposition. The favour itself is fine.", 12, 0, FREE, 6, "SOCIAL"),
        ("Take up someone's time deliberately — a long conversation, a proper catch-up — without apologising for it once.",
         "People give time to those who seem worth it. Apologising for taking it tells them you are not.", 40, 0, FREE, 5, "SOCIAL"),
    ],
    "perfection_paralysis": [
        ("Do one thing badly today, on purpose, and let it stay done.",
         "The finished bad version teaches you more than the perfect unstarted one, and it is the only one that exists.", 15, 0, FREE, 3, "ACTIVITY"),
        ("Ship something before you think it is ready. Send it, post it, hand it over.",
         "Ready is a feeling that arrives after, not before. Waiting for it is how projects die.", 20, 0, FREE, 5, "ACTIVITY"),
        ("Set a timer for one hour, work on the thing you have been putting off, and stop when it goes.",
         "The timer removes the standard. Whatever exists at the end is the deliverable.", 60, 0, FREE, 4, "ACTIVITY"),
    ],
    "no_self_narrative": [
        ("Write down three things you are actually interested in. Not impressive — actual.",
         "You are not boring. You have simply never assembled the sentence, so you go blank when asked.", 10, 0, FREE, 1, "ACTIVITY"),
        ("Say one of them out loud to someone, with a reason you like it.",
         "The reason is the interesting part. 'I like hiking' is nothing; why you like it is a conversation.", 10, 0, FREE, 4, "SOCIAL"),
        ("Spend two hours on one of them this week so you have something new to say about it.",
         "The long-term answer to having nothing to say is doing more things, not phrasing them better.", 120, 0, FREE, 3, "ACTIVITY"),
    ],
}


def build():
    by_id = {}
    debuffs = []
    for did, cat, label, detail, tags in DEBUFFS:
        assert did not in by_id, f"duplicate debuff id {did}"
        assert cat in {c["id"] for c in CATEGORIES}, f"{did} has unknown category {cat}"
        # First person, but not necessarily starting with "I" — "People ask me to repeat
        # myself" is the user's own account of themselves and reads better than the
        # grammatically uniform version.
        assert any(t in f" {label.lower()} " for t in (" i ", " me ", " my ", " myself ")), \
            f"{did} label is not in the first person: {label}"
        by_id[did] = True
        debuffs.append({"id": did, "cat": cat, "label": label, "detail": detail, "tags": tags})

    rem = []
    for did in by_id:
        rungs = REMEDIATION.get(did)
        assert rungs, f"{did} has no remediation"
        assert len(rungs) == 3, f"{did} has {len(rungs)} rungs, expected 3"
        for i, (do, why, minutes, cost, tier, exp, pillar) in enumerate(rungs, start=1):
            assert pillar in ("ACCESS", "ACTIVITY", "SOCIAL"), f"{did}#{i} bad pillar"
            assert 1 <= exp <= 10, f"{did}#{i} exposure out of range"
            assert (cost == 0) == (tier == FREE), f"{did}#{i} cost and tier disagree"
            rem.append({
                "id": f"{did}_{i}",
                "debuff": did,
                "rung": i,
                "do": do,
                "why": why,
                "min": minutes,
                "cost": cost,
                "tier": tier,
                "exp": exp,
                "pillar": pillar,
            })

    # Rung 1 must be gentle enough that everyone has a first move.
    for r in rem:
        if r["rung"] == 1:
            assert r["exp"] <= 5, f"{r['id']} is too exposed for a first rung ({r['exp']})"
            assert r["tier"] == FREE, f"{r['id']} must be free on the first rung"

    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, "debuffs.json"), "w") as f:
        json.dump({"categories": CATEGORIES, "debuffs": debuffs}, f, indent=1, ensure_ascii=False)
    with open(os.path.join(OUT, "remediation.json"), "w") as f:
        json.dump({"challenges": rem}, f, indent=1, ensure_ascii=False)

    print(f"debuffs.json     {len(debuffs)} debuffs in {len(CATEGORIES)} categories")
    for c in CATEGORIES:
        print(f"    {c['id']:14} {sum(1 for d in debuffs if d['cat'] == c['id'])}")
    print(f"remediation.json {len(rem)} challenges")


if __name__ == "__main__":
    build()
