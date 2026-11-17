package com.rgupta.tonalwidgets

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** Weather from Open-Meteo (free, no API key). Results are cached in SharedPreferences. */
object WeatherRepo {
    data class Place(val lat: Double, val lon: Double, val name: String)
    data class Now(
        val temp: Int, val code: Int, val isDay: Boolean,
        val hi: Int, val lo: Int, val place: String, val updated: Long,
    )

    private fun prefs(c: Context) = c.getSharedPreferences("weather", Context.MODE_PRIVATE)

    fun savePlace(c: Context, p: Place) {
        prefs(c).edit()
            .putString("lat", p.lat.toString())
            .putString("lon", p.lon.toString())
            .putString("place", p.name)
            .apply()
    }

    fun place(c: Context): Place? {
        val p = prefs(c)
        val lat = p.getString("lat", null)?.toDoubleOrNull() ?: return null
        val lon = p.getString("lon", null)?.toDoubleOrNull() ?: return null
        return Place(lat, lon, p.getString("place", "") ?: "")
    }

    fun cached(c: Context): Now? {
        val p = prefs(c)
        if (!p.contains("temp")) return null
        return Now(
            p.getInt("temp", 0), p.getInt("code", 0), p.getBoolean("day", true),
            p.getInt("hi", 0), p.getInt("lo", 0), p.getString("place", "") ?: "",
            p.getLong("updated", 0L),
        )
    }

    /** Fetches current weather for the saved place. Runs on a background thread. */
    fun fetch(c: Context): Boolean {
        val place = place(c) ?: return false
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=${place.lat}&longitude=${place.lon}" +
                "&current=temperature_2m,weather_code,is_day" +
                "&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=auto"
        )
        val json = JSONObject(get(url))
        val cur = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")
        prefs(c).edit()
            .putInt("temp", cur.getDouble("temperature_2m").roundToInt())
            .putInt("code", cur.getInt("weather_code"))
            .putBoolean("day", cur.optInt("is_day", 1) == 1)
            .putInt("hi", daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt())
            .putInt("lo", daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt())
            .putLong("updated", System.currentTimeMillis())
            .apply()
        return true
    }

    /** City name → coordinates. Runs on a background thread. */
    fun geocode(query: String): Place? {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val json = JSONObject(
            get(URL("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=1&language=en&format=json"))
        )
        val results = json.optJSONArray("results") ?: return null
        if (results.length() == 0) return null
        val r = results.getJSONObject(0)
        val region = r.optString("admin1", "")
        val name = if (region.isNotEmpty()) "${r.getString("name")}, $region" else r.getString("name")
        return Place(r.getDouble("latitude"), r.getDouble("longitude"), name)
    }

    private fun get(url: URL): String {
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /** WMO weather code → short description. */
    fun describe(code: Int): Int = when (code) {
        0 -> R.string.wx_clear
        1 -> R.string.wx_mostly_clear
        2 -> R.string.wx_partly_cloudy
        3 -> R.string.wx_cloudy
        45, 48 -> R.string.wx_fog
        51, 53, 55, 56, 57 -> R.string.wx_drizzle
        61, 63, 65, 66, 67, 80, 81, 82 -> R.string.wx_rain
        71, 73, 75, 77, 85, 86 -> R.string.wx_snow
        95, 96, 99 -> R.string.wx_storm
        else -> R.string.wx_cloudy
    }

    fun icon(code: Int, isDay: Boolean): Int = when (code) {
        0, 1 -> if (isDay) R.drawable.ic_wx_sun else R.drawable.ic_wx_moon
        2, 3 -> R.drawable.ic_wx_cloud
        45, 48 -> R.drawable.ic_wx_fog
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.ic_wx_rain
        71, 73, 75, 77, 85, 86 -> R.drawable.ic_wx_snow
        95, 96, 99 -> R.drawable.ic_wx_storm
        else -> R.drawable.ic_wx_cloud
    }

    fun refreshWidgets(c: Context) {
        BaseWidget.refresh(c, WeatherPillWidget::class.java)
        BaseWidget.refresh(c, WeatherBlobWidget::class.java)
        BaseWidget.refresh(c, MonoWeatherWidget::class.java)
    }
}

class WeatherWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val ok = runCatching { WeatherRepo.fetch(applicationContext) }.getOrDefault(false)
        WeatherRepo.refreshWidgets(applicationContext)
        return if (ok || WeatherRepo.place(applicationContext) == null) Result.success() else Result.retry()
    }

    companion object {
        private val NET = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "weather-refresh",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WeatherWorker>(30, TimeUnit.MINUTES).setConstraints(NET).build()
            )
        }

        fun now(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "weather-now",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WeatherWorker>().setConstraints(NET).build()
            )
        }
    }
}

private fun openApp(context: Context, code: Int) =
    Launch.activity(context, Intent(context, MainActivity::class.java), code)

private fun fillWeather(context: Context, v: RemoteViews, withPlace: Boolean) {
    val now = WeatherRepo.cached(context)
    if (now == null) {
        v.setTextViewText(R.id.wx_temp, "--°")
        v.setTextViewText(
            R.id.wx_desc,
            context.getString(if (WeatherRepo.place(context) == null) R.string.wx_set_place else R.string.wx_loading)
        )
        v.setImageViewResource(R.id.wx_icon, R.drawable.ic_wx_cloud)
        if (withPlace) v.setTextViewText(R.id.wx_hilo, "")
        return
    }
    v.setTextViewText(R.id.wx_temp, "${now.temp}°")
    v.setTextViewText(R.id.wx_desc, context.getString(WeatherRepo.describe(now.code)))
    v.setImageViewResource(R.id.wx_icon, WeatherRepo.icon(now.code, now.isDay))
    if (withPlace) {
        v.setTextViewText(R.id.wx_hilo, context.getString(R.string.wx_hilo, now.hi, now.lo))
        v.setTextViewText(R.id.wx_place, now.place.substringBefore(","))
    }
}

// ─── Weather pill (4×1) ─────────────────────────────────────────────────────
class WeatherPillWidget : BaseWidget() {
    override fun build(context: Context, widgetId: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_weather_pill).apply {
            fillWeather(context, this, withPlace = true)
            setOnClickPendingIntent(R.id.root, openApp(context, 600))
        }

    override fun onUpdate(context: Context, manager: android.appwidget.AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, manager, ids)
        WeatherWorker.schedule(context)
    }
}

// ─── Weather blob (2×2) ─────────────────────────────────────────────────────
class WeatherBlobWidget : BaseWidget() {
    override fun build(context: Context, widgetId: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_weather_blob).apply {
            fillWeather(context, this, withPlace = false)
            setOnClickPendingIntent(R.id.root, openApp(context, 610))
        }

    override fun onUpdate(context: Context, manager: android.appwidget.AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, manager, ids)
        WeatherWorker.schedule(context)
    }
}
