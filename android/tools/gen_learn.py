#!/usr/bin/env python3
"""
Generates the Learn curriculum.

  assets/learn/curriculum.json

Two kinds of lesson in one file:

  - **Course lessons** carry a day number, 1..30, and unlock on that day of the user's
    run. They are the spine.
  - **Targeted articles** carry day 0 and a list of debuff ids. They appear only for
    someone who ticked one of those in the Systems Audit, and they appear immediately,
    because a person who has just admitted their clothes do not fit should not wait
    three weeks to read about fit.

House style, enforced by the checks at the bottom:

  - Short sentences. No jargon that is not immediately defined.
  - Every lesson ends with a **Do this** section containing something concrete, because
    a lesson with no action is entertainment.
  - Nothing tells the reader what they are. It tells them what to do and what happens.

Run: python3 android/tools/gen_learn.py
"""

import json
import os
import re

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "learn")

# (id, day, title, summary, minutes, tags, debuffs, body)
LESSONS = [
    ("bubble_mechanics", 1, "Why your world got smaller",
     "Nothing went wrong. Comfort compounds, and so does avoidance.",
     4, ["foundations"], [], """
Nobody decides to shrink their life. It happens through a series of individually
sensible choices.

You skip one thing because you are tired. The next time, skipping is slightly
easier, because you have done it before and nothing bad happened. Six months
later there is a whole category of thing you no longer do, and you never made a
decision about it.

This is not a character flaw. It is how habits work in both directions.
Avoidance compounds exactly the way practice does.

## The part that matters

Every avoided thing gets harder in proportion to how long it has been avoided.
Not because the thing changed — because your estimate of it grew. You have been
rehearsing the difficulty without ever checking it against reality.

The only thing that corrects the estimate is doing the thing. Not thinking about
it, not preparing for it. Doing a small version of it and seeing what actually
happens.

That is the entire method of this app. Small versions, done, repeatedly, until
your estimates are accurate again.

## Do this

Write down one thing you used to do and no longer do. Not a big one. Something
like "I used to go to that cafe" or "I used to answer the phone."

You are not going to do it today. You are just naming it, so it stops being
invisible.
"""),

    ("three_directions", 2, "Access, Activity, Social",
     "Three separate edges. Progress in one does not carry to the others.",
     3, ["foundations"], [], """
Your bubble has three edges, and they move independently.

**Access** is geography. Where you physically go. Someone can be socially
confident and still have a two-kilometre radius.

**Activity** is experience. What you do. Someone can travel constantly and still
do the same five things everywhere.

**Social** is people. Who you talk to, and how directly. This one is usually the
tightest, and it is the one most people mean when they say they want to change.

## Why they are separate

Because progress does not transfer. Getting comfortable on unfamiliar streets
does very little for your ability to start a conversation. They feel like the
same courage from the inside. They are not the same skill.

If the app only tracked one number you would push on the easiest edge and feel
like you were making progress everywhere. Three separate ladders make that
impossible to fake.

## Do this

Look at your three levels on the home screen. One of them is lower than the
others.

That is not a problem to fix today. It is just the direction with the most
available return.
"""),

    ("friction_score", 3, "Why rejection scores highest",
     "The score you want is the one that only goes up when things go wrong.",
     4, ["foundations", "rejection"], [], """
Most apps count wins. This one counts the awkward stuff highest, and there is a
specific reason.

If you only score outcomes, you learn to attempt only the things you will win.
That is a rational response to the scoring system, and it produces a person who
never tries anything uncertain.

Friction inverts it. You get points for asking and being refused. For starting a
conversation that dies. For turning back halfway.

## What this is actually training

The thing that stops most people is not failure. It is the *anticipation* of
failure — the flinch before the attempt. That flinch weakens every time you
attempt something and survive the outcome, regardless of what the outcome was.

So the app scores the attempt. It has to, because the attempt is the part you
control and the part that needs practice.

There is a real consequence to this: someone who plays it safe for a hundred days
will end up with a high level and almost no Friction. The app will show that
plainly. That is not an insult. It is the most useful piece of information it
can give you.

## Do this

Look at your Friction number. If it is zero, your next challenge should be one
you are not sure you will complete.
""" ),

    ("small_beginnings", 4, "Why the first challenges are so small",
     "The size is the point. You are building a mechanism, not a result.",
     3, ["foundations"], [], """
Standing outside your front door for sixty seconds is not a meaningful act.

It is also exactly the right first challenge, and here is why.

You are not trying to change your life this week. You are trying to establish
that when this app says do something, you do it. That link — instruction to
action, without a negotiation in between — is the actual mechanism. Everything
else is built on it.

If the first challenge is hard, you will negotiate. You will decide to do it
tomorrow, or do a modified version, and the link never forms.

## The trap

The trap is deciding the small ones are beneath you and skipping ahead. People
who do this reliably stall around day twelve, because they built the ambition
without building the mechanism.

The ladder goes to a hundred. It gets genuinely difficult. Let it.

## Do this

Do today's challenge even though it is too easy. Especially because it is too
easy.
"""),

    ("spotlight_effect", 5, "Nobody is looking at you",
     "The audience you are performing for is not there.",
     4, ["mindset"], ["assumes_judgement", "catastrophizes"], """
There is a well-documented gap between how much people think they are noticed
and how much they actually are. It is large, and it is consistent.

The reason is simple: everyone is the main character of their own attention.
The person you are worried is judging you is, at that moment, worried about
something of their own.

## The useful version

This is not a reassurance. It is a testable claim, and you should test it.

Next time you are somewhere public and feeling watched, count the number of
people actually looking at you. Not glancing past — looking.

The number is almost always zero. Occasionally it is one, and they are looking
because you are in the way.

Do this three or four times and the feeling stops being persuasive. Not because
you talked yourself out of it, but because you have data.

## The exception

You get noticed more when you are visibly uncomfortable. Hunched, rushing,
scanning. Discomfort is legible in a way that ordinary presence is not.

Which means the fastest way to be less noticed is to stop trying to be
invisible.

## Do this

Somewhere public today, count how many people are actually looking at you.
Write the number down.
"""),

    ("first_conversations", 6, "The opening line does not matter",
     "What you say first is almost irrelevant. What you do next is not.",
     4, ["social"], ["no_questions", "stiff_greeting"], """
People spend enormous energy on the opening line. It is the least important part
of the interaction.

Nobody remembers what you said first. They remember whether the next ninety
seconds were comfortable.

## What actually works

The opening only needs to be **relevant to the situation** and **easy to
answer**. That is the whole specification.

- In a queue: something about the queue.
- At an event: something about the event.
- Anywhere: a question about the thing you are both looking at.

"Is this the line for the front?" is a fine opening. So is "have you been to one
of these before?"

## The part that matters

What determines whether it goes anywhere is what you do with their answer.

Most stalled conversations die because the answer arrives and nothing is done
with it. They say something and you say "oh, nice", and then it is over.

Take the last thing they said and ask about it. That is the whole technique.
It works forever and it never runs out.

## Do this

Start one conversation today with something relevant to where you are. Then ask
one question about whatever they answer.
"""),

    ("questions_that_open", 7, "Ask how, not what",
     "One-word change, completely different conversation.",
     3, ["social"], ["interview_mode", "no_questions"], """
There are two kinds of question, and one of them ends conversations.

**Closed questions** ask for a fact. Where are you from. What do you do. How
long have you lived here. They get a fact back, and then the conversation needs
restarting.

**Open questions** ask for a reason or a story. How did you end up doing that.
Why that one. What is that actually like.

## The mechanism

A fact has no follow-up. A reason always does, because reasons contain other
things — decisions, people, changes of mind.

Asking three closed questions in a row is what makes you feel like you are
interviewing someone. That feeling is accurate. You are.

## The upgrade

Take any closed question you were about to ask and put "how" or "why" in front
of the same subject.

- "What do you do?" becomes "How did you get into that?"
- "Where are you from?" becomes "What made you move here?"
- "Do you like it?" becomes "What is the best part of it?"

Same subject. Entirely different amount of material comes back.

## Do this

Ask one "how" or "why" question today instead of the "what" you were going to
ask.
"""),

    ("vulnerability_vs_neediness", 8, "Vulnerability is not neediness",
     "They look similar and they do opposite things. Here is the difference.",
     6, ["attraction", "social"], ["neediness", "overshares_early"], """
This is the distinction that most people get wrong, and getting it wrong costs
them a lot.

**Vulnerability** is saying a true thing about yourself and not requiring a
particular reaction to it.

**Neediness** is saying something in order to get a reaction, and adjusting
yourself if it does not arrive.

The words can be identical. The difference is entirely in whether you needed
something back.

## How to tell which one you are doing

Ask yourself: if they respond neutrally, is that fine?

If yes, you were being vulnerable. If the thought of a flat response makes you
want to take it back, add to it, or explain it better — that was a bid, and
people can feel the difference immediately.

## Why oversharing reads as needy

Telling someone difficult things very early looks like vulnerability and usually
is not. It is a shortcut, an attempt to manufacture closeness faster than it
normally arrives.

The other person feels the request in it — the requirement to respond with care
they have not agreed to give. It is a bill arriving before the meal.

Real vulnerability is usually much smaller and much better timed. One honest
sentence, at a normal point in a conversation, that you would have said anyway.

## The rule

Say the true thing. Then stop talking and let whatever happens happen.

If you can do that, you are being vulnerable. If you cannot, you are asking for
something, and it is better to know that.

## Do this

Say one true thing today that you would normally keep back. Then do not check
how it landed.
"""),

    ("attraction_triggers", 9, "What people actually respond to",
     "Not looks. Not money. A short list of things, most of which are learnable.",
     6, ["attraction"], ["no_personal_style", "compares_constantly"], """
People overweight the things they cannot change and underweight the things they
can. Here is the actual list, roughly in order of how much it moves.

## 1. Non-neediness

The single strongest signal. Someone who is not adjusting themselves to be liked
reads as having options, and having options is attractive in a way that no
amount of technique replaces.

This is why desperation is so visible. It is not that people dislike wanting.
It is that they dislike being *needed*, because being needed is a job.

## 2. Congruence

Whether the things about you agree with each other. Someone whose clothes,
posture, voice and stated interests all point the same direction reads as real.
Someone assembled out of borrowed pieces reads as unfinished.

Congruence is why copying someone else's style rarely works and why finding your
own does.

## 3. Presence

Whether you are actually in the room. Eye contact, stillness, listening to the
answer rather than preparing your next line. Almost nobody does this, so it is
disproportionately noticeable.

## 4. Investedness in something

Having something you care about that is not the person in front of you. It gives
you material, it gives you somewhere to be, and it signals that your life
continues regardless of how this goes.

## 5. Grooming and fit

Not looks. Grooming and fit. These are the parts of appearance that are entirely
within your control, and they carry most of the signal that people attribute to
attractiveness.

## What is missing from this list

Height. Bone structure. Income. These are not zero, but they are far below where
most people rank them, and they are the only ones you cannot work on.

Spending your attention on the fixed items is how people stay stuck. Everything
above is trainable.

## Do this

Pick the lowest one on that list for you and note it. That is where your return
is highest.
"""),

    ("hobbies_signal", 10, "What your interests say about you",
     "Some hobbies make you more interesting. Most do not. The difference is specific.",
     5, ["attraction", "identity"], ["no_self_narrative"], """
"Get a hobby" is useless advice on its own, because most hobbies do nothing for
you socially.

What actually matters is what the hobby demonstrates.

## The three things a good interest signals

**Competence acquired over time.** Anything with a visible skill curve — an
instrument, a sport, a craft — says you can stick with something difficult. That
is a claim about your character, not your leisure time.

**A world you can take someone into.** Climbing, sailing, restoring things,
cooking properly. These come with places, people and vocabulary. You can bring
someone in. Watching television cannot do this.

**Physical or social risk.** Anything performed, taught, competed or shown
carries a small ongoing cost of being judged. People who pay that cost read
differently.

## What does not signal

Consumption. Watching, following, collecting, having opinions about. These are
fine and you are allowed to enjoy them. They just do not tell anyone anything
about you, because anyone can do them.

The test: does it produce anything, take you anywhere, or require practice? If
none of the three, it is a pastime rather than an interest.

## The honest version

You do not need an impressive hobby. You need a real one — something you would
do if nobody asked about it.

Faked interests are transparent within two questions, because you cannot answer
the "why" behind them.

## Do this

Write down three things you are actually interested in. Not impressive — actual.
For each, write one sentence about why.
"""),

    ("style_basics", 11, "Fit is the whole thing",
     "Almost everything people call style is fit and upkeep. Both are free or nearly.",
     6, ["style"], ["ill_fitting_clothes", "no_personal_style", "worn_out_clothes"], """
People assume dressing well requires money and taste. It mostly requires fit and
maintenance, and the first is not expensive.

## Fit, in three rules

**Shoulders.** The shoulder seam should sit on the edge of your shoulder. Not
down your arm. This one thing separates a garment that looks bought from one
that looks borrowed, and it cannot be altered cheaply — so check it before you
buy.

**Length.** Sleeves end at the wrist bone. Trousers break once, lightly, on the
shoe. Hems are the cheapest alteration there is.

**Width.** You should be able to pinch a small amount of fabric at the chest and
thigh. A lot of fabric means it is too big. None means it is too small.

That is it. Three checks, and they cover most of what makes clothes look right.

## Silhouette

The shape you make from across a room. You want a defined shoulder line and no
extra volume anywhere it is not needed.

This is why oversized clothing reads as "hiding" — it removes the silhouette
entirely, which is often the actual reason someone chose it.

## Upkeep

Faded, bobbled and stretched are visible from further away than you think, and
they are what read as neglect rather than as inexpensive.

Cheap and maintained beats expensive and worn out, every time.

## Where to start

Not shopping. Auditing. Try on everything you own and separate what fits from
what does not. Most people find they own three or four things that actually fit
and have been rotating through twenty.

## Do this

Try on five things you wear regularly and check them against the three fit
rules. Be honest about the shoulders.
"""),

    ("grooming_floor", 12, "The floor, and how to hold it",
     "A short list, done consistently, puts you above most people.",
     4, ["style"], ["grooming_neglect", "hygiene_slips", "looks_tired"], """
This is unglamorous and it has a higher return than almost anything else in this
app.

## The floor

- Hair cut on a schedule, not when it becomes a problem.
- Nails short and clean. People look at hands more than you think.
- Facial hair either deliberate or absent. The in-between state reads as
  "did not get round to it", because it is.
- Teeth, twice, properly.
- Clean clothes, including the days nobody sees you.

None of this is difficult. All of it is consistency, which is why it is a
schedule rather than a decision.

## Why the schedule matters

Every one of these becomes a project once it has been neglected long enough.
A haircut you have put off for four months carries dread that a haircut every
five weeks does not.

Put the recurring ones in a calendar and they stop requiring willpower.

## Sleep and daylight

Both show on your face within about three days, and both are usually the first
thing to go when someone is isolated.

Fifteen minutes of morning daylight and a consistent wake time do more for how
you look than any product. This is not wellness advice; it is the most efficient
available intervention.

## The honest bit

If several of these have slipped, that is usually a signal about how you have
been doing, not about laziness. Start with one. Do it today.

## Do this

Pick the one on that list that has slipped furthest. Deal with it today, not
this week.
"""),

    ("rejection_psychology", 13, "What a 'no' actually is",
     "Almost never about you. Usually about timing, and almost always forgotten.",
     6, ["rejection", "mindset"], ["fear_of_rejection", "pre_rejects_self", "outcome_dependent"], """
Rejection feels like a verdict on your worth. It is almost never that, and
understanding what it actually is changes how much it costs.

## What a refusal is made of

Someone declining you is a function of: their current situation, their mood, the
timing, what they are already dealing with, whether they are looking for anything
at all, and — somewhere down the list — you.

You are one input among six, and you are not usually the deciding one.

This is not a comfort. It is arithmetic. You have refused people yourself, and
you know how little of it was a judgement about them.

## Why it hurts anyway

Because social rejection uses some of the same neural machinery as physical
pain, and because for most of human history exclusion from the group was
genuinely dangerous.

The response is not irrational. It is just calibrated for a world where being
turned down by a stranger did not exist as a category.

## The thing that reduces it

Volume. Nothing else works.

People who are comfortable with rejection are not braver. They have simply been
refused enough times that the event has lost its novelty, and the nervous system
downgrades things that keep happening without consequence.

There is no way to get that except by collecting refusals.

## The reframe that actually helps

Stop treating a no as information about your value and start treating it as
information about fit. It tells you this particular person, at this particular
time, is not it. That is genuinely useful and it is all it means.

## Do this

Go and get one refusal today. Ask for something small you expect to be refused.
The point is to survive one on purpose.
"""),

    ("silence", 14, "Learn to not fill it",
     "The pause is not a failure. Filling it is.",
     4, ["social"], ["cant_hold_silence", "nervous_laugh"], """
A three-second pause in conversation feels like a minute from the inside. It is
three seconds.

## What filling it does

When you rush to fill a silence you communicate two things without meaning to:
that you are uncomfortable, and that you consider the silence your
responsibility to fix.

Both read as anxiety, and anxiety is contagious. The other person starts feeling
it too, and now the conversation is actually going badly.

## What holding it does

Silence after someone speaks reads as consideration. Silence after you ask a
question gives them room to give a real answer rather than a reflexive one.

The best interviewers on earth are all people who have learned to wait.

## The specific fix

After you ask a question, say nothing until they answer. Not a clarification,
not a "or whatever", not a laugh. Nothing.

You will want to rescue them. Do not. The thing they say after the pause is
almost always better than what they would have said immediately.

## Do this

Let one silence run for three full seconds today without filling it. Count it in
your head.
"""),

    ("opinions", 15, "Have some",
     "Agreeable is not likeable. It is forgettable.",
     4, ["social", "attraction"], ["agrees_with_everything", "no_opinions", "people_pleaser"], """
Being easy to get along with feels like a strategy for being liked. It is
actually a strategy for being unmemorable.

## Why

People bond over specifics — shared taste, shared dislikes, an argument about
something that does not matter. You cannot bond with someone who has no
position, because there is nothing to bond to.

"I do not mind" is a friendly sentence that gives the other person nothing to
work with.

## The polarisation principle

Anything that makes some people like you more will make some people like you
less. That is not a risk of having a personality; it is the definition of it.

Aiming for universal mild approval guarantees that nobody feels strongly. If
nobody feels strongly, nobody pursues.

## The low-stakes version

You do not have to start with politics. Start with:

- Where you actually want to eat.
- A film everyone likes that you did not.
- A film nobody respects that you love.

The last one is the most useful. A slightly embarrassing genuine preference does
more for you than a defensible opinion, because it cannot be faked.

## Do this

Pick the restaurant, the film or the plan today. Do not ask what everyone else
would prefer first.
"""),

    ("body_language", 16, "What your body says first",
     "You are read before you speak. Three things carry most of it.",
     5, ["presence"], ["posture_slump", "closed_body", "takes_up_no_space"], """
People form an impression before you have said anything. Most of it comes from
three things.

## 1. Vertical

Chin level, shoulders back and down, chest open. Not military — just not
collapsed.

Slumping is read as low status or low mood, and it is read fast. The correction
feels theatrical for about a week and then becomes invisible to you and normal
to everyone else.

## 2. Open

Arms uncrossed, hands visible, body square to whoever you are talking to.

Hidden hands and folded arms are read as guarded, and angling your body away is
read as wanting to leave. You may just be cold. It does not matter — you are
read on the signal, not the intent.

## 3. Still

Fidgeting is the loudest tell you have. Phone, keys, sleeves, face. Every one of
them says "I need somewhere to put this discomfort."

Stillness is rare enough that it reads as composure almost by itself.

## The order to fix them

Stillness first, because it is the most noticeable. Then open, because it is one
decision. Then vertical, because it is a genuine habit and takes longest.

## Do this

Pick one of the three for today. Just one. Notice how often you have to correct
it — that number is the actual size of the habit.
"""),

    ("voice", 17, "Volume, pace and the end of sentences",
     "Three fixable things that change how seriously you are taken.",
     5, ["presence"], ["speaks_too_quietly", "mumbles", "monotone"], """
Voice is not a fixed asset. Almost everything that makes a voice hard to listen
to is a habit.

## Volume

If people ask you to repeat yourself, your calibration is off, not your voice.
What feels 30 per cent too loud from inside your own head is usually normal from
a metre away.

Being heard the first time matters more than it sounds. Repeating yourself
teaches a room that you can be talked over.

## The end of the sentence

Most people who are described as mumbling are perfectly clear for the first two
thirds and then drop off.

Trailing off is a retraction — it says "you do not need to have heard that."
Land the final word at full volume and the same sentence carries completely
differently.

## Pace and pitch

Monotone is usually not a limitation. It is a habit of not letting your voice do
anything, often learned after some moment where it did.

Read something out loud three times — bored, excited, conspiratorial. You will
find the range is there.

## Do this

Record yourself saying three sentences and listen back. You almost certainly do
not know what you sound like, and two minutes of evidence beats a year of
guessing.
"""),

    ("initiating", 18, "Someone has to go first",
     "It has never been you. That is the whole problem.",
     4, ["social"], ["waits_to_be_chosen", "feels_like_imposition"], """
Being available is not the same as being present.

A lot of people are perfectly friendly, perfectly willing, and never once the
person who starts something. From the outside this is indistinguishable from not
being interested.

## The asymmetry

The person who initiates carries a small risk — of being declined, of having
misjudged it. The person who waits carries none.

But the person who waits also gets whatever they are given, which over years is
usually not much.

## What initiating actually is

It is not charisma. It is a message, sent, with a specific thing in it.

"We should catch up" is not initiating. It is a way of feeling like you tried
while leaving the actual work to the other person.

"Are you free Thursday? There is a thing at seven" is initiating. A day, a time,
a plan.

## The thing you are afraid of

That they will say no and you will have been the one who wanted it more.

They might. That is the price, and it is much lower than the price of the
alternative, which is a quiet decade.

## Do this

Send one message today with a day, a time and a plan in it.
"""),

    ("hosting", 19, "The fastest route to a circle",
     "Hosting turns acquaintances into friends faster than anything else, and the bar is low.",
     5, ["community"], [], """
If you want a social circle rather than a set of individual friendships, host
something.

## Why it works

Friendship needs repeated, unplanned, low-stakes contact. Adult life removes
almost all of that. School and university supplied it for free; nothing after
them does.

Hosting manufactures it deliberately. It also makes you the centre of the
network by default, because you are the connection everyone has in common.

## The bar is much lower than you think

You do not need a nice flat, a signature dish or a plan. Two people, an evening,
and something to drink is a gathering.

The elaborate version is usually procrastination wearing preparation's clothes.
People are not coming to inspect your home.

## The recurring version

The strongest move available is a standing thing. Same day, same place, every
fortnight. Tell people it is recurring.

Once it is recurring, nobody has to be invited, nobody has to organise it, and
attendance stops being a decision. It becomes the thing that happens on
Thursdays.

## The rule

Invite more people than you want. Roughly half will not come, and that is
normal, not a verdict.

## Do this

Pick a date within the next fortnight and invite two people to your home for the
evening.
"""),

    ("saying_no", 20, "The word that makes yes mean something",
     "If you cannot decline, your agreement carries no information.",
     4, ["boundaries"], ["people_pleaser", "over_apologizes"], """
Someone who cannot say no is not generous. They are unreadable.

## Why

If you agree to everything, your agreement stops being informative. Nobody knows
whether you actually want to be there, which means nobody can trust that you do.

Being able to decline is what makes your acceptance mean something.

## The reason it is hard

Saying no feels like it will cost you the relationship. In practice, the
resentment from repeatedly agreeing to things you did not want costs far more,
and it arrives with interest.

## How to do it

A complete no is short and does not negotiate.

- "I cannot do that."
- "That does not work for me."
- "No, but thanks for asking."

Notice what is missing: the excuse. Excuses invite problem-solving. Give someone
a reason and they will helpfully remove it, and now you are trapped in a
negotiation you did not want.

## The thing that surprises people

People respect it. Reliably. A clear no from someone who normally agrees is
noticed, and it usually raises their standing rather than lowering it.

## Do this

Say no to one small thing today. Without an excuse attached.
"""),

    ("reframing", 21, "Catching the thought before it lands",
     "You cannot argue yourself out of a feeling. You can check the claim underneath it.",
     6, ["mindset"], ["catastrophizes", "mind_reading", "assumes_judgement"], """
Anxious thinking is not irrational; it is over-confident. It states things as
facts that were never checked.

## The three that do most of the damage

**Mind reading.** Deciding what someone thought without asking. "They went
quiet, so they think I am boring."

**Catastrophising.** Taking a small event to its worst possible conclusion.
"That was awkward, so they will tell everyone, so I cannot go back."

**Personalising.** Assuming you are the cause of something that had nothing to
do with you.

## The technique, in three steps

1. **Write the thought down as a sentence.** Not the feeling. The claim.
   "She thinks I am boring."

2. **List the evidence for it.** Actual evidence, of the kind you could tell
   someone else. Usually the column is empty or contains one ambiguous thing.

3. **Write one alternative that fits the same evidence.** "She was tired."
   "She was thinking about something else."

You are not trying to believe the alternative. You are trying to notice that the
original was one option presented as the only one.

## Why writing matters

Doing this in your head does not work. The thought stays fast and vague, and
vague is what gives it its power. On paper it has to be specific, and specific
claims are easy to check.

## Do this

Take the interaction you have been replaying. Write the thought as one sentence.
Then list the actual evidence.
"""),

    ("rumination", 22, "How to stop replaying it",
     "Not by trying not to think about it. By giving it a box.",
     4, ["mindset"], ["catastrophizes", "outcome_dependent"], """
Telling yourself to stop thinking about something does not work. It has never
worked for anyone.

## What does work

Give it a container.

Set a timer for ten minutes and think about it deliberately. Fully. Write it
down if that helps. When the timer ends, stop and go and do something physical.

This works because rumination expands to fill whatever time is available. Bounded
time bounds it.

## The other half

Rumination is maintained by avoidance. You replay the awkward moment *because*
you have not gone back to the place it happened. The memory stays enormous
precisely because it stays untested.

Going back — to the shop, the group, the person — shrinks it faster than any
amount of thinking, because reality is nearly always smaller than the rerun.

## The timeline nobody tells you

Everyone else forgot within about a day. You are the only person still in the
room.

This is not dismissal. It is the actual distribution of attention, and it is
worth internalising, because you have forgotten everyone else's awkward moments
too.

## Do this

Set a ten-minute timer, think about it properly, and stop when it goes.
"""),

    ("comparison", 23, "The only comparison worth making",
     "Against strangers, it is rigged. Against your own past, it is data.",
     4, ["mindset"], ["compares_constantly"], """
Comparing yourself to other people is not a character flaw; it is a default
setting. But the comparison you are making is not a fair one.

## Why it is rigged

You are comparing your inside to their outside. You have full access to your own
doubt, effort and history, and none to theirs.

You are also comparing on one narrow dimension at a time, and switching
dimensions to whichever one you are losing on.

## The feed problem

Social media makes this considerably worse, and not accidentally. Comparison is
the mechanism that keeps attention. You are not weak-willed for finding it
difficult — you are the target of a well-designed system.

## The comparison that works

Against yourself, a year ago.

That one is fair, because you have full information on both sides. It is also
the only one that produces a next action rather than just a mood.

## Do this

Write down three things you can do now that you could not do a year ago.

If you cannot fill three, that is not a verdict — it is the reason you are using
this app, and in a hundred days the list will not be empty.
"""),

    ("first_impressions", 24, "The first ten seconds",
     "Mostly settled before you speak. All of it is controllable.",
     4, ["presence", "style"], ["stiff_greeting", "grooming_neglect"], """
People form a working impression of you very quickly. Not accurately —
quickly. And they then look for evidence that confirms it.

## What lands in the first ten seconds

- Whether you look after yourself. Grooming and fit, not features.
- How you carry yourself. Upright and open, or collapsed and closed.
- Whether you made eye contact and said something clearly.

That is nearly all of it. Nothing on that list is about your face.

## The greeting is scriptable

Everyone who seems smooth in the first ten seconds is running a script. Theirs is
just worn in enough that it looks improvised.

The script: eye contact, a clear hello, their name if you know it, one sentence.
That is the whole thing. Practise it out loud five times and it stops being an
event.

## Why it matters more than it should

Because of confirmation. Someone who reads you as awkward in the first ten
seconds will interpret ambiguous things afterwards as more evidence of it. The
same behaviour after a good opening gets read as dry humour.

This is unfair. It is also cheap to work with, because the first ten seconds are
the most controllable part of any interaction.

## Do this

Say your greeting out loud five times until it sounds normal. Then use it on the
next person you see.
"""),

    ("stories", 25, "How to tell one",
     "Structure, not talent. Five sentences and a clean stop.",
     5, ["social"], ["cant_tell_stories", "no_self_narrative"], """
People who tell good stories are not more interesting than you. They have a
structure, and you do not.

## The structure

1. **Setup.** Where and when, in one line.
2. **Problem.** What went wrong or was strange.
3. **Turn.** The thing that changed.
4. **End.** What happened.
5. **Why it stuck.** One line on why you still think about it.

Five sentences. That is a story.

## The two things that kill stories

**No ending.** You get to the end and realise there was not one, so you trail
off. Fix this by knowing the last line before you start.

**Apologising afterwards.** "Anyway, you had to be there." This retroactively
tells everyone the last two minutes were not worth it. Never do it. Stop at the
end and let it sit.

## Where to get material

You already have it. Most people believe nothing happens to them because they
have never written any of it down.

Write down one thing that happened this year, in the five-part structure. Once
it is written, you own it permanently.

## Do this

Write one story in five sentences. Then tell it to someone without apologising
at the end.
"""),

    ("exits", 26, "How to leave a conversation",
     "The bottleneck on meeting anyone is not starting. It is stopping.",
     4, ["social"], ["exits_badly"], """
Most people who avoid events are not afraid of starting conversations. They are
afraid of being stuck in one.

That fear is rational, and it is entirely fixable.

## Why it matters

If you cannot leave a conversation, every conversation you start is a potential
trap. So you start fewer. The exit is the actual bottleneck.

Fix the exit and the whole event becomes lower stakes.

## The line

You need one, learned in advance:

> "I am going to go and say hello to a few people — it was good to meet you."

That is complete. It is honest, it is warm, and it does not require an excuse.

## The timing

Leave while it is still going well.

Staying until a conversation dies is what makes people avoid the next one. A
conversation that ends on a high leaves both people willing to have another.

## The thing you are worried about

That leaving is rude. It is not — it is what everyone at an event is doing, and
staying too long out of politeness is a much more common failure than leaving
early.

## Do this

Learn the line. Say it out loud until it sounds like you. Then use it today,
while a conversation is still going well.
"""),

    ("range", 27, "Widen who you know",
     "Most people's acquaintance is one demographic deep. That is a ceiling.",
     4, ["community", "identity"], [], """
Look at the last ten people you had a real conversation with. There is a good
chance they are all roughly your age, background and line of work.

That is normal, and it is a limit.

## What range gives you

**Material.** Most of what makes someone interesting to talk to is having been
somewhere the other person has not.

**Perspective.** A single demographic produces a single account of how things
work, and it is easy to mistake it for how things are.

**Opportunity.** Almost everything that arrives unexpectedly — work, invitations,
people — comes from the edges of your network rather than its middle.

## How to widen it

Not by trying to meet different people in the abstract. By going to different
rooms.

Mixed-age activities. Classes rather than bars. Volunteering. Anything organised
around an interest rather than around a life stage.

## The awkward part

You will be the outsider for the first few visits. That is what widening means,
and it is why most people do not do it.

It gets normal around visit three.

## Do this

Talk to one person this week whose life looks nothing like yours, and ask about
it.
"""),

    ("maintenance", 28, "Keeping what you built",
     "Circles decay quietly. A small amount of upkeep prevents almost all of it.",
     4, ["community"], [], """
The thing nobody mentions about a social circle is that it requires maintenance,
and the decay is silent.

Nobody announces that they have drifted. It just becomes three months since you
spoke, and then it is awkward to message, and then it has been a year.

## The upkeep

**Follow up within a day.** After a good conversation, send one message with a
specific thing you enjoyed. This single habit converts more acquaintances into
friends than anything else.

**Remember one detail and use it later.** "How did that thing with your sister
go?" three weeks on says: you were actually there.

**Message without a reason.** Most people only make contact when they need
something. Being the exception is cheap and it is remembered.

## The gap problem

If it has been a long time, do not apologise for the gap. The apology makes it
awkward and asks them to reassure you.

Just start talking. Nobody minds nearly as much as you think.

## Do this

Message one person you have not spoken to in over a month. No apology for the
gap.
"""),

    ("plateaus", 29, "When it stops working",
     "Everyone stalls. Here is what it usually is.",
     4, ["foundations"], ["perfection_paralysis", "outcome_dependent"], """
At some point this stops feeling like progress. That is expected, and it is
usually one of four things.

## 1. The challenges got hard and you started negotiating

You do a modified, easier version and count it. It is worth noticing when this
starts, because it is the point where the ladder stops being real.

If a challenge is genuinely too big, log Friction. That is what it is for, and
it steps the level back down honestly.

## 2. You are only pushing on one edge

Look at your three levels. If one is much lower than the others, you have been
taking the easy wins. The low one is where the return is.

## 3. Your Friction has stopped moving

If your level is climbing and your Friction is flat, you have optimised for
completing rather than for attempting. That is comfortable and it is a
plateau by definition.

## 4. You need a bigger reason

Daily challenges expand your bubble in general. Without a goal pointed at
something you actually want, general expansion runs out of motivation somewhere
around week four.

## What not to do

Do not restart. Do not wait until you feel motivated. Do the smallest thing on
today's list and let the mechanism carry you.

## Do this

Look at your three levels and your Friction. Whichever number has moved least is
your answer.
"""),

    ("after", 30, "What this was for",
     "The app is a scaffold. Scaffolds come down.",
     4, ["foundations"], [], """
Thirty days in, it is worth saying plainly what this is trying to do.

It is not trying to keep you here.

## The actual goal

You are building three things:

**Accurate estimates.** Of how hard things are, how much you are noticed, what
happens when you are refused. Most of the limitation was never the world; it was
the forecast.

**A mechanism.** Instruction to action without a negotiation in the middle. That
is what all the small early challenges were for.

**Evidence.** A list of things you have done that you would not have done. That
list is the only durable answer to the voice that says you cannot.

## The measure of success

The measure is not your level. It is whether you would do any of this without
being asked.

At some point a challenge will arrive and you will have already done it that
week, for your own reasons. That is the whole thing working.

## What happens at the end

The ladder goes to a hundred, and then it stops, and there is deliberately
nothing after it.

An app whose stated purpose is to get you out of the house cannot also be built
to keep you in it. If this works, you will use it less. That is the intended
outcome, not a flaw in the retention strategy.

## Do this

Write down one thing you did in the last month that you would not have done
before. Keep it somewhere you will find it again.
"""),

    # ------------------------------------------------------- targeted articles
    # No day number. These appear as soon as the matching debuff is selected.
    ("fit_deep_dive", 0, "Silhouette, and why it reads from across a room",
     "The shape you make before anyone can see detail.",
     5, ["style"], ["ill_fitting_clothes", "no_personal_style", "one_outfit"], """
From ten metres nobody can see your face, your fabric or your brand. They can see
your outline.

## What a good outline is

A defined shoulder line, a visible waist, and no unnecessary volume. That is the
whole target, and it is why fit matters more than anything you can buy.

Oversized clothing removes the outline entirely. This is often the actual reason
someone chose it, which is worth being honest about.

## Proportion

The two halves of an outfit should not both be voluminous, and they should not
both be tight. One relaxed, one fitted, is the rule that covers almost every
combination.

## The colour shortcut

Fewer colours reads as more deliberate. Two, or three at most, with one of them
doing the talking.

If you have no idea where to start: dark trousers, a fitted top in one plain
colour, clean shoes. That combination has never once looked wrong on anybody.

## The audit before the shopping

Do not buy anything yet. Try on what you own, in front of a full-length mirror,
and sort it by whether the outline works.

Most people discover they own three or four things that actually fit and have
been rotating through twenty.

## Do this

Photograph yourself, full length, in the outfit you wear most. Look at the
outline, not the details.
"""),

    ("aesthetics_signal", 0, "What looking after yourself signals",
     "It is not about being attractive. It is about what effort communicates.",
     5, ["style", "attraction"], ["grooming_neglect", "avoids_photos", "hygiene_slips"], """
People read appearance as information, and mostly not the information you think.

## What is actually being read

Not your features. Your **upkeep**.

Clean, maintained, deliberately dressed says: this person looks after things,
including themselves. Neglected says the opposite, and people extend that
inference well past clothing.

This is why grooming outperforms genetics in practice. Features are noise;
upkeep is signal, because upkeep is a choice.

## The self-directed half

There is a second effect that matters more than the social one.

How you present affects how you behave. People who are dressed deliberately
stand differently, take up more space and initiate more. Not because clothes are
magic — because the internal claim "I am worth the effort" is being made
physically, repeatedly, and it becomes hard to argue with.

## The photograph problem

If you avoid every photograph, you have no accurate picture of how you look —
only the one your worst moments constructed.

That gap is worth closing, and closing it usually improves the estimate.

## Do this

Take one photograph of yourself today. Do not delete it immediately. Look at it
tomorrow instead.
"""),

    ("neediness_deep", 0, "Where neediness actually comes from",
     "Not from wanting people. From having nothing else.",
     6, ["attraction", "mindset"], ["neediness", "waits_to_be_chosen", "outcome_dependent"], """
Neediness is treated as a personality problem. It is usually a structural one.

## The mechanism

If one person or one interaction is the only source of connection in your week,
it carries impossible weight. Every message matters too much. Every silence is
data.

You are not being clingy because you are flawed. You are being clingy because
there is nothing else in the account.

## Why advice fails

"Be less needy" cannot be executed. It describes an outcome, not an action, and
trying to perform non-neediness produces something people read as coldness — a
different problem with the same cause.

## What actually reduces it

Volume elsewhere. More things in your week that have nothing to do with the
person you are anxious about.

A hobby with a fixed slot. A recurring social thing. Anything that means Saturday
already has a shape.

This is not a distraction technique. It genuinely changes the arithmetic: when
one interaction is one of eight, it stops being able to determine the week.

## The tell

You can measure this. Count how many things are in your week that you look
forward to and that do not involve that person.

If the answer is zero, that is the actual problem, and it is fixable without
going anywhere near the relationship.

## Do this

Put one thing in your calendar this week that you would enjoy alone. Then go to
it.
"""),

    ("cbt_toolkit", 0, "Four thinking traps and what to do about them",
     "The specific distortions that make social situations worse, and the counter to each.",
     6, ["mindset"], ["catastrophizes", "mind_reading", "assumes_judgement", "compares_constantly"], """
These are the four that do most of the damage socially. Each has a specific
counter.

## 1. Mind reading

*"They think I am boring."*

**Counter:** write the claim down and list the evidence. Not the feeling — the
evidence you could show someone else. The column is almost always empty.

## 2. Catastrophising

*"That was awkward, so now I cannot go back."*

**Counter:** write the actual worst case, then the most likely case. They are
rarely the same, and the likely one is usually "nothing".

## 3. Fortune telling

*"There is no point asking, they will say no."*

**Counter:** you are not permitted to predict. Run the experiment and record what
happened. Keep a count. Your prediction accuracy is worse than you think.

## 4. Personalising

*"They were quiet, so it was me."*

**Counter:** list three explanations that have nothing to do with you. Tired,
distracted, dealing with something. Then ask why yours was the default.

## The one rule that makes all four work

**Write it down.**

Doing this in your head is useless. Thoughts stay fast and vague, and vagueness
is where their power lives. On paper they have to be specific, and specific
claims can be checked.

Two minutes and a piece of paper.

## Do this

Take the thought you have had most this week. Write it as one sentence, then
list the evidence.
"""),

    ("eye_contact_guide", 0, "Eye contact, calibrated",
     "How much is right, when to break it, and why it feels like too much.",
     4, ["presence"], ["avoids_eye_contact", "walks_head_down"], """
Eye contact is the fastest-moving item in this whole app, because it is a pure
habit with no skill underneath it.

## The calibration

Roughly 60 per cent while listening, and slightly less while speaking. Breaking
occasionally, to the side rather than down.

Down is the one to avoid. Sideways reads as thinking; downwards reads as
retreating.

## Why it feels like too much

Because you have been running at close to zero, and the correction is measured
against your baseline, not against normal.

What feels like staring is almost always normal contact. There is no way to know
this except by doing it and observing that nobody reacts.

## The safest place to practise

Transactions. A cashier, a barista, someone at a desk. They have a script, a
fixed length and a guaranteed end, which makes them the ideal training ground.

## The harder rep

Holding contact through someone's entire answer to a question. This is the one
that actually changes how you are read, and it will feel like far too much.

It is not.

## Do this

Hold eye contact with one cashier today until they look away first. Once.
"""),

    ("listening", 0, "How to actually listen",
     "Most people are waiting. The difference is visible from outside.",
     5, ["social"], ["conversational_narcissist", "interrupts", "no_questions"], """
There is a difference between listening and waiting for your turn, and everyone
can tell which one you are doing.

## The tell

If, while they are talking, you are assembling what you will say next, you are
waiting. The giveaway is that your reply is about you — your version, your
similar experience, your opinion.

This feels like relating. It lands as redirecting.

## The fix, mechanically

**Ask about what they just said before you say anything about yourself.**

One follow-up question. That is the whole technique, and almost nobody does it,
which is why it is so noticeable when someone does.

## The harder version

Have a conversation where you never once bring the topic back to yourself.

You will feel like you contributed nothing. They will come away thinking it went
unusually well. Sit with that gap — it is the most useful thing in this article.

## Why this works

Being properly listened to is rare. Rare enough that people attribute all sorts
of qualities to whoever does it — warmth, intelligence, depth — none of which you
had to demonstrate.

## The test

After a conversation, write down three things about their life you did not know
before. If you cannot fill three lines, you were waiting.

## Do this

In your next conversation, ask one follow-up question before saying anything
about yourself.
"""),
]


