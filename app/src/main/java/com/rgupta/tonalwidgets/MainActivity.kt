package com.rgupta.tonalwidgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.BatteryManager
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Gallery screen: shows a live preview of every widget with an
 * "Add to home screen" button (uses the launcher's pin-widget dialog).
 */
class MainActivity : Activity() {

    private data class Entry(
        val name: Int,
        val layout: Int,
        val cls: Class<out BaseWidget>,
        val heightDp: Int,
        val widthFraction: Float = 1f,
    )

    private val entries = listOf(
        Entry(R.string.w_clock, R.layout.widget_clock, ClockWidget::class.java, 170),
        Entry(R.string.w_pill, R.layout.widget_pill_clock, PillClockWidget::class.java, 84),
        Entry(R.string.w_date, R.layout.widget_date, DateWidget::class.java, 170, 0.5f),
        Entry(R.string.w_battery, R.layout.widget_battery, BatteryWidget::class.java, 170, 0.5f),
        Entry(R.string.w_quote, R.layout.widget_quote, QuoteWidget::class.java, 170),
        Entry(R.string.w_search, R.layout.widget_search, SearchWidget::class.java, 72),
        Entry(R.string.w_quick, R.layout.widget_quick, QuickWidget::class.java, 88),
    )

    private val batteryPreviews = mutableListOf<View>()

    private fun dp(v: Int) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
        }

        list.addView(TextView(this).apply {
            setText(R.string.app_name)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            setTextColor(getColor(R.color.w_on_surface))
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        })
        list.addView(TextView(this).apply {
            setText(R.string.gallery_hint)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(getColor(R.color.w_on_surface))
            alpha = 0.75f
            setPadding(0, dp(6), 0, dp(8))
        })

        for (e in entries) {
            list.addView(TextView(this).apply {
                setText(e.name)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(getColor(R.color.w_accent))
                isAllCaps = true
                letterSpacing = 0.08f
                setPadding(0, dp(28), 0, dp(10))
            })

            val preview = layoutInflater.inflate(e.layout, list, false)
            val width = if (e.widthFraction < 1f)
                ((resources.displayMetrics.widthPixels - dp(40)) * e.widthFraction).toInt()
            else LinearLayout.LayoutParams.MATCH_PARENT
            preview.layoutParams = LinearLayout.LayoutParams(width, dp(e.heightDp))
            if (e.cls == BatteryWidget::class.java) batteryPreviews += preview
            if (e.cls == QuoteWidget::class.java) {
                preview.findViewById<TextView>(R.id.quote_text).text = Quotes.ALL[0].first
                preview.findViewById<TextView>(R.id.quote_author).text = "— ${Quotes.ALL[0].second}"
            }
            list.addView(preview)

            list.addView(Button(this).apply {
                setText(R.string.add_to_home)
                setOnClickListener { pin(e.cls) }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8); gravity = Gravity.START }
            })
        }

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            clipToPadding = false
            addView(list)
        })
    }

    override fun onResume() {
        super.onResume()
        BaseWidget.refreshAll(this)
        val bm = getSystemService(BatteryManager::class.java)
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
        for (v in batteryPreviews) {
            v.findViewById<ProgressBar>(R.id.battery_ring).progress = level
            v.findViewById<TextView>(R.id.battery_pct).text = "$level%"
            v.findViewById<TextView>(R.id.battery_label)
                .setText(if (bm.isCharging) R.string.charging else R.string.battery)
            v.findViewById<View>(R.id.battery_bolt).visibility =
                if (bm.isCharging) View.VISIBLE else View.GONE
        }
    }

    private fun pin(cls: Class<out BaseWidget>) {
        val manager = getSystemService(AppWidgetManager::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(ComponentName(this, cls), null, null)
        } else {
            Toast.makeText(this, R.string.pin_unsupported, Toast.LENGTH_LONG).show()
        }
    }
}
