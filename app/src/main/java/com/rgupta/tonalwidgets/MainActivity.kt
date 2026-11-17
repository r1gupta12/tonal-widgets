package com.rgupta.tonalwidgets

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Setup screen: weather location, one-tap wallpapers, and a gallery of every
 * widget with an "Add to home screen" button.
 */
class MainActivity : Activity() {

    private data class Entry(
        val name: Int,
        val layout: Int,
        val cls: Class<out BaseWidget>,
        val heightDp: Int,
        val half: Boolean = false,
    )

    private val entries = listOf(
        Entry(R.string.w_mono_date, R.layout.widget_mono_date, MonoDateWidget::class.java, 230, true),
        Entry(R.string.w_mono_weather, R.layout.widget_mono_weather, MonoWeatherWidget::class.java, 230, true),
        Entry(R.string.w_weather_pill, R.layout.widget_weather_pill, WeatherPillWidget::class.java, 84),
        Entry(R.string.w_weather_blob, R.layout.widget_weather_blob, WeatherBlobWidget::class.java, 170, true),
        Entry(R.string.w_cookie, R.layout.widget_cookie_clock, CookieClockWidget::class.java, 170, true),
        Entry(R.string.w_stacked, R.layout.widget_stacked_clock, StackedClockWidget::class.java, 170, true),
        Entry(R.string.w_liquid, R.layout.widget_liquid_battery, LiquidBatteryWidget::class.java, 170, true),
        Entry(R.string.w_clock, R.layout.widget_clock, ClockWidget::class.java, 170),
        Entry(R.string.w_pill, R.layout.widget_pill_clock, PillClockWidget::class.java, 84),
        Entry(R.string.w_date, R.layout.widget_date, DateWidget::class.java, 170, true),
        Entry(R.string.w_battery, R.layout.widget_battery, BatteryWidget::class.java, 170, true),
        Entry(R.string.w_quote, R.layout.widget_quote, QuoteWidget::class.java, 170),
        Entry(R.string.w_search, R.layout.widget_search, SearchWidget::class.java, 72),
        Entry(R.string.w_quick, R.layout.widget_quick, QuickWidget::class.java, 88),
    )

    private val previews = mutableListOf<Pair<Class<out BaseWidget>, View>>()
    private lateinit var placeText: TextView

    private fun dp(v: Int) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun heading(text: Int) = TextView(this).apply {
        setText(text)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        setTextColor(getColor(R.color.w_on_surface))
        typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
        setPadding(0, dp(32), 0, dp(10))
    }

