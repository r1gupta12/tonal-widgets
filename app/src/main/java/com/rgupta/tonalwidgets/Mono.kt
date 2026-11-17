package com.rgupta.tonalwidgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.LocalTime

/** Background-free "editorial" widgets: bold type straight on the wallpaper. */

fun greeting(context: Context, hour: Int = LocalTime.now().hour): String = context.getString(
    when (hour) {
        in 5..11 -> R.string.greet_morning
        in 12..16 -> R.string.greet_afternoon
        in 17..20 -> R.string.greet_evening
        else -> R.string.greet_night
    }
)

// ─── Mono date (2×2): greeting + giant day number ───────────────────────────
class MonoDateWidget : BaseWidget() {
    override fun build(context: Context, widgetId: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_mono_date).apply {
            setTextViewText(R.id.mono_greeting, greeting(context))
            val calendar = Launch.firstResolvable(
                context,
                Intent(Intent.ACTION_VIEW, android.provider.CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()),
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR),
            )
            setOnClickPendingIntent(R.id.root, Launch.activity(context, calendar, 800))
        }
}

// ─── Mono weather (2×2): icon + text, no background ─────────────────────────
class MonoWeatherWidget : BaseWidget() {
    override fun build(context: Context, widgetId: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_mono_weather).apply {
            val now = WeatherRepo.cached(context)
            if (now == null) {
                setTextViewText(R.id.wx_temp, "--°C")
                setTextViewText(
                    R.id.wx_desc,
                    context.getString(if (WeatherRepo.place(context) == null) R.string.wx_set_place else R.string.wx_loading)
                )
                setTextViewText(R.id.wx_place, "")
            } else {
                val desc = context.getString(WeatherRepo.describe(now.code))
                setTextViewText(R.id.wx_temp, "${now.temp}°C")
                setTextViewText(R.id.wx_desc, if (now.isDay) desc else "$desc · Night")
                setImageViewResource(R.id.wx_icon, WeatherRepo.icon(now.code, now.isDay))
                setTextViewText(
                    R.id.wx_place,
                    context.getString(R.string.in_your_city, now.place.substringBefore(","))
                )
            }
            setOnClickPendingIntent(
                R.id.root,
                Launch.activity(context, Intent(context, MainActivity::class.java), 810)
            )
        }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, manager, ids)
        WeatherWorker.schedule(context)
    }
}