def build():
    ids = set()
    days = {}
    out = []
    for lid, day, title, summary, minutes, tags, debuffs, body in LESSONS:
        assert lid not in ids, f"duplicate lesson id {lid}"
        ids.add(lid)
        body = body.strip()

        assert "## Do this" in body, f"{lid} has no 'Do this' section"
        assert len(body) > 700, f"{lid} is too short to be worth opening ({len(body)})"
        assert 2 <= minutes <= 12, f"{lid} has an implausible read time"

        if day:
            assert 1 <= day <= 30, f"{lid} has day {day} outside the course"
            assert day not in days, f"day {day} is claimed by both {days[day]} and {lid}"
            days[day] = lid
        else:
            assert debuffs, f"{lid} has no day and no debuffs, so nothing will ever show it"

        out.append({
            "id": lid,
            "day": day,
            "title": title,
            "summary": summary,
            "min": minutes,
            "tags": tags,
            "debuffs": debuffs,
            "body": body,
        })

    # The course must have no gaps: a missing day is a day the Learn tab shows nothing.
    missing = [d for d in range(1, 31) if d not in days]
    assert not missing, f"course days missing: {missing}"

    # Every debuff referenced by an article has to exist in the audit catalogue.
    audit_path = os.path.join(OUT, "..", "audit", "debuffs.json")
    if os.path.exists(audit_path):
        known = {d["id"] for d in json.load(open(audit_path))["debuffs"]}
        for lesson in out:
            for d in lesson["debuffs"]:
                assert d in known, f"{lesson['id']} references unknown debuff '{d}'"

        # Every category should have at least one article behind it, or ticking things
        # in that tab produces no reading at all.
        cats = {d["id"]: d["cat"] for d in json.load(open(audit_path))["debuffs"]}
        covered = {cats[d] for lesson in out for d in lesson["debuffs"]}
        for c in ("kinesics", "conversation", "presentation", "mindset"):
            assert c in covered, f"no lesson covers any '{c}' debuff"

    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, "curriculum.json"), "w") as f:
        json.dump({"lessons": out}, f, indent=1, ensure_ascii=False)

    course = [l for l in out if l["day"]]
    targeted = [l for l in out if not l["day"]]
    words = sum(len(l["body"].split()) for l in out)
    print(f"curriculum.json  {len(out)} lessons, {words} words")
    print(f"    course     {len(course)} (days 1-30, no gaps)")
    print(f"    targeted   {len(targeted)} triggered by "
          f"{len({d for l in targeted for d in l['debuffs']})} debuffs")


if __name__ == "__main__":
    build()
