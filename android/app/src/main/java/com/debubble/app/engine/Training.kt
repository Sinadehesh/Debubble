package com.debubble.app.engine

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/**
 * Training: the gym tracker, and the next small thing to do.
 *
 * It lives under your body, because it is the body's other habit: not looking after it, but
 * making it stronger. The design follows four findings, each of which is load-bearing:
 *
 *  1. **Any amount counts, and a little is most of it.** Adults are advised 150 to 300
 *     minutes of moderate activity a week and muscle-strengthening on two or more days, but
 *     the largest benefit comes from moving from none to some (Bull et al., 2020, British
 *     Journal of Sports Medicine: the WHO 2020 guidelines). So the week is counted against
 *     those two targets, and a single set is a perfectly good day.
 *
 *  2. **Short bursts work.** A minute or two of hard effort spread through the day, an
 *     "exercise snack", improves fitness in people who would never do a full session (Islam,
 *     Gibala & Little, 2022, Exercise and Sport Sciences Reviews). That is the "move for right
 *     now": one exercise, one set, sized to what you did last time.
 *
 *  3. **Progress is a rep at a time.** Double progression, the standard novice model: add a
 *     rep each session until the top of the range, then a harder version or a little more
 *     weight, and back to the bottom of the range (American College of Sports Medicine, 2009,
 *     position stand on progression in resistance training). Every suggestion is what you
 *     actually did last time plus one step, so it is never a guess about who you should be.
 *
 *  4. **It helps your mood, which is the point here.** Walking, jogging, yoga and strength
 *     training all reduced depression in a network meta-analysis of 218 trials (Noetel et
 *     al., 2024, BMJ). Nothing here counts calories or weighs anyone.
 *
 * Pure: same inputs, same outputs, no clock and no Android.
 */

/** What a set is counted in. */
enum class Measure(val unit: String, val short: String) {
    REPS("reps", ""),
    SECONDS("seconds", "s"),
    MINUTES("minutes", " min")
}

/** What it needs. Home suggestions only use [NONE]. */
enum class Gear(val label: String) {
    NONE("No kit"),
    DUMBBELL("Dumbbells"),
    BARBELL("Barbell"),
    MACHINE("Machine"),
    BAR("Pull-up bar")
}

/** Where you train, which decides what gets suggested. */
enum class Place(val label: String, val detail: String) {
    HOME("At home", "Nothing to buy: a floor, a wall and a chair."),
    GYM("At the gym", "Dumbbells, barbells and machines as well.")
}

enum class MoveGroup(val label: String, val strength: Boolean) {
    PUSH("Push", true),
    PULL("Pull", true),
    LEGS("Legs", true),
    CORE("Core", true),
    CARDIO("Heart and lungs", false),
    STRETCH("Stretch", false);

    companion object {
        val strengthGroups = entries.filter { it.strength }
    }
}

/** One exercise, and where it sits on its ladder. */
data class Exercise(
    val id: String,
    val name: String,
    val group: MoveGroup,
    val measure: Measure,
    val gear: Gear,
    /** Where a beginner starts, in its own measure. */
    val start: Int,
    /** The top of the range: here it is time for the harder version, or more weight. */
    val top: Int,
    /** How much to add after a session that reached its target. */
    val step: Int = 1,
    /** How to do it well, in one breath. */
    val cue: String,
    val easier: String? = null,
    val harder: String? = null,
    /** Loaded: progresses by weight once the reps reach [top]. */
    val startKg: Float = 0f,
    val kgStep: Float = 2.5f,
    /** Counted per side, so "8" means eight each. */
    val eachSide: Boolean = false,
    /** The room sprite that stands for it. */
    val icon: String = "dumbbells"
) {
    val weighted: Boolean get() = startKg > 0f

    /** "8 knee push-ups", "30 s plank", "15 min brisk walk". */
    fun amount(n: Int): String = when (measure) {
        Measure.REPS -> "$n" + if (eachSide) " each side" else ""
        Measure.SECONDS -> "$n s" + if (eachSide) " each side" else ""
        Measure.MINUTES -> "$n min"
    }
}

