package com.rgupta.tonalwidgets

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.util.concurrent.TimeUnit

private fun clockAppIntent(context: Context): Intent = Launch.firstResolvable(
    context,
    Intent(AlarmClock.ACTION_SHOW_ALARMS),
    Intent(Settings.ACTION_DATE_SETTINGS),
)

private fun calendarIntent(context: Context): Intent = Launch.firstResolvable(
    context,
    Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()),
    Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR),
)

// ─── Big clock (4×2) ────────────────────────────────────────────────────────
class ClockWidget : BaseWidget() {
    override fun build(context: Context, widgetId: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_clock).apply {
            setOnClickPendingIntent(R.id.root, Launch.activity(context, clockAppIntent(context), 100))
        }
}

// ─── Pill clock (4×1) ───────────────────────────────────────────────────────
class PillClockWidget : BaseWidget() {
    override fun build(context: Context, widgetId: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_pill_clock).apply {
            setOnClickPendingIntent(R.id.root, Launch.activity(context, clockAppIntent(context), 110))
            setOnClickPendingIntent(R.id.pill_day, Launch.activity(context, calendarIntent(context), 111))
        }
}

// ─── Date tile (2×2) ────────────────────────────────────────────────────────
class DateWidget : BaseWidget() {
    override fun build(context: Context, widgetId: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_date).apply {
            setOnClickPendingIntent(R.id.root, Launch.activity(context, calendarIntent(context), 120))
        }
}

// ─── Battery ring (2×2) ─────────────────────────────────────────────────────
class BatteryWidget : BaseWidget() {
    override fun build(context: Context, widgetId: Int): RemoteViews {
        val bm = context.getSystemService(BatteryManager::class.java)
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
        val charging = bm.isCharging
        return RemoteViews(context.packageName, R.layout.widget_battery).apply {
            setProgressBar(R.id.battery_ring, 100, level, false)
            setTextViewText(R.id.battery_pct, "$level%")
            setTextViewText(
                R.id.battery_label,
                context.getString(if (charging) R.string.charging else R.string.battery)
            )
            setViewVisibility(R.id.battery_bolt, if (charging) View.VISIBLE else View.GONE)
            setOnClickPendingIntent(
                R.id.root,
                Launch.activity(context, Intent(Intent.ACTION_POWER_USAGE_SUMMARY), 200)
            )
        }
    }

    override fun onUpdate(
        context: Context,
        manager: android.appwidget.AppWidgetManager,
        ids: IntArray
    ) {
        super.onUpdate(context, manager, ids)
        BatteryWorker.schedule(context)
    }

    override fun onDisabled(context: Context) {
        BatteryWorker.cancel(context)
    }
}

/** Refreshes the battery widget every 15 minutes (the shortest interval Android allows). */
class BatteryWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        BaseWidget.refresh(applicationContext, BatteryWidget::class.java)
        return Result.success()
    }

    companion object {
        private const val NAME = "battery-refresh"

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<BatteryWorker>(15, TimeUnit.MINUTES).build()
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}

// ─── Quote of the day (4×2) — tap for the next one ──────────────────────────
class QuoteWidget : BaseWidget() {
    override fun build(context: Context, widgetId: Int): RemoteViews {
        val offset = prefs(context).getInt(KEY_OFFSET, 0)
        val day = LocalDate.now().toEpochDay().toInt()
        val (text, author) = Quotes.ALL[Math.floorMod(day + offset, Quotes.ALL.size)]
        return RemoteViews(context.packageName, R.layout.widget_quote).apply {
            setTextViewText(R.id.quote_text, text)
            setTextViewText(R.id.quote_author, "— $author")
            setOnClickPendingIntent(
                R.id.root,
                Launch.broadcast(context, QuoteWidget::class.java, ACTION_NEXT, 300)
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_NEXT) {
            val p = prefs(context)
            p.edit().putInt(KEY_OFFSET, p.getInt(KEY_OFFSET, 0) + 1).apply()
            refresh(context, QuoteWidget::class.java)
        }
    }

    companion object {
        const val ACTION_NEXT = "com.rgupta.tonalwidgets.NEXT_QUOTE"
        private const val KEY_OFFSET = "offset"
        private fun prefs(c: Context) = c.getSharedPreferences("quotes", Context.MODE_PRIVATE)
    }
}

// ─── Search bar (4×1) ───────────────────────────────────────────────────────
class SearchWidget : BaseWidget() {
    override fun build(context: Context, widgetId: Int): RemoteViews {
        val google = context.packageManager
            .getLaunchIntentForPackage("com.google.android.googlequicksearchbox")
        val search = google ?: Launch.firstResolvable(
            context,
            Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", ""),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")),
        )
        val voice = Launch.firstResolvable(
            context,
            Intent("android.intent.action.VOICE_ASSIST"),
            search,
        )
        return RemoteViews(context.packageName, R.layout.widget_search).apply {
            setOnClickPendingIntent(R.id.root, Launch.activity(context, search, 400))
            setOnClickPendingIntent(R.id.search_mic, Launch.activity(context, voice, 401))
        }
    }
}

// ─── Quick actions (4×1): phone, camera, maps, flashlight ───────────────────
class QuickWidget : BaseWidget() {
    override fun build(context: Context, widgetId: Int): RemoteViews {
        val torchOn = prefs(context).getBoolean(KEY_TORCH, false)
        val camera = Launch.firstResolvable(
            context,
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        )
        return RemoteViews(context.packageName, R.layout.widget_quick).apply {
            setOnClickPendingIntent(R.id.q_phone, Launch.activity(context, Intent(Intent.ACTION_DIAL), 500))
            setOnClickPendingIntent(R.id.q_camera, Launch.activity(context, camera, 501))
            setOnClickPendingIntent(
                R.id.q_maps,
                Launch.activity(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")), 502)
            )
            setOnClickPendingIntent(
                R.id.q_torch,
                Launch.broadcast(context, QuickWidget::class.java, ACTION_TORCH, 503)
            )
            setInt(
                R.id.q_torch, "setBackgroundResource",
                if (torchOn) R.drawable.shape_squircle_accent else R.drawable.shape_squircle_track
            )
            setInt(
                R.id.q_torch, "setColorFilter",
                context.getColor(if (torchOn) R.color.w_on_accent else R.color.w_on_bg)
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TORCH) {
            toggleTorch(context)
            refresh(context, QuickWidget::class.java)
        }
    }

    private fun toggleTorch(context: Context) {
        val cm = context.getSystemService(CameraManager::class.java) ?: return
        val camId = runCatching {
            cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull() ?: return
        val p = prefs(context)
        val turnOn = !p.getBoolean(KEY_TORCH, false)
        runCatching { cm.setTorchMode(camId, turnOn) }
            .onSuccess { p.edit().putBoolean(KEY_TORCH, turnOn).apply() }
            .onFailure { p.edit().putBoolean(KEY_TORCH, false).apply() }
    }

    companion object {
        const val ACTION_TORCH = "com.rgupta.tonalwidgets.TOGGLE_TORCH"
        private const val KEY_TORCH = "torch"
        private fun prefs(c: Context) = c.getSharedPreferences("quick", Context.MODE_PRIVATE)
    }
}
