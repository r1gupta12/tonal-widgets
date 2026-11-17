package com.rgupta.tonalwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Every widget just builds a RemoteViews; this handles the update plumbing. */
abstract class BaseWidget : AppWidgetProvider() {

    abstract fun build(context: Context, widgetId: Int): RemoteViews

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, build(context, id))
    }

    companion object {
        fun refresh(context: Context, cls: Class<out BaseWidget>) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, cls))
            if (ids.isEmpty()) return
            val provider = cls.getDeclaredConstructor().newInstance()
            for (id in ids) manager.updateAppWidget(id, provider.build(context, id))
        }

        val ALL: List<Class<out BaseWidget>> = listOf(
            ClockWidget::class.java,
            PillClockWidget::class.java,
            DateWidget::class.java,
            BatteryWidget::class.java,
            QuoteWidget::class.java,
            SearchWidget::class.java,
            QuickWidget::class.java,
        )

        fun refreshAll(context: Context) = ALL.forEach { refresh(context, it) }
    }
}

/** Helpers for the PendingIntents behind each tap. */
object Launch {
    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    fun activity(context: Context, intent: Intent, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context, requestCode,
            Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), FLAGS
        )

    fun broadcast(context: Context, cls: Class<*>, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(context, requestCode, Intent(context, cls).setAction(action), FLAGS)

    /** First intent some installed app can handle; falls back to the last one. */
    fun firstResolvable(context: Context, vararg intents: Intent): Intent =
        intents.firstOrNull { it.resolveActivity(context.packageManager) != null } ?: intents.last()
}