/** One logged set. Kept forever: it is the record of getting stronger. */
@Serializable
data class WorkoutSet(
    /** When it was logged, in milliseconds. Also its identity. */
    val id: Long,
    val exercise: String,
    /** Reps, seconds or minutes, in the exercise's own [Measure]. */
    val amount: Int,
    /** Zero for bodyweight. */
    val kg: Float = 0f,
    val epochDay: Long,
    /** Minute of the local day it was logged, for "when do I usually train". */
    val minute: Int = 0
)

/** What to do next at one exercise, and why that number. */
data class Target(
    val exercise: Exercise,
    val amount: Int,
    val kg: Float,
    val sets: Int,
    val why: String
) {
    /** "8 knee push-ups", "30 s plank", "6 split squats each side · 10 kg". */
    val label: String
        get() {
            val side = if (exercise.eachSide) " each side" else ""
            val what = exercise.name.lowercase()
            val base = when (exercise.measure) {
                Measure.REPS -> "$amount $what$side"
                Measure.SECONDS -> "$amount s $what$side"
                Measure.MINUTES -> "$amount min $what"
            }
            return base + if (kg > 0f) " · ${Training.kg(kg)}" else ""
        }
}

/** This week against the WHO's two numbers. */
data class Week(
    val strengthDays: Int,
    val activeMinutes: Int,
    /** Monday to Sunday: was anything done that day. */
    val days: List<Boolean>
) {
    companion object {
        const val STRENGTH_DAYS = 2
        const val ACTIVE_MINUTES = 150
    }
}

/** A best, for the "personal bests" list. */
data class Best(val exercise: Exercise, val amount: Int, val kg: Float, val epochDay: Long)

object Moves {

