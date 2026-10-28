package com.debubble.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.debubble.app.MainActivity
import com.debubble.app.R
import com.debubble.app.data.Repository
import com.debubble.app.data.dirt
import com.debubble.app.data.quests
import com.debubble.app.engine.CareLibrary
import com.debubble.app.engine.Curriculum
import com.debubble.app.engine.Today
import com.debubble.app.ui.components.PixelAtlas
import com.debubble.app.ui.components.RoomPainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * The home-screen widget: your room, and today's four.
 *
 * It is the one piece of the product that reaches someone who has not opened the app, so it
 * shows the two things that pull people back in. The room, because it is theirs and it is a
 * mirror — a sock on the floor on the home screen is a better reminder than any notification,
 * and a clean one is a small pleasure every time the phone is unlocked. And the four tasks,
 * each led by its cue, because a plan works when it is in front of you at the moment it names.
 *
 * Tapping a task opens that task; tapping the room opens the app. See [WidgetCues] for what
 * it says and why there is no tick-box that skips the app.
 *
 * Two sizes from one provider. Tall enough, and each task gets a row with its cue; shorter,
 * and the four become a strip of icons under the room. Decided from the size the launcher
 * reports, so resizing the widget changes it.
 */
class DayWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        render(context, manager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        render(context, manager, intArrayOf(appWidgetId))
    }

    /**
     * The date rolling over changes what the widget should say without anything in the app
     * happening: the room slips, the tasks reset, the mission changes. DATE_CHANGED no longer
     * reaches a manifest receiver on Android 8+, so each render also books one inexact alarm
     * for just after midnight, addressed to this receiver by name.
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_MIDNIGHT,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> refresh(context)
        }
    }

    override fun onDisabled(context: Context) {
        runCatching { context.getSystemService(AlarmManager::class.java)?.cancel(midnight(context)) }
    }

    private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val app = context.applicationContext
        // goAsync() would be the textbook answer, but AppWidgetProvider's pending result is
        // single-use and onUpdate can be re-entered; the update itself is idempotent and the
        // process stays alive long enough for a DataStore read, so a plain scope is safer
        // here than a result that might already have been finished.
        CoroutineScope(Dispatchers.IO).launch {
            val today = LocalDate.now().toEpochDay()
            val drawn = runCatching {
                val state = Repository(app).state.first().rolledTo(today)
                val quests = state.quests(today, care(app), curriculum(app))
                val cue = WidgetCues.of(state, today, quests)
                val room = if (!cue.ready) null else RoomPainter.paint(
                    context = app,
                    atlas = atlas(app),
                    stage = state.stage,
                    home = state.home,
                    dirt = state.dirt(today),
                    letter = Today.letter(quests),
                    scale = ROOM_SCALE
                )
                cue to room
            }.getOrElse { WidgetCues.unavailable to null }

            ids.forEach { id ->
                runCatching {
                    val tall = isTall(manager.getAppWidgetOptions(id))
                    manager.updateAppWidget(id, views(app, drawn.first, drawn.second, tall))
                }
            }
            scheduleMidnight(app)
        }
    }

    // ------------------------------------------------------------------ drawing

    private fun colourOf(tone: WidgetCue.Tone): Int = when (tone) {
        WidgetCue.Tone.BODY -> R.color.ink_body
        WidgetCue.Tone.ROOM -> R.color.ink_room
        WidgetCue.Tone.HEART -> R.color.ink_heart
        WidgetCue.Tone.ACCESS -> R.color.ink_access
        WidgetCue.Tone.ACTIVITY -> R.color.ink_activity
        WidgetCue.Tone.SOCIAL -> R.color.ink_social
    }

    private fun views(context: Context, c: WidgetCue, room: Bitmap?, tall: Boolean): RemoteViews {
        val v = RemoteViews(context.packageName, if (tall) R.layout.widget_room else R.layout.widget_room_compact)
        val open = deepLink(context, EXTRA_OPEN_CHALLENGE, "")
        v.setOnClickPendingIntent(R.id.widget_root, open)
        v.setOnClickPendingIntent(R.id.widget_room, open)
        if (room != null) v.setImageViewBitmap(R.id.widget_room, room)

        v.setTextViewText(R.id.widget_count, c.count)
        if (tall) {
            v.setTextViewText(R.id.widget_title, c.title)
            v.setTextViewText(R.id.widget_plan, c.plan)
            v.setViewVisibility(R.id.widget_plan, if (c.plan.isBlank()) View.GONE else View.VISIBLE)
            v.setViewVisibility(R.id.widget_review, if (c.review) View.VISIBLE else View.GONE)
            v.setOnClickPendingIntent(R.id.widget_review, deepLink(context, EXTRA_OPEN_REVIEW, "1"))
        }

        ROWS.forEachIndexed { i, ids ->
            val row = c.rows.getOrNull(i)
            v.setViewVisibility(ids.row, if (row == null) View.GONE else View.VISIBLE)
            if (row == null) return@forEachIndexed
            icon(context, row.icon)?.let { v.setImageViewBitmap(ids.icon, it) }
            icon(context, if (row.done) Today.ICON_DONE else Today.ICON_TODO)?.let {
                v.setImageViewBitmap(ids.check, it)
            }
            v.setContentDescription(ids.row, "${row.name}. ${row.text}")
            v.setOnClickPendingIntent(ids.row, deepLink(context, EXTRA_OPEN_CHALLENGE, row.link))
            if (tall) {
                v.setTextViewText(ids.name, row.name)
                v.setTextColor(ids.name, context.getColor(colourOf(row.tone)))
                v.setTextViewText(ids.text, row.text)
                v.setTextColor(
                    ids.text,
                    context.getColor(if (row.done) R.color.ink_muted else R.color.ink_primary)
                )
            }
        }
        return v
    }

    private fun deepLink(context: Context, extra: String, value: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (value.isNotEmpty()) putExtra(extra, value)
        }
        return PendingIntent.getActivity(
            context,
            // One request code per destination. Extras do not make two PendingIntents
            // different, so four rows sharing a code would all open the last one set.
            (extra + ":" + value).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** The view ids of one task row. The compact layout has no name or text views. */
    private class RowIds(val row: Int, val icon: Int, val check: Int, val name: Int, val text: Int)

    companion object {
        const val EXTRA_OPEN_CHALLENGE = "open_challenge"
        const val EXTRA_OPEN_REVIEW = "open_review"
        private const val ACTION_MIDNIGHT = "com.debubble.app.widget.MIDNIGHT"

        /**
         * Room pixels to bitmap pixels. Three keeps the bitmap near 580 KB, comfortably under
         * what a launcher will accept, while the ImageView's own scaling stays under 2x on
         * most phones so the pixels stay close to crisp.
         */
        private const val ROOM_SCALE = 3
        private const val ICON_SCALE = 4

        /** Below this, the tasks are a strip of icons rather than rows. */
        private const val TALL_DP = 300

        private val ROWS = listOf(
            RowIds(R.id.widget_row_0, R.id.widget_icon_0, R.id.widget_check_0, R.id.widget_name_0, R.id.widget_text_0),
            RowIds(R.id.widget_row_1, R.id.widget_icon_1, R.id.widget_check_1, R.id.widget_name_1, R.id.widget_text_1),
            RowIds(R.id.widget_row_2, R.id.widget_icon_2, R.id.widget_check_2, R.id.widget_name_2, R.id.widget_text_2),
            RowIds(R.id.widget_row_3, R.id.widget_icon_3, R.id.widget_check_3, R.id.widget_name_3, R.id.widget_text_3)
        )

        /** Portrait height is the max the launcher reports; landscape is the min. */
        private fun isTall(options: Bundle?): Boolean {
            val h = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) ?: 0
            return h == 0 || h >= TALL_DP
        }

        /* Parsed once per process: the ladders are ~190 KB of JSON and the atlas is a decode. */
        @Volatile private var cachedCurriculum: Curriculum? = null
        @Volatile private var cachedCare: CareLibrary? = null
        @Volatile private var cachedAtlas: PixelAtlas? = null
        private val icons = mutableMapOf<String, Bitmap>()

        private fun curriculum(context: Context): Curriculum =
            cachedCurriculum ?: synchronized(this) {
                cachedCurriculum ?: Repository(context).loadCurriculum().also { cachedCurriculum = it }
            }

        private fun care(context: Context): CareLibrary =
            cachedCare ?: synchronized(this) {
                cachedCare ?: Repository(context).loadCare().also { cachedCare = it }
            }

        private fun atlas(context: Context): PixelAtlas =
            cachedAtlas ?: synchronized(this) {
                cachedAtlas ?: PixelAtlas.load(context, Repository(context).loadItems()).also { cachedAtlas = it }
            }

        private fun icon(context: Context, id: String): Bitmap? = synchronized(icons) {
            icons[id] ?: RoomPainter.icon(atlas(context), id, ICON_SCALE)?.also { icons[id] = it }
        }

        private fun midnight(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            ACTION_MIDNIGHT.hashCode(),
            Intent(context, DayWidget::class.java).setAction(ACTION_MIDNIGHT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        /** An inexact alarm just after midnight. Needs no permission and wakes nothing early. */
        private fun scheduleMidnight(context: Context) {
            runCatching {
                val at = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault())
                    .plusMinutes(1).toInstant().toEpochMilli()
                context.getSystemService(AlarmManager::class.java)
                    ?.set(AlarmManager.RTC, at, midnight(context))
            }
        }

        /** True when at least one of these is actually on a home screen. */
        fun isInstalled(context: Context): Boolean = runCatching {
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, DayWidget::class.java))
                .isNotEmpty()
        }.getOrDefault(false)

        /** Whether the launcher will let us ask. Some do not, and then we never offer. */
        fun canRequestPin(context: Context): Boolean = runCatching {
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
        }.getOrDefault(false)

        /**
         * Ask the launcher to place one. This is a system dialog, not ours — the user can
         * refuse it, and the app is told nothing either way, which is why the in-app card
         * checks [isInstalled] rather than recording that it asked.
         */
        fun requestPin(context: Context) {
            runCatching {
                AppWidgetManager.getInstance(context).requestPinAppWidget(
                    ComponentName(context, DayWidget::class.java),
                    null,
                    null
                )
            }
        }

        /** Redraw every placed widget. Called from the repository on every state write. */
        fun refresh(context: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val component = ComponentName(context, DayWidget::class.java)
                val ids = manager.getAppWidgetIds(component)
                if (ids.isEmpty()) return
                context.sendBroadcast(
                    Intent(context, DayWidget::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                )
            }
        }
    }
}
