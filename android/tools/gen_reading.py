#!/usr/bin/env python3
"""
Generates the reading room: Premium's short reads, in packs.

  assets/learn/reading.json

Four packs of five pages, each page five minutes or less. They sit beside the free
thirty-day library and never repeat it: that one is about getting out of your bubble and
talking to people; these are about the things underneath that make it possible — sleep,
moving for your mood, getting unstuck, and a quieter mind.

House style, the same as gen_learn.py and enforced by the checks at the bottom:

  - Short sentences. Any term is explained where it first appears.
  - Every page ends with a **Do this** section: something concrete, today.
  - Every page names where its claims come from, on the last line.
  - Nothing tells the reader what they are. It tells them what to try and what happens.

Run: python3 android/tools/gen_reading.py
"""

import json
import os
import re

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "learn")

# (pack id, title, blurb, icon sprite, [(id, title, summary, minutes, body)])
PACKS = [
    ("sleep", "Sleep, gently", "Five reads on falling asleep without fighting it.", "bed", [
        ("sleep_effort", "Trying harder makes it worse",
         "Sleep is the one thing effort undoes.", 3, """
Most things get better when you try harder. Sleep gets worse.

Sleep arrives when your body stops guarding the day. Lying in bed checking whether
you are asleep yet, counting the hours left, telling yourself you must sleep: all of
that is guarding. It is effort, and effort is the opposite of what sleep needs.

Researchers even measure it. People with insomnia score much higher on *sleep
effort*, the amount of trying they put into falling asleep, than good sleepers do,
and the trying keeps the problem going.

## What to do instead

Lower the stakes. Tonight you are going to bed to rest, not to perform. Rest is
useful on its own: lying still in the dark, breathing slowly, is restorative even if
you never drop off.

Stop checking the clock. Turn it to face the wall. Every look at it is a small
alarm going off.

## Do this

Tonight, when you lie down, say to yourself: "I am here to rest. Sleep can come
or not." Then do not check the time again until morning.

*Broomfield & Espie, 2005, Journal of Sleep Research.*
"""),
        ("one_wake_time", "One time to get up",
         "Your body clock sets itself from the morning, not the night.", 3, """
If you change one thing about your sleep, change this: get up at the same time every
day, weekends included.

Two systems decide when you feel sleepy. One is the pressure to sleep, which builds
up the longer you have been awake. The other is your body clock, which runs on a
roughly daily rhythm. A fixed wake-up time lines them up. Sleep pressure starts
building at the same time each morning, so it peaks at the same time each night.

Lie in after a bad night and you start the pressure late, so the next night is late
too. That is how one bad night becomes a bad week.

## The hard part

Getting up on time after a poor night feels cruel. It is the part that works. The
tiredness you feel that day is sleep pressure, and it will carry you to sleep that
night. A fixed schedule is one of the core parts of cognitive behavioural therapy for
insomnia, which reliably helps people fall asleep faster and wake less.

## Do this

Pick a wake-up time you can keep on Saturday and Sunday too, within half an hour.
Set one alarm for it, and get up when it goes, however the night went.

*Trauer et al., 2015, Annals of Internal Medicine.*
"""),
        ("bed_for_sleep", "Bed is for sleeping",
         "If you cannot sleep, get up. It sounds wrong and it works.", 3, """
Your brain learns what a place is for. If your bed is where you scroll, worry, eat
and lie awake for hours, your brain learns that bed is a place for being awake.

The fix is called *stimulus control*, and it is one of the oldest and best-tested
treatments for insomnia. It is simple and slightly annoying.

## The rules

Use the bed for sleep, and nothing else that keeps you awake.

Go to bed when you are sleepy, not when the clock says so.

If you have been awake for what feels like twenty minutes, get up. Go to another
room, keep the light dim, and do something dull: fold washing, read something boring.
Go back to bed when you feel sleepy again. Repeat as often as it takes.

## Why it works

Every time you get up instead of lying there frustrated, you break the link between
bed and being awake. After a week or two, lying down starts to mean sleep again.

The first few nights can be short. That is the wake-up time and the sleep pressure
doing their work.

## Do this

Tonight, if you are lying awake and it feels like twenty minutes, get up. Do one dull
thing in dim light, and go back only when you are sleepy.

*Bootzin, 1972; part of the therapy reviewed in Trauer et al., 2015.*
"""),
        ("wind_down", "The last hour",
         "Give your mind somewhere to put the day.", 3, """
You cannot go from full speed to asleep. The last hour before bed is the slope down,
and it works better with something on it.

## Lower the light

Bright light late in the evening tells your body clock it is still day. Dim the room
and turn the screen brightness down for the last hour. If you use your phone, use it
for something calm, and stop at the same time each night.

## Put the day down

A lot of lying awake is your mind carrying tomorrow. Give it somewhere else to put
it. In one study, people who spent five minutes before bed writing a to-do list for
the next few days fell asleep faster than people who wrote about what they had
already done. The more specific the list, the faster they fell asleep.

## Keep it the same

A wind-down routine works through repetition, like any habit: the same few things in
the same order become a signal that sleep is next. It can be small: plug the phone in
across the room, wash your face, write the list, lights down.

## Do this

Tonight, five minutes before bed, write tomorrow's to-do list. Be specific:
"email Sam about Thursday", not "work stuff". Then put it out of reach.

*Scullin et al., 2018, Journal of Experimental Psychology: General.*
"""),
        ("caffeine_light", "Coffee, light and the clock",
         "Two things you take in during the day that decide your night.", 3, """
Two things during the day shape your night more than anything you do in bed.

## Caffeine lasts longer than it feels

Caffeine blocks the chemical that builds up as sleep pressure, and it stays in your
body for hours after the buzz fades. In a sleep lab study, caffeine taken six hours
before bed still cut total sleep by more than an hour, even though people did not
feel it was keeping them awake.

So the cup at four in the afternoon is part of the night. A cut-off around lunch is a
good start: early enough that most of it is gone by bedtime.

## Light in the morning

Your body clock sets itself from daylight, and morning light is the strongest signal.
A few minutes outside, or at a window, soon after you get up tells your body when the
day started, which makes it easier to feel sleepy at the right time that night.

Even a cloudy morning outside is far brighter than a lit room inside.

## Do this

Have your last coffee or tea with caffeine before one o'clock today. Tomorrow, within
an hour of getting up, stand outside or at an open window for five minutes.

*Drake et al., 2013, Journal of Clinical Sleep Medicine.*
"""),
    ]),

    ("move", "Move for your mood", "Five reads on what moving does for how you feel.", "sneakers", [
        ("any_movement", "Some is most of it",
         "The biggest gain is from none to a little.", 3, """
The guidelines say adults should do 150 to 300 minutes of moderate activity a week,
and something that works the muscles on two or more days. That sounds like a lot if
you are starting from the sofa.

Here is the part that matters more: the biggest health gain is not from the athlete
who adds an hour. It is from the person who goes from almost nothing to a little.
The guidelines say it plainly: some activity is better than none, and every bit
counts.

## What counts

Anything that makes you breathe a bit harder. A brisk walk, stairs, carrying the
shopping, dancing in the kitchen, cycling to the shop. It does not have to be in one
go. Ten minutes three times is the same as thirty once.

## Why this matters for you

If the target feels impossible, you will not start. If the target is ten minutes,
you will. And once ten minutes is normal, twenty is a small step, not a leap.

That is why training in this app starts with one set, and counts a single walk as a
day.

## Do this

Go for a ten-minute walk today, fast enough that you could talk but not sing. Log it
under Training. That is a day.

*Bull et al., 2020, British Journal of Sports Medicine: the WHO 2020 guidelines.*
"""),
        ("exercise_mood", "What it does for your mood",
         "Moving helps low mood, and harder helps more.", 4, """
Exercise is one of the most reliable things you can do for a low mood, and the
evidence for it is large.

A review in 2024 pulled together 218 trials of exercise for depression. Walking or
jogging, yoga and strength training all reduced symptoms. Harder sessions helped more
than gentle ones, but even light movement like walking helped. People also stuck with
yoga and strength training better than with some other kinds.

## Why it works

Nobody knows every part of it. Moving changes your body's chemistry, improves sleep,
gives you a sense of having done something, and gets you out of the loop of thoughts
for a while. It is also a small win you can have on a day with no other wins.

## The catch

When your mood is low, moving is the last thing you feel like doing. That is not a
sign it will not help. It is a sign of the low mood. Feeling like it usually comes
after you start, not before.

## A note

Exercise helps, and it is not a replacement for proper help if you need it. If your
mood has been low for weeks, talk to your doctor too.

## Do this

Next time you feel low, move for ten minutes first, then decide how you feel. Walk,
stretch or do one set of something. Notice the difference, if there is one.

*Noetel et al., 2024, BMJ.*
"""),
        ("strength_twice", "Strength, twice a week",
         "Muscles are not just for looks: they are for the rest of your life.", 3, """
Strength training is not only for people who want to look a certain way. Muscle is
what gets you up stairs, carries the shopping, and keeps you steady on your feet for
decades. You lose some every year after your thirties unless you use it.

That is why the guidelines ask for something that works the muscles on two or more
days a week, on top of moving around.

## You do not need a gym

Your own body is enough to start: push-ups against a wall, sitting down and standing
up from a chair, a plank on the floor. When those get easy, there is a harder
version of each. That is how the training ladders in this app work.

## What a session needs

Not much. Work the main groups: something that pushes, something that pulls,
something for your legs, something for your middle. Two or three sets of each, close
to as many as you can do well. Twenty minutes is plenty.

## Rest is part of it

Muscles get stronger between sessions, not during them. A day off in between is not
skipping. It is the other half of training.

## Do this

Do two strength sessions this week, any size. Even one set each of push-ups and
squats counts. Training shows your strength days for the week.

*Bull et al., 2020, British Journal of Sports Medicine.*
"""),
        ("progress_slowly", "One more than last time",
         "How strength actually grows: a rep at a time.", 3, """
The way strength grows is almost boring. You do a little more than last time, often.

Coaches call it *progressive overload*: your body adapts to what you ask of it, so to
keep improving you ask for slightly more. The simplest version is double progression.
Pick a range, say eight to twelve. Each session, try to do one more rep than last
time. When you reach twelve, make it harder (a harder version, or a little more
weight) and go back to eight.

That is exactly what the suggestions in Training do. Every number is your last
session plus one step, never a guess.

## Sore is normal, pain is not

Feeling stiff a day or two after a new exercise is normal and fades as you get used
to it. Sharp pain, pain in a joint, or pain that gets worse as you go means stop. Try
an easier version, and if it keeps happening, ask a doctor or physiotherapist.

## Slow is fast

The people who get strongest are the ones who keep going, not the ones who go hardest
in the first week. A plan you can repeat beats a plan that hurts.

## Do this

Look at your best set at one exercise in Training. Next time, do one more than that.
Just one.

*American College of Sports Medicine, 2009, Medicine & Science in Sports & Exercise.*
"""),
        ("not_in_the_mood", "When you do not feel like it",
         "Motivation follows action more often than it leads it.", 3, """
Waiting until you feel like exercising is like waiting until you feel rested before
going to bed. The feeling usually comes after.

This is the idea behind *behavioural activation*, one of the best-supported
treatments for low mood: you plan small, valued activities and do them whether or
not you feel like it, and your mood catches up. It does not ask you to change how you
feel first. It asks you to change what you do, and lets the feeling follow.

## Make the first step stupidly small

Not "go for a run". Put your shoes on. Not "do a workout". Do one push-up. The first
step should be so small that saying no to it feels silly. Once you are moving, the
second step is much easier than the first.

## Plan it, do not decide it

Decide the night before: when, where, what. "After I put the kettle on, I do ten
squats." When the moment comes, there is nothing to decide, only something to do.

## Do this

Pick tomorrow's moment now: after something you already do every day. Write it down
as "After I ___, I will ___." Then make the ___ small enough that you will.

*Ekers et al., 2014, PLoS ONE.*
"""),
    ]),

    ("unstuck", "Getting unstuck", "Five reads on starting when starting feels impossible.", "map", [
        ("action_first", "Action comes before motivation",
         "You do not have to feel ready. You have to start small.", 3, """
When you are stuck, it feels as if you are waiting for something: energy, motivation,
a good day. The waiting makes sense from the inside. It almost never works.

Low mood and low motivation feed each other. You do less, so there is less that feels
good or useful in your day, so your mood drops, so you do less. The way out is not to
feel better first. It is to do a little, which gives your mood something to work with.

## Pick something that matters

Not everything you do has the same effect. Things that give you a sense of pleasure,
or a sense of having done something useful, or that fit what you care about, lift
mood the most. Washing up a few plates can count. So can sending a message to a
friend.

## Keep a tiny record

Note what you did and how you felt after, even roughly. Over a week, you will see
which things help. That is your own evidence, which is far more convincing than any
article.

## Do this

Choose one small thing that would make tomorrow slightly better, and do two minutes
of it today. Afterwards, notice how you feel compared with before.

*Martell, Addis & Jacobson, 2001, Depression in Context.*
"""),
        ("procrastination_mood", "Putting it off is about feelings",
         "Procrastination is mood repair, not laziness.", 3, """
If you keep putting something off, it is probably not because you are lazy. It is
because the task brings up a feeling you would rather not have: boredom, dread,
self-doubt, overwhelm. Putting it off makes the feeling go away. For now.

Researchers describe procrastination as short-term mood repair. It works in the
moment, which is why it is so hard to stop, and it costs more later, because the task
is still there and now it comes with guilt.

## Name the feeling

Ask: what does this task make me feel? Bored? Scared it will be bad? Not sure where
to start? Naming it takes some of its power away, and it tells you what to fix. Not
sure where to start needs a smaller first step. Scared it will be bad needs
permission to do a rough version.

## Shrink the first step

Make the first step so small that the feeling barely has time to show up. Open the
document. Write one sentence. Find the envelope.

## Be kind about it

Beating yourself up over past procrastination makes the next round worse. Students
who forgave themselves for putting off revision procrastinated less on the next exam.

## Do this

Pick the thing you have been putting off. Name the feeling in one word, then do the
first two minutes. Stop after two minutes if you want to.

*Sirois & Pychyl, 2013, Social and Personality Psychology Compass.*
"""),
        ("if_then", "If this, then that",
         "A plan with a moment in it works far better than a wish.", 3, """
"I will exercise more" is a wish. "When I get home from work, I will put my trainers
on before I sit down" is a plan. The difference is huge.

Psychologists call these *implementation intentions*: if-then plans that link a
specific moment to a specific action. Across 94 studies, people who made them were
much more likely to follow through on their goals than people who only set the goal.

## Why it works

The moment does the remembering for you. You do not have to keep the goal in your
head all day and decide again when to do it. When the moment arrives, it triggers the
action, almost automatically.

## How to write one

Pick a moment that happens every day and is easy to notice: after you brush your
teeth, when you sit down on the bus, when the kettle boils. Then pick an action small
enough to do every time.

"When the kettle boils, I will do five squats." "When I sit on the bus, I will read
one page."

This is what the times and moments in this app are for.

## Do this

Write one if-then plan for something you keep meaning to do: "When ___, I will ___."
Make the moment specific and the action small.

*Gollwitzer & Sheeran, 2006, Advances in Experimental Social Psychology.*
"""),
        ("make_it_tiny", "Shrink it until it is easy",
         "Tiny is not a compromise. It is the method.", 3, """
Most habits fail because they start too big. "Go to the gym every day" survives about
a week. "Do one push-up after I brush my teeth" can survive for years, and it
quietly grows.

A habit is built by repeating the same small act in the same moment until it happens
without thinking. In one study following people forming new habits, it took a median
of 66 days to become automatic, with a range of 18 to 254. What mattered was the
repetition, not the size. And missing a single day did not undo it.

## How small

Small enough that you could do it on your worst day. If you are not sure, halve it.
Then halve it again. One page, one push-up, one glass of water, one line in a journal.

## It grows on its own

Once the tiny version is automatic, doing more is easy, and often happens by itself.
You put on your trainers "just to walk to the end of the road" and end up walking for
twenty minutes. But the habit is the tiny version. Anything more is a bonus.

That is why "tiny" in this app counts in full.

## Do this

Take one habit you keep failing at. Halve it, and halve it again. Do that tiny
version today, at the same moment you will do it tomorrow.

*Lally et al., 2010, European Journal of Social Psychology.*
"""),
        ("after_a_slip", "After a slip",
         "What you say to yourself after a bad day decides the next one.", 3, """
You missed a few days. Or a few weeks. The next thought is often: "I have ruined it,
so what is the point." That thought does more damage than the missed days ever did.

## A slip is not a reset

In research on how habits form, a single missed day made no real difference to how
automatic the habit became. Habits are built across weeks, not days. The only way a
slip becomes a stop is if it turns into a reason to give up.

## Kindness works better than criticism

It feels as if being hard on yourself should motivate you. The evidence says the
opposite. In a series of studies, people who were encouraged to treat a mistake with
self-compassion, the kindness you would give a friend, were more motivated to improve
and put in more effort afterwards than people who were encouraged to build up their
self-esteem or given no help at all.

## Start again tiny

The first day back is not the day to make up for lost time. Do the tiny version. The
point is to reconnect the habit to its moment, not to prove anything.

## Do this

If you have slipped recently, say out loud: "That happened, and it is normal. I am
starting again today." Then do the smallest version of the habit, now.

*Breines & Chen, 2012, Personality and Social Psychology Bulletin; Lally et al., 2010.*
"""),
    ]),

    ("calm", "A quieter mind", "Five reads on worry, feelings and the voice in your head.", "candle", [
        ("worry_time", "Give worry an appointment",
         "Worry postponed is often worry that never comes back.", 3, """
Worry tends to spread through the whole day. A thought arrives, you follow it, and an
hour has gone. Telling yourself to stop worrying rarely works.

What does work is giving worry a place and a time. It is called *worry postponement*,
and it was first tested in the 1980s with people who worried a lot. They set aside
half an hour at the same time and place each day for worrying, and postponed every
worry that turned up at other times until then. Their worry went down.

## How to do it

Pick fifteen minutes at a set time, not right before bed. When a worry turns up
during the day, write it down in a word or two and tell it: "Later."

Then, at worry time, read the list. Worry about anything still worth worrying about.
For each one, ask: is there something I can do about this? If yes, write the first
step. If no, let it sit.

## What you will notice

Many worries do not need the appointment by the time it comes. They were urgent only
in the moment.

## Do this

Choose your worry time today, and write it down. For the rest of the day, note each
worry in two words and say "later". At worry time, look at the list.

*Borkovec et al., 1983, Behaviour Research and Therapy.*
"""),
        ("slow_breathing", "Slow breathing, on purpose",
         "Five minutes of a long breath out changes how you feel.", 3, """
Your breathing is one of the few parts of your body's stress system you can steer
directly. When you breathe out slowly, your heart rate drops a little, and your body
reads that as a signal that things are safe.

## The cyclic sigh

In a study comparing breathing techniques, people who did five minutes a day of
*cyclic sighing* felt better, and less anxious, than people who did mindfulness
meditation for the same time.

It goes like this: breathe in through your nose, then take a second short breath in
on top of it to fill your lungs completely. Then let it all out slowly through your
mouth, taking longer to breathe out than you did to breathe in. Repeat.

## When to use it

Any time you notice you are tense, and on purpose at a set moment each day, so it
becomes easy to reach for. It is the Heart routine called slow breathing in this app.

## A note

If breathing exercises make you feel dizzy or more anxious, breathe more gently, or
stop. It should feel calming, not like a test.

## Do this

Right now, or at your next tense moment: three cyclic sighs. Two breaths in through
the nose, one long breath out through the mouth.

*Balban et al., 2023, Cell Reports Medicine.*
"""),
        ("name_it", "Name it to tame it",
         "Putting a feeling into words turns its volume down.", 3, """
When a strong feeling arrives, the instinct is to push it away or to follow it. There
is a third option that sounds too simple to work: name it.

In a brain imaging study, simply putting a label on an emotional image, choosing the
word "angry" or "scared", reduced activity in the part of the brain that responds to
threat, more than other ways of looking at the same image. Researchers call this
*affect labelling*. Naming the feeling seems to bring the thinking parts of the brain
back online.

## How to do it

Pause and finish the sentence: "Right now I feel ___." One word is enough. If you can
be more precise, do: "disappointed" is more useful than "bad". If there is more than
one feeling, name each.

You do not have to fix it, explain it or argue with it. Naming is the whole step.

## Why it helps later

Over time, noticing your feelings early makes them easier to handle before they get
big. It also gives you a record: what you feel, when, and after what.

This is the Heart routine called Name it.

## Do this

Three times today, at random moments, stop and finish the sentence: "Right now I feel
___." Write the word down if you can.

*Lieberman et al., 2007, Psychological Science.*
"""),
        ("thoughts_not_facts", "Thoughts are not facts",
         "You can notice a thought without having to believe it.", 3, """
Your mind produces thoughts all day, and some of them are cruel: "Everyone thinks I
am strange." "I always mess things up." They arrive sounding like facts. They are
not. They are thoughts.

Therapists call the skill of seeing that difference *defusion*: stepping back far
enough to see a thought as words your mind is producing, rather than as the truth
about the world.

## Two ways in

Add a frame. Instead of "I am useless", say "I am having the thought that I am
useless." The thought is the same. Your relationship to it changes.

Wear it out. In one study, repeating a negative word about yourself out loud,
quickly, for half a minute made it feel less believable and less upsetting. Said
enough times, words go back to being sounds.

## What this is not

It is not arguing with the thought or replacing it with a positive one. You do not
have to win against your mind. You just have to stop taking every word it says as
an order.

## Do this

Notice the harshest thing your mind tells you today. Say it again, starting with "I
am having the thought that...". Notice whether it lands any differently.

*Masuda et al., 2004, Behaviour Research and Therapy.*
"""),
        ("kind_voice", "Talk to yourself like a friend",
         "Self-compassion is not letting yourself off. It is how people keep going.", 3, """
Listen to how you talk to yourself after a mistake. Now imagine saying the same words
to a friend who made the same mistake. Most people could not.

*Self-compassion* is treating yourself with the kindness you would give a friend. It
has three parts: being kind rather than harsh, remembering that struggling is part of
being human rather than proof something is wrong with you, and noticing painful
feelings without drowning in them.

## It is not going easy on yourself

People worry that being kind to themselves will make them lazy. The research points
the other way. People who are more self-compassionate tend to have less anxiety and
depression, and they are more willing to try again after a failure, because failing
does not cost them as much.

## How to start

When you notice the harsh voice, ask: "What would I say to a friend right now?" Then
say that to yourself, even if it feels awkward. Putting a hand on your chest while you
do it can help, which is why the Heart routine called Kind voice includes it.

## Do this

Think of something you are being hard on yourself about. Write two sentences you
would say to a friend in the same position. Then read them to yourself.

*Neff, 2003, Self and Identity; MacBeth & Gumley, 2012, Clinical Psychology Review.*
"""),
    ]),
]