    val all: List<Exercise> = listOf(
        /* ---- push: a ladder from the wall to the floor and past it ---- */
        Exercise("wall_pushup", "Wall push-ups", MoveGroup.PUSH, Measure.REPS, Gear.NONE, 8, 15,
            cue = "Hands on the wall at shoulder height, body in one straight line. Chest to the wall, then push away.",
            harder = "incline_pushup"),
        Exercise("incline_pushup", "Incline push-ups", MoveGroup.PUSH, Measure.REPS, Gear.NONE, 6, 12,
            cue = "Hands on a table or the kitchen counter. The higher the surface, the easier it is.",
            easier = "wall_pushup", harder = "knee_pushup"),
        Exercise("knee_pushup", "Knee push-ups", MoveGroup.PUSH, Measure.REPS, Gear.NONE, 5, 12,
            cue = "Knees down, hips in line with your shoulders. Lower until your chest nearly touches the floor.",
            easier = "incline_pushup", harder = "pushup"),
        Exercise("pushup", "Push-ups", MoveGroup.PUSH, Measure.REPS, Gear.NONE, 3, 15,
            cue = "Hands under your shoulders, body like a plank. Lower to a fist's height from the floor, then press.",
            easier = "knee_pushup", harder = "feet_up_pushup"),
        Exercise("feet_up_pushup", "Feet-up push-ups", MoveGroup.PUSH, Measure.REPS, Gear.NONE, 4, 15,
            cue = "Feet on a step or the sofa, so more of your weight goes through your arms.",
            easier = "pushup"),
        Exercise("chair_dip", "Chair dips", MoveGroup.PUSH, Measure.REPS, Gear.NONE, 5, 15,
            cue = "Hands on a sturdy chair behind you. Bend your elbows to a right angle, then press up."),
        Exercise("db_press", "Dumbbell press", MoveGroup.PUSH, Measure.REPS, Gear.DUMBBELL, 8, 12,
            cue = "On a bench, press both dumbbells up over your chest, then lower them slowly.", startKg = 8f, kgStep = 2f),
        Exercise("bench_press", "Bench press", MoveGroup.PUSH, Measure.REPS, Gear.BARBELL, 6, 10,
            cue = "Feet flat, shoulder blades pinched, bar to the middle of your chest. Use safety bars or a spotter.",
            startKg = 20f),
        Exercise("overhead_press", "Overhead press", MoveGroup.PUSH, Measure.REPS, Gear.DUMBBELL, 8, 12,
            cue = "Stand tall, brace your stomach, press straight up and keep your ribs down.", startKg = 6f, kgStep = 2f),

        /* ---- pull ---- */
        Exercise("superman", "Supermans", MoveGroup.PULL, Measure.REPS, Gear.NONE, 8, 15,
            cue = "Lie face down. Lift your chest and legs a little, hold for a second, lower.",
            harder = "door_row"),
        Exercise("door_row", "Door-frame rows", MoveGroup.PULL, Measure.REPS, Gear.NONE, 6, 15,
            cue = "Hold both sides of a door frame, lean back on straight arms, pull your chest to the frame. The frame, never the door.",
            easier = "superman"),
        Exercise("db_row", "Dumbbell rows", MoveGroup.PULL, Measure.REPS, Gear.DUMBBELL, 8, 12,
            cue = "One hand and knee on a bench, pull the dumbbell to your hip, lower it slowly.", startKg = 8f, kgStep = 2f,
            eachSide = true),
        Exercise("lat_pulldown", "Lat pulldown", MoveGroup.PULL, Measure.REPS, Gear.MACHINE, 8, 12,
            cue = "Pull the bar to your upper chest, elbows down and back, and let it rise slowly.", startKg = 25f, kgStep = 5f),
        Exercise("seated_row", "Seated cable row", MoveGroup.PULL, Measure.REPS, Gear.MACHINE, 8, 12,
            cue = "Sit tall, pull the handle to your stomach, squeeze your shoulder blades, return slowly.", startKg = 25f, kgStep = 5f),
        Exercise("dead_hang", "Dead hang", MoveGroup.PULL, Measure.SECONDS, Gear.BAR, 10, 45, step = 5,
            cue = "Hang from the bar on straight arms. Breathe, and let your shoulders take it.", harder = "chin_up"),
        Exercise("chin_up", "Chin-ups", MoveGroup.PULL, Measure.REPS, Gear.BAR, 1, 10,
            cue = "Palms facing you, pull your chin over the bar, lower all the way down.", easier = "dead_hang"),

        /* ---- legs ---- */
        Exercise("sit_to_stand", "Sit-to-stands", MoveGroup.LEGS, Measure.REPS, Gear.NONE, 8, 15,
            cue = "Sit on a chair, stand up without using your hands, sit back down slowly.", harder = "squat"),
        Exercise("squat", "Squats", MoveGroup.LEGS, Measure.REPS, Gear.NONE, 8, 20,
            cue = "Feet shoulder-width apart. Sit back and down as if into a chair, knees following your toes.",
            easier = "sit_to_stand", harder = "split_squat"),
        Exercise("split_squat", "Split squats", MoveGroup.LEGS, Measure.REPS, Gear.NONE, 6, 12,
            cue = "One foot forward, one back. Lower your back knee towards the floor, then stand.",
            easier = "squat", eachSide = true),
        Exercise("glute_bridge", "Glute bridges", MoveGroup.LEGS, Measure.REPS, Gear.NONE, 10, 20,
            cue = "On your back, knees bent. Push your hips up until your body is straight, squeeze, lower.", icon = "yoga_mat"),
        Exercise("calf_raise", "Calf raises", MoveGroup.LEGS, Measure.REPS, Gear.NONE, 12, 25, step = 2,
            cue = "On the edge of a step, rise onto your toes, pause, lower slowly."),
        Exercise("wall_sit", "Wall sit", MoveGroup.LEGS, Measure.SECONDS, Gear.NONE, 20, 60, step = 5,
            cue = "Back flat on the wall, slide down until your knees are bent, and hold."),
        Exercise("goblet_squat", "Goblet squats", MoveGroup.LEGS, Measure.REPS, Gear.DUMBBELL, 8, 12,
            cue = "Hold one dumbbell at your chest, squat between your knees, stand tall.", startKg = 8f, kgStep = 2f),
        Exercise("back_squat", "Barbell squats", MoveGroup.LEGS, Measure.REPS, Gear.BARBELL, 6, 10,
            cue = "Bar on your upper back, brace, sit down between your hips, drive up. Set the safety bars.", startKg = 20f),
        Exercise("deadlift", "Deadlifts", MoveGroup.LEGS, Measure.REPS, Gear.BARBELL, 5, 8,
            cue = "Bar over the middle of your foot, flat back, push the floor away, stand tall, lower with control.",
            startKg = 30f, kgStep = 5f),
        Exercise("rdl", "Romanian deadlifts", MoveGroup.LEGS, Measure.REPS, Gear.DUMBBELL, 8, 12,
            cue = "Soft knees, push your hips back until you feel your hamstrings, then stand.", startKg = 10f, kgStep = 2f),
        Exercise("leg_press", "Leg press", MoveGroup.LEGS, Measure.REPS, Gear.MACHINE, 10, 15,
            cue = "Feet flat on the plate, lower until your knees are bent, press without locking them.", startKg = 40f, kgStep = 10f),

        /* ---- core ---- */
        Exercise("dead_bug", "Dead bugs", MoveGroup.CORE, Measure.REPS, Gear.NONE, 6, 16, step = 2,
            cue = "On your back, arms up. Lower the opposite arm and leg slowly, lower back pressed to the floor.",
            harder = "plank", icon = "yoga_mat"),
        Exercise("bird_dog", "Bird dogs", MoveGroup.CORE, Measure.REPS, Gear.NONE, 6, 16, step = 2,
            cue = "On hands and knees, reach one arm forward and the opposite leg back, hold, swap.", icon = "yoga_mat"),
        Exercise("plank", "Plank", MoveGroup.CORE, Measure.SECONDS, Gear.NONE, 15, 60, step = 5,
            cue = "Forearms down, one straight line from head to heels. Breathe the whole time.",
            easier = "dead_bug", harder = "side_plank", icon = "yoga_mat"),
        Exercise("side_plank", "Side plank", MoveGroup.CORE, Measure.SECONDS, Gear.NONE, 10, 45, step = 5,
            cue = "On one forearm, hips up, body straight. Knees down makes it easier.",
            easier = "plank", eachSide = true, icon = "yoga_mat"),

        /* ---- heart and lungs ---- */
        Exercise("walk", "Brisk walk", MoveGroup.CARDIO, Measure.MINUTES, Gear.NONE, 10, 45, step = 5,
            cue = "Fast enough that you could talk but not sing.", icon = "sneakers"),
        Exercise("stairs", "Stairs", MoveGroup.CARDIO, Measure.MINUTES, Gear.NONE, 3, 15, step = 1,
            cue = "Up at a steady pace, down carefully. Hold the rail.", icon = "sneakers"),
        Exercise("jumping_jacks", "Jumping jacks", MoveGroup.CARDIO, Measure.REPS, Gear.NONE, 20, 60, step = 5,
            cue = "Jump your feet out as your arms go up, back in as they come down. Step it out if jumping is too much.",
            icon = "sneakers"),
        Exercise("run", "Run", MoveGroup.CARDIO, Measure.MINUTES, Gear.NONE, 5, 30, step = 2,
            cue = "Slow enough to hold a conversation. Walk breaks are part of it.", icon = "sneakers"),
        Exercise("cycle", "Cycling", MoveGroup.CARDIO, Measure.MINUTES, Gear.NONE, 10, 45, step = 5,
            cue = "Any bike, outside or still. Easy gear, steady legs.", icon = "bike"),

        /* ---- stretch ---- */
        Exercise("stretch", "Stretch", MoveGroup.STRETCH, Measure.MINUTES, Gear.NONE, 5, 15, step = 1,
            cue = "Neck, shoulders, hips, calves. Breathe out into each one, and never bounce.", icon = "yoga_mat")
    )

