package com.rgupta.tonalwidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews

private fun clockIntent(context: Context): Intent = Launch.firstResolvable(
    context,
    Intent(AlarmClock.ACTION_SHOW_ALARMS),
    Intent(Settings.ACTION_DATE_SETTINGS),
)

// ─── Cookie clock (2×2): time inside a 12-point scallop ─────────────────────
class CookieClockWidget : BaseWidget() {
    override fun build(context: Context, widgetId: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_cookie_clock).apply {
            setOnClickPendingIntent(R.id.root, Launch.activity(context, clockIntent(context), 700))
        }
}

// ─── Stacked clock (2×2): hours over minutes ────────────────────────────────
class StackedClockWidget : BaseWidget() {
    override fun build(context: Context, widgetId: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_stacked_clock).apply {
            setOnClickPendingIntent(R.id.root, Launch.activity(context, clockIntent(context), 710))
        }
}

// ─── Liquid battery (2×2): the fill height is the charge ────────────────────
class LiquidBatteryWidget : BaseWidget() {
    override fun build(context: Context, widgetId: Int): RemoteViews {
        val bm = context.getSystemService(BatteryManager::class.java)
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
        val charging = bm.isCharging
        return RemoteViews(context.packageName, R.layout.widget_liquid_battery).apply {
            setInt(R.id.battery_fill, "setImageLevel", level * 100)
            setTextViewText(R.id.battery_pct, "$level%")
            setTextViewText(
                R.id.battery_label,
                context.getString(if (charging) R.string.charging else R.string.battery)
            )
            setViewVisibility(R.id.battery_bolt, if (charging) View.VISIBLE else View.GONE)
            setOnClickPendingIntent(
                R.id.root,
                Launch.activity(context, Intent(Intent.ACTION_POWER_USAGE_SUMMARY), 720)
            )
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, manager, ids)
        BatteryWorker.schedule(context)
    }

    override fun onDisabled(context: Context) {
        val m = AppWidgetManager.getInstance(context)
        if (m.getAppWidgetIds(ComponentName(context, BatteryWidget::class.java)).isEmpty()) {
            BatteryWorker.cancel(context)
        }
    }
}