def build():
    packs = []
    seen = set()
    for pid, title, blurb, icon, pages in PACKS:
        lessons = []
        for lid, ltitle, summary, minutes, body in pages:
            assert lid not in seen, f"duplicate page id {lid}"
            seen.add(lid)
            text = body.strip()
            lessons.append({
                "id": lid, "day": -1, "title": ltitle, "summary": summary, "min": minutes,
                "tags": [pid], "debuffs": [], "body": text,
            })
        packs.append({"id": pid, "title": title, "blurb": blurb, "icon": icon, "lessons": lessons})
    return packs


def check(packs):
    problems = []
    learn = json.load(open(os.path.join(OUT, "curriculum.json")))
    free_ids = {l["id"] for l in learn["lessons"]}
    words = 0
    for p in packs:
        if len(p["lessons"]) != 5:
            problems.append(f"{p['id']} has {len(p['lessons'])} pages, expected 5")
        for l in p["lessons"]:
            body = l["body"]
            n = len(re.findall(r"[A-Za-z']+", body))
            words += n
            if l["id"] in free_ids:
                problems.append(f"{l['id']} clashes with a free library page")
            if "## Do this" not in body:
                problems.append(f"{l['id']} has no Do this")
            last = body.strip().splitlines()[-1]
            if not (last.startswith("*") and last.endswith("*") and re.search(r"\d{4}", last)):
                problems.append(f"{l['id']} does not end with its source")
            if not 150 <= n <= 360:
                problems.append(f"{l['id']} is {n} words")
            if l["min"] > 5:
                problems.append(f"{l['id']} is over five minutes")
            for bad in ("weakness", "flaw", "lazy person", "failure as a person"):
                if bad in body.lower():
                    problems.append(f"{l['id']} says '{bad}'")
    return problems, words


if __name__ == "__main__":
    packs = build()
    problems, words = check(packs)
    if problems:
        print("\n".join(problems))
        raise SystemExit(1)
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, "reading.json"), "w") as f:
        json.dump({"packs": packs}, f, indent=1, ensure_ascii=False)
    print(f"reading.json  {len(packs)} packs, {sum(len(p['lessons']) for p in packs)} pages, {words} words")