    private val byId = all.associateBy { it.id }

    fun byId(id: String): Exercise? = byId[id]

    fun inGroup(g: MoveGroup): List<Exercise> = all.filter { it.group == g }

    /** What a place can do: home is bodyweight only, the gym is everything. */
    fun at(place: Place): List<Exercise> = when (place) {
        Place.HOME -> all.filter { it.gear == Gear.NONE }
        Place.GYM -> all
    }

    /** Where someone new to a group starts, at a place. */
    fun starter(group: MoveGroup, place: Place): Exercise = when (place) {
        Place.HOME -> when (group) {
            MoveGroup.PUSH -> byId.getValue("wall_pushup")
            MoveGroup.PULL -> byId.getValue("superman")
            MoveGroup.LEGS -> byId.getValue("sit_to_stand")
            MoveGroup.CORE -> byId.getValue("dead_bug")
            MoveGroup.CARDIO -> byId.getValue("walk")
            MoveGroup.STRETCH -> byId.getValue("stretch")
        }
        Place.GYM -> when (group) {
            MoveGroup.PUSH -> byId.getValue("db_press")
            MoveGroup.PULL -> byId.getValue("lat_pulldown")
            MoveGroup.LEGS -> byId.getValue("goblet_squat")
            MoveGroup.CORE -> byId.getValue("plank")
            MoveGroup.CARDIO -> byId.getValue("walk")
            MoveGroup.STRETCH -> byId.getValue("stretch")
        }
    }
}

