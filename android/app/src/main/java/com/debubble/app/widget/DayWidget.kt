package com.debubble.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.debubble.app.MainActivity
import com.debubble.app.R
import com.debubble.app.data.Repository
import com.debubble.app.engine.Curriculum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The home-screen widget.
 *
 * This is the one piece of the product that reaches someone who has not opened the app, and
 * it is deliberately not a progress display. Two findings point the same way. Gollwitzer's
 * if-then plans work because the cue retrieves the response without a fresh decision — but
 * only if the plan is actually in mind when the cue arrives, and a plan filed inside an app
 * is not. And retrieval cues are on Craske's list of things that make new learning stick
 * outside the session it was built in. So the largest text on the widget is the *when*, and
 * the challenge title is secondary.
 *
 * It is also why there is no "mark it done" button. Completing a challenge runs through a
 * celebration and, where one was recorded, a prediction to check against — the two parts that
 * do the work. A one-tap tick on the home screen would let someone skip both and collect the
 * number instead, which is the shape of exactly the habit this app is not for.
 */
class DayWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        render(context, manager, appWidgetIds)
    }

    /**
     * The date rolling over changes what the widget should say without anything in the app
     * happening, so the receiver listens for it directly. Without this, a widget on a phone
     * whose owner has not opened DeBubble since yesterday shows yesterday's cue all morning.
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> refresh(context)
        }
    }

    private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val app = context.applicationContext
        // goAsync() would be the textbook answer, but AppWidgetProvider's pending result is
        // single-use and onUpdate can be re-entered; the update itself is idempotent and the
        // process stays alive long enough for a DataStore read, so a plain scope is safer
        // here than a result that might already have been finished.
        CoroutineScope(Dispatchers.IO).launch {
            val content = runCatching {
                WidgetCues.of(
                    state = Repository(app).state.first(),
                    today = LocalDate.now().toEpochDay(),
                    curriculum = curriculum(app)
                )
            }.getOrElse { WidgetCues.unavailable }
            ids.forEach { id -> manager.updateAppWidget(id, views(app, content)) }
        }
    }

    // ------------------------------------------------------------------ drawing

    private fun colourOf(tone: WidgetCue.Tone): Int = when (tone) {
        WidgetCue.Tone.ACCESS -> R.color.ink_access
        WidgetCue.Tone.ACTIVITY -> R.color.ink_activity
        WidgetCue.Tone.SOCIAL -> R.color.ink_social
        // A plan is a commitment the user made, so it gets the colour the app uses for
        // things they have already decided. Finishing the day gets the unlock colour.
        WidgetCue.Tone.PLAN -> R.color.ink_activity
        WidgetCue.Tone.DONE -> R.color.ink_gold
        WidgetCue.Tone.NEUTRAL -> R.color.ink_muted
    }

    private fun views(context: Context, c: WidgetCue): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.widget_day)
        v.setTextViewText(R.id.widget_kicker, c.kicker)
        v.setTextColor(R.id.widget_kicker, context.getColor(colourOf(c.tone)))
        v.setTextViewText(R.id.widget_cue, c.cue)
        v.setTextViewText(R.id.widget_body, c.body)
        v.setTextViewText(R.id.widget_badge, c.badge)
        v.setTextViewText(R.id.widget_action, context.getString(R.string.widget_action_default))
        v.setViewVisibility(
            R.id.widget_second,
            if (c.review) View.VISIBLE else View.GONE
        )

        // Tapping anywhere opens the app. The buttons are the obvious targets, but a widget
        // whose text is not tappable reads as a notification someone forgot to dismiss.
        val open = deepLink(context, EXTRA_OPEN_CHALLENGE, c.link)
        v.setOnClickPendingIntent(R.id.widget_root, open)
        v.setOnClickPendingIntent(R.id.widget_action, open)
        v.setOnClickPendingIntent(
            R.id.widget_second,
            deepLink(context, EXTRA_OPEN_REVIEW, "1")
        )
        return v
    }

    private fun deepLink(context: Context, extra: String, value: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (value.isNotEmpty()) putExtra(extra, value)
        }
        return PendingIntent.getActivity(
            context,
            // One request code per destination, or the second intent silently reuses the
            // first one's extras.
            extra.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_OPEN_CHALLENGE = "open_challenge"
        const val EXTRA_OPEN_REVIEW = "open_review"

        /** Parsed once per process. The three ladders are ~190 KB of JSON. */
        @Volatile
        private var cached: Curriculum? = null

        private fun curriculum(context: Context): Curriculum =
            cached ?: synchronized(this) {
                cached ?: Repository(context).loadCurriculum().also { cached = it }
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