    private fun label(text: Int) = TextView(this).apply {
        setText(text)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setTextColor(getColor(R.color.w_accent))
        isAllCaps = true
        letterSpacing = 0.08f
        setPadding(0, dp(22), 0, dp(10))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(40))
        }

        list.addView(TextView(this).apply {
            setText(R.string.app_name)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 36f)
            setTextColor(getColor(R.color.w_on_surface))
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            letterSpacing = -0.02f
        })
        list.addView(TextView(this).apply {
            setText(R.string.gallery_hint)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(getColor(R.color.w_on_surface))
            alpha = 0.75f
            setPadding(0, dp(6), 0, 0)
        })

        // ── Weather location ─────────────────────────────────────────────
        list.addView(heading(R.string.h_weather))
        placeText = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(getColor(R.color.w_on_surface))
        }
        list.addView(placeText)
        val city = EditText(this).apply {
            setHint(R.string.city_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            isSingleLine = true
        }
        val setCity = Button(this).apply { setText(R.string.set_city) }
        val cityRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(city, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(setCity)
        }
        list.addView(cityRow)
        val search = { searchCity(city.text.toString()) }
        setCity.setOnClickListener { search() }
        city.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEARCH) { search(); true } else false
        }
        list.addView(Button(this).apply {
            setText(R.string.use_location)
            setOnClickListener { useMyLocation() }
        }, wrap())

        // ── Wallpapers ───────────────────────────────────────────────────
        list.addView(heading(R.string.h_wallpapers))
        list.addView(TextView(this).apply {
            setText(R.string.wallpaper_hint)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(getColor(R.color.w_on_surface))
            alpha = 0.75f
            setPadding(0, 0, 0, dp(12))
        })
        val wallRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (style in Wallpapers.ALL) {
            val img = ImageView(this).apply {
                setImageBitmap(Wallpapers.render(style, dp(108), dp(220)))
                background = GradientDrawable().apply { cornerRadius = dp(20).toFloat() }
                clipToOutline = true
                contentDescription = style.name
            }
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, 0, dp(12), 0)
                addView(img, LinearLayout.LayoutParams(dp(108), dp(220)))
                addView(Button(this@MainActivity).apply {
                    text = getString(R.string.set_wallpaper, style.name)
                    setOnClickListener { setWallpaper(style) }
                }, wrap())
            }
            wallRow.addView(card)
        }
        list.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(wallRow)
        })

        // ── Widget gallery ───────────────────────────────────────────────
        list.addView(heading(R.string.h_widgets))
        val full = resources.displayMetrics.widthPixels - dp(40)
        for (e in entries) {
            list.addView(label(e.name))
            val preview = layoutInflater.inflate(e.layout, list, false)
            preview.layoutParams = LinearLayout.LayoutParams(
                if (e.half) (full * 0.5f).toInt() else LinearLayout.LayoutParams.MATCH_PARENT,
                dp(e.heightDp)
            )
            previews += e.cls to preview
            list.addView(preview)
            list.addView(Button(this).apply {
                setText(R.string.add_to_home)
                setOnClickListener { pin(e.cls) }
            }, wrap().apply { topMargin = dp(8) })
        }

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            clipToPadding = false
            addView(list)
        })
    }

    private fun wrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
    )

    override fun onResume() {
        super.onResume()
        BaseWidget.refreshAll(this)
        WeatherWorker.schedule(this)
        val cached = WeatherRepo.cached(this)
        if (WeatherRepo.place(this) != null &&
            (cached == null || System.currentTimeMillis() - cached.updated > 30 * 60_000L)
        ) {
            WeatherWorker.now(this)
        }
        updatePreviews()
    }

    private fun updatePreviews() {
        val place = WeatherRepo.place(this)
        placeText.text = if (place == null) getString(R.string.no_place)
        else getString(R.string.current_place, place.name)

        val bm = getSystemService(BatteryManager::class.java)
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
        val charging = bm.isCharging
        val wx = WeatherRepo.cached(this)

        for ((cls, v) in previews) {
            when (cls) {
                BatteryWidget::class.java -> {
                    v.findViewById<ProgressBar>(R.id.battery_ring).progress = level
                    batteryText(v, level, charging)
                }
                LiquidBatteryWidget::class.java -> {
                    v.findViewById<ImageView>(R.id.battery_fill).setImageLevel(level * 100)
                    batteryText(v, level, charging)
                }
                QuoteWidget::class.java -> {
                    v.findViewById<TextView>(R.id.quote_text).text = Quotes.ALL[0].first
                    v.findViewById<TextView>(R.id.quote_author).text = "— ${Quotes.ALL[0].second}"
                }
                MonoDateWidget::class.java ->
                    v.findViewById<TextView>(R.id.mono_greeting).text = greeting(this)
                MonoWeatherWidget::class.java -> if (wx != null) {
                    v.findViewById<TextView>(R.id.wx_temp).text = "${wx.temp}°C"
                    v.findViewById<TextView>(R.id.wx_desc).setText(WeatherRepo.describe(wx.code))
                    v.findViewById<ImageView>(R.id.wx_icon).setImageResource(WeatherRepo.icon(wx.code, wx.isDay))
                    v.findViewById<TextView>(R.id.wx_place).text =
                        getString(R.string.in_your_city, wx.place.substringBefore(","))
                }
                WeatherPillWidget::class.java, WeatherBlobWidget::class.java -> if (wx != null) {
                    v.findViewById<TextView>(R.id.wx_temp).text = "${wx.temp}°"
                    v.findViewById<TextView>(R.id.wx_desc).setText(WeatherRepo.describe(wx.code))
                    v.findViewById<ImageView>(R.id.wx_icon).setImageResource(WeatherRepo.icon(wx.code, wx.isDay))
                    v.findViewById<TextView>(R.id.wx_hilo)?.text = getString(R.string.wx_hilo, wx.hi, wx.lo)
                    v.findViewById<TextView>(R.id.wx_place)?.text = wx.place.substringBefore(",")
                }
                else -> Unit
            }
        }
    }

    private fun batteryText(v: View, level: Int, charging: Boolean) {
        v.findViewById<TextView>(R.id.battery_pct).text = "$level%"
        v.findViewById<TextView>(R.id.battery_label).setText(if (charging) R.string.charging else R.string.battery)
        v.findViewById<View>(R.id.battery_bolt).visibility = if (charging) View.VISIBLE else View.GONE
    }

    // ── Weather location ────────────────────────────────────────────────
    private fun searchCity(query: String) {
        if (query.isBlank()) return
        Toast.makeText(this, R.string.searching, Toast.LENGTH_SHORT).show()
        Thread {
            val place = runCatching { WeatherRepo.geocode(query) }.getOrNull()
            runOnUiThread {
                if (place == null) {
                    Toast.makeText(this, R.string.city_not_found, Toast.LENGTH_LONG).show()
                } else {
                    savePlace(place)
                }
            }
        }.start()
    }

    private fun savePlace(place: WeatherRepo.Place) {
        WeatherRepo.savePlace(this, place)
        WeatherWorker.now(this)
        updatePreviews()
        Toast.makeText(this, getString(R.string.place_saved, place.name), Toast.LENGTH_SHORT).show()
        // Fetch right away so the preview fills in quickly.
        Thread {
            runCatching { WeatherRepo.fetch(this) }
            WeatherRepo.refreshWidgets(this)
            runOnUiThread { updatePreviews() }
        }.start()
    }

    private fun useMyLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQ_LOCATION)
            return
        }
        locate()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) locate()
            else Toast.makeText(this, R.string.location_denied, Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun locate() {
        val lm = getSystemService(LocationManager::class.java)
        val provider = listOf(
            LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER
        ).firstOrNull { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        if (provider == null) {
            Toast.makeText(this, R.string.location_off, Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, R.string.locating, Toast.LENGTH_SHORT).show()
        lm.getCurrentLocation(provider, null, mainExecutor) { loc: Location? ->
            val l = loc ?: lm.getLastKnownLocation(provider)
            if (l == null) {
                Toast.makeText(this, R.string.location_failed, Toast.LENGTH_LONG).show()
            } else {
                Thread {
                    val name = cityName(l.latitude, l.longitude) ?: getString(R.string.my_location)
                    runOnUiThread { savePlace(WeatherRepo.Place(l.latitude, l.longitude, name)) }
                }.start()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun cityName(lat: Double, lon: Double): String? = runCatching {
        Geocoder(this).getFromLocation(lat, lon, 1)?.firstOrNull()?.let { a ->
            listOfNotNull(a.locality ?: a.subAdminArea, a.adminArea).joinToString(", ").ifEmpty { null }
        }
    }.getOrNull()

    // ── Wallpapers ──────────────────────────────────────────────────────
    private fun setWallpaper(style: Wallpapers.Style) {
        val bounds = windowManager.currentWindowMetrics.bounds
        Toast.makeText(this, getString(R.string.setting_wallpaper, style.name), Toast.LENGTH_SHORT).show()
        Thread {
            val ok = runCatching { Wallpapers.apply(this, style, bounds.width(), bounds.height()) }.isSuccess
            runOnUiThread {
                Toast.makeText(
                    this, if (ok) R.string.wallpaper_done else R.string.wallpaper_failed, Toast.LENGTH_LONG
                ).show()
            }
        }.start()
    }

    private fun pin(cls: Class<out BaseWidget>) {
        val manager = getSystemService(AppWidgetManager::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(ComponentName(this, cls), null, null)
        } else {
            Toast.makeText(this, R.string.pin_unsupported, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val REQ_LOCATION = 42
    }
}