object Training {

    /** After this long away, the next suggestion steps back rather than forward. */
    const val COMING_BACK_DAYS = 14L

    /** Sets per exercise in a suggested session: two to begin with, three once it is a habit. */
    const val SETS_NEW = 2
    const val SETS_SETTLED = 3
    const val SETTLED_AFTER_DAYS = 6

    fun kg(kg: Float): String =
        if (kg == kg.toInt().toFloat()) "${kg.toInt()} kg" else "${"%.1f".format(kg)} kg"

    /** Monday of the week [day] is in. Epoch day 0 was a Thursday. */
    fun weekStart(day: Long): Long = day - Math.floorMod(day + 3, 7L)

    /** Days anything at all was trained. */
    fun days(sets: List<WorkoutSet>): Set<Long> = sets.map { it.epochDay }.toSet()

    fun on(sets: List<WorkoutSet>, day: Long): List<WorkoutSet> = sets.filter { it.epochDay == day }

    /**
     * The sets of the last session at [exercise] before [today]: the evidence the next
     * target is built on. Today's own sets are not used, so logging a set does not move the
     * goal you are in the middle of.
     */
    fun lastSession(sets: List<WorkoutSet>, exercise: String, today: Long): List<WorkoutSet> {
        val earlier = sets.filter { it.exercise == exercise && it.epochDay < today }
        val day = earlier.maxOfOrNull { it.epochDay } ?: return emptyList()
        return earlier.filter { it.epochDay == day }
    }

    /**
     * What to aim for at one exercise: double progression.
     *
     * The last session's best set, plus one step, until the top of the range; then the harder
     * version at its start (bodyweight) or a little more weight back at the bottom of the
     * range (loaded). After a long gap it steps back, because a first session back that is
     * too hard is the one that ends the habit.
     */
    fun target(ex: Exercise, sets: List<WorkoutSet>, today: Long, settled: Boolean = false): Target {
        val setsCount = if (settled) SETS_SETTLED else SETS_NEW
        val last = lastSession(sets, ex.id, today)
        if (last.isEmpty()) {
            return Target(ex, ex.start, ex.startKg, setsCount, "A starting point. Easy is the right place to begin.")
        }
        val gap = today - last.first().epochDay
        if (ex.weighted) {
            val kg = last.maxOf { it.kg }.takeIf { it > 0f } ?: ex.startKg
            val best = last.filter { it.kg >= kg }.maxOf { it.amount }
            return when {
                gap > COMING_BACK_DAYS -> Target(ex, ex.start, max(ex.startKg, kg - ex.kgStep), setsCount,
                    "Welcome back. A little lighter than last time, to find your feet.")
                best >= ex.top -> Target(ex, ex.start, kg + ex.kgStep, setsCount,
                    "You reached ${ex.top} last time, so a little more weight and back to ${ex.start}.")
                else -> Target(ex, min(best + ex.step, ex.top), kg, setsCount,
                    "One more than your best last time ($best).")
            }
        }
        val best = last.maxOf { it.amount }
        val harder = ex.harder?.let { Moves.byId(it) }
        return when {
            gap > COMING_BACK_DAYS -> Target(ex, max(ex.start, best - ex.step * 2), 0f, setsCount,
                "Welcome back. A little easier than last time.")
            best >= ex.top && harder != null -> Target(harder, harder.start, 0f, setsCount,
                "${ex.top} ${ex.name.lowercase()} is the top of that ladder, so here is the next one up.")
            best >= ex.top -> Target(ex, ex.top, 0f, setsCount, "That is the top of the range. Hold it here.")
            else -> Target(ex, min(best + ex.step, ex.top), 0f, setsCount,
                "One step past your best last time (${ex.amount(best)}).")
        }
    }

    /**
     * Where you are on a group's ladder: the exercise in it you did most recently that the
     * place can do, or the place's starter for it.
     */
    fun current(group: MoveGroup, sets: List<WorkoutSet>, place: Place): Exercise {
        val available = Moves.at(place).map { it.id }.toSet()
        val latest = sets.filter { it.exercise in available }
            .filter { Moves.byId(it.exercise)?.group == group }
            .maxByOrNull { it.id }
        return latest?.let { Moves.byId(it.exercise) } ?: Moves.starter(group, place)
    }

    /** When each strength group was last trained, oldest first: what is most due. */
    fun dueGroups(sets: List<WorkoutSet>, today: Long, offset: Int = 0): List<MoveGroup> {
        val groups = MoveGroup.strengthGroups
        val last = groups.associateWith { g ->
            sets.filter { Moves.byId(it.exercise)?.group == g }.maxOfOrNull { it.epochDay } ?: Long.MIN_VALUE
        }
        // Ties (never trained, or trained the same day) rotate by date, so a new user does
        // not get push-ups every single time.
        val spin = Math.floorMod(today + offset, groups.size.toLong()).toInt()
        val rotated = groups.drop(spin) + groups.take(spin)
        return rotated.sortedBy { last.getValue(it) }
    }

    /**
     * A move for right now: one set of whatever is most due, sized to what you did last time.
     * [skip] steps through the next most due, for "a different one".
     */
    fun snack(sets: List<WorkoutSet>, today: Long, place: Place, skip: Int = 0): Target {
        val doneToday = on(sets, today).mapNotNull { Moves.byId(it.exercise)?.group }.toSet()
        val due = dueGroups(sets, today).sortedBy { it in doneToday }
        val group = due[Math.floorMod(skip, due.size)]
        val t = target(current(group, sets, place), sets, today)
        return t.copy(sets = 1)
    }

    /** Today's session: one exercise from each of the three most due strength groups. */
    fun session(sets: List<WorkoutSet>, today: Long, place: Place): List<Target> {
        val settled = days(sets).size >= SETTLED_AFTER_DAYS
        return dueGroups(sets, today).take(3).map { g -> target(current(g, sets, place), sets, today, settled) }
    }

    /** Easier and harder versions of a target, for the two buttons beside it. */
    fun easier(t: Target, sets: List<WorkoutSet>, today: Long): Target? =
        t.exercise.easier?.let { Moves.byId(it) }?.let { target(it, sets, today).copy(sets = t.sets) }

    fun harder(t: Target, sets: List<WorkoutSet>, today: Long): Target? =
        t.exercise.harder?.let { Moves.byId(it) }?.let { target(it, sets, today).copy(sets = t.sets) }

    /**
     * This week, Monday to Sunday, against the WHO targets. Strength days are days with any
     * strength set; active minutes are minutes of walking, running, cycling and the like.
     */
    fun week(sets: List<WorkoutSet>, today: Long): Week {
        val start = weekStart(today)
        val inWeek = sets.filter { it.epochDay in start..(start + 6) }
        val strengthDays = inWeek.filter { Moves.byId(it.exercise)?.group?.strength == true }
            .map { it.epochDay }.distinct().size
        val minutes = inWeek.filter { Moves.byId(it.exercise)?.measure == Measure.MINUTES }.sumOf { it.amount }
        val days = (0..6).map { i -> inWeek.any { it.epochDay == start + i } }
        return Week(strengthDays, minutes, days)
    }

    /** The best set ever at every exercise done, most recent first. */
    fun bests(sets: List<WorkoutSet>): List<Best> = sets.groupBy { it.exercise }.mapNotNull { (id, list) ->
        val ex = Moves.byId(id) ?: return@mapNotNull null
        val best = if (ex.weighted) {
            list.maxWithOrNull(compareBy<WorkoutSet> { it.kg }.thenBy { it.amount })
        } else {
            list.maxWithOrNull(compareBy<WorkoutSet> { it.amount }.thenBy { -it.id })
        } ?: return@mapNotNull null
        Best(ex, best.amount, best.kg, best.epochDay)
    }.sortedByDescending { it.epochDay }

    /** "Push-ups 12, 10 · Squats 15, 15": one line for one day. */
    fun summary(day: List<WorkoutSet>): String = day.groupBy { it.exercise }.entries
        .sortedBy { e -> e.value.minOf { it.id } }
        .mapNotNull { (id, list) ->
            val ex = Moves.byId(id) ?: return@mapNotNull null
            val amounts = list.sortedBy { it.id }.joinToString(", ") { s ->
                ex.amount(s.amount).substringBefore(" each") + if (s.kg > 0f) " × ${kg(s.kg)}" else ""
            }
            "${ex.name} $amounts"
        }.joinToString(" · ")
}

/* ======================================================================= plans */

/** One exercise in a plan session: so many sets of so much. */
data class PlanItem(val exercise: String, val sets: Int, val amount: Int)

data class PlanSession(val title: String, val items: List<PlanItem>)

/**
 * A plan: a set number of sessions in order, each a little harder than the last.
 *
 * Counted in sessions rather than dates, so a missed week does not leave you "behind": the
 * next session is simply the next one. That is the same rule as the rest of the app.
 */
data class Plan(
    val id: String,
    val name: String,
    val blurb: String,
    val perWeek: Int,
    val place: Place,
    val icon: String,
    val sessions: List<PlanSession>
) {
    val length: Int get() = sessions.size
}

object Plans {

    private fun s(title: String, vararg items: PlanItem) = PlanSession(title, items.toList())
    private fun i(exercise: String, sets: Int, amount: Int) = PlanItem(exercise, sets, amount)

    /** From the wall to the floor in twelve sessions. */
    val firstPushup = Plan(
        id = "first_pushup",
        name = "Your first push-up",
        blurb = "Twelve sessions, three a week, from the wall to a real push-up on the floor.",
        perWeek = 3, place = Place.HOME, icon = "dumbbells",
        sessions = listOf(
            s("Wall, gently", i("wall_pushup", 3, 8)),
            s("Wall, a bit more", i("wall_pushup", 3, 10)),
            s("Wall, strong", i("wall_pushup", 3, 12)),
            s("Up to the table", i("incline_pushup", 3, 6)),
            s("Table again", i("incline_pushup", 3, 8)),
            s("Table, strong", i("incline_pushup", 3, 10)),
            s("Down to your knees", i("knee_pushup", 3, 5)),
            s("Knees again", i("knee_pushup", 3, 7)),
            s("Knees, strong", i("knee_pushup", 3, 9)),
            s("Your first ones", i("pushup", 3, 1), i("knee_pushup", 1, 8)),
            s("Two at a time", i("pushup", 3, 2), i("knee_pushup", 1, 8)),
            s("Three in a row", i("pushup", 3, 3), i("knee_pushup", 1, 10))
        )
    )

    /** A whole-body routine for a living room, alternating two sessions. */
    val strongAtHome = Plan(
        id = "strong_home",
        name = "Strong at home",
        blurb = "Twelve whole-body sessions, three a week, with nothing to buy.",
        perWeek = 3, place = Place.HOME, icon = "yoga_mat",
        sessions = (0 until 12).map { n ->
            val w = n / 3 // one step harder each week
            if (n % 2 == 0) s("Session A",
                i("squat", 3, 10 + w), i("knee_pushup", 3, 6 + w), i("glute_bridge", 2, 12 + w), i("plank", 2, 20 + w * 5))
            else s("Session B",
                i("split_squat", 3, 6 + w), i("superman", 3, 10 + w), i("dead_bug", 2, 8 + w * 2), i("wall_sit", 2, 25 + w * 5))
        }
    )

    /** A first gym routine: two whole-body sessions, alternating, weights from your own log. */
    val gymStarter = Plan(
        id = "gym_starter",
        name = "Gym starter",
        blurb = "Twelve sessions, three a week, the basics with weights. Every weight comes from what you lifted last time.",
        perWeek = 3, place = Place.GYM, icon = "dumbbells",
        sessions = (0 until 12).map { n ->
            if (n % 2 == 0) s("Session A",
                i("goblet_squat", 3, 8), i("db_press", 3, 8), i("lat_pulldown", 3, 10), i("plank", 2, 30))
            else s("Session B",
                i("rdl", 3, 8), i("overhead_press", 3, 8), i("seated_row", 3, 10), i("dead_bug", 2, 10))
        }
    )

    /** From a ten-minute walk to thirty, in eighteen walks. */
    val walkItOff = Plan(
        id = "walk_it_off",
        name = "Walk it off",
        blurb = "Eighteen walks, three a week, from ten minutes to thirty. Fast enough to talk, not sing.",
        perWeek = 3, place = Place.HOME, icon = "sneakers",
        sessions = listOf(10, 10, 12, 12, 15, 15, 18, 18, 20, 20, 22, 22, 25, 25, 28, 28, 30, 30)
            .mapIndexed { n, min -> s("Walk ${n + 1}", i("walk", 1, min)) }
    )

    val all: List<Plan> = listOf(firstPushup, strongAtHome, gymStarter, walkItOff)

    fun byId(id: String): Plan? = all.firstOrNull { it.id == id }

    /** The session to do next, or null when the plan is finished. */
    fun next(plan: Plan, done: Int): PlanSession? = plan.sessions.getOrNull(done)

    /** How many sets of each item in [session] have been logged on [day]. */
    fun progress(session: PlanSession, sets: List<WorkoutSet>, day: Long): Map<PlanItem, Int> =
        session.items.associateWith { item -> sets.count { it.epochDay == day && it.exercise == item.exercise } }

    /** Every set of every item logged on [day]. */
    fun complete(session: PlanSession, sets: List<WorkoutSet>, day: Long): Boolean =
        progress(session, sets, day).all { (item, n) -> n >= item.sets }
}
