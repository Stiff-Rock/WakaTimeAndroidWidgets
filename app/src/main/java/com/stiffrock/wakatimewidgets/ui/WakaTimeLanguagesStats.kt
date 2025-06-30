package com.stiffrock.wakatimewidgets.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import com.stiffrock.wakatimewidgets.R
import com.stiffrock.wakatimewidgets.data.LanguageService
import com.stiffrock.wakatimewidgets.data.WakaTimeApi
import com.stiffrock.wakatimewidgets.data.model.Language
import com.stiffrock.wakatimewidgets.ui.MainActivity.Companion.API_KEY_PREF
import com.stiffrock.wakatimewidgets.ui.MainActivity.Companion.PREFS_FILENAME
import com.stiffrock.wakatimewidgets.utils.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.yaml.snakeyaml.Yaml

// IDs for language labels
private val langTextViews = arrayOf(
    R.id.language_label_1,
    R.id.language_label_2,
    R.id.language_label_3,
    R.id.language_label_4,
    R.id.language_label_5,
)

// IDs for bar segments
private val barSegments = arrayOf(
    R.id.bar_segment_1,
    R.id.bar_segment_2,
    R.id.bar_segment_3,
    R.id.bar_segment_4,
    R.id.bar_segment_5,
)

// IDs for color dots
private val dotIndicators = arrayOf(
    R.id.dot_1,
    R.id.dot_2,
    R.id.dot_3,
    R.id.dot_4,
    R.id.dot_5,
)

private var colorMap: Map<String, String>? = null

class WakaTimeLanguagesStats : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val sharedPreferences = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    val savedApiKey = sharedPreferences.getString(API_KEY_PREF, "") ?: ""

    // Create initial views with loading state
    val views = RemoteViews(context.packageName, R.layout.waka_time_languages_stats)

    // Set up click intent
    setupClickIntent(context, views)

    // Update widget initially (can show loading state)
    appWidgetManager.updateAppWidget(appWidgetId, views)

    // Launch a coroutine to fetch data
    CoroutineScope(Dispatchers.IO).launch {
        val langStats = getLangStats(savedApiKey)
        val username = getUsername(savedApiKey)

        // Update UI on the Main dispatcher
        withContext(Dispatchers.Main) {
            uiWidgetUpdate(appWidgetManager, appWidgetId, views, username, langStats)
        }
    }
}

// Separated UI update logic
private fun uiWidgetUpdate(
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    views: RemoteViews,
    username: String,
    langStats: Array<Language>
) {
    val title = "Language Stats - $username"
    views.setTextViewText(R.id.widget_title, title)

    for (i in langStats.indices) {
        if (i >= langTextViews.size) break

        val tvId = langTextViews[i]
        val lang = langStats[i]
        val langName = lang.name
        val timeText = lang.text
        val percentText = lang.percent
        val text = "$langName - $timeText ($percentText%)"

        views.setTextViewText(tvId, text)

        Log.w(TAG, "PRE: ${lang.name}: ${lang.color}")

        val colorValue = if (lang.color.isNullOrBlank()) "#E0E0E0" else lang.color
        lang.color = colorValue

        Log.w(TAG, "POST: ${lang.name}: ${lang.color}")

        val color = Color.parseColor(lang.color)

        val dotId = dotIndicators[i]
        views.setInt(dotId, "setBackgroundColor", color)

        val barSegId = barSegments[i]
        views.setInt(barSegId, "setBackgroundColor", color)
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private fun setupClickIntent(context: Context, views: RemoteViews) {
    val intent = Intent(context, MainActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.langWidgetRoot, pendingIntent)
}

private suspend fun getUsername(apiKey: String): String {
    return try {
        val response = WakaTimeApi.service.getCurrentUser(apiKey)

        if (!response.isSuccessful) {
            Log.w(TAG, "API Error: ${response.code()} - ${response.errorBody()}")
            return "Unknown"
        }

        val username = response.body()?.data?.username
        if (username.isNullOrEmpty()) {
            Log.w(TAG, "Failed to get username")
            "Unknown"
        } else {
            username
        }
    } catch (e: Exception) {
        Log.w(TAG, "Error obtaining username: ${e.message}")
        "Unknown"
    }
}

private suspend fun getLangStats(apiKey: String): Array<Language> {
    Log.d(TAG, "Obtaining lang stats")

    val fallbackLanguages = arrayOf(
        Language("No data", 0.0, 0.0, "0:00", "0.00", "0 mins", 0, 0)
    )

    if (apiKey.isEmpty()) {
        Log.w(
            TAG,
            "No API key provided"
        )
        return fallbackLanguages
    }

    return try {
        val response = WakaTimeApi.service.getLangStats(apiKey)
        if (!response.isSuccessful) {
            Log.e(
                TAG,
                "API response error with code '${response.code()}': ${response.errorBody()}"
            )
        }
        val body = response.body()

        val langStats = body?.data?.languages
        if (langStats.isNullOrEmpty()) {
            Log.w(TAG, "NO DATA: API returned null or empty language stats")
            fallbackLanguages
        } else {
            if (colorMap == null) {
                getLangColorMap()
            }

            langStats.forEach { language ->
                colorMap?.get(language.name)?.let { color ->
                    language.color = color
                }
            }

            langStats
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error obtaining last 7 days lang stats: ${e.message}")
        fallbackLanguages
    }
}

suspend fun getLangColorMap() {
    try {
        val yamlString = LanguageService.githubService.getLanguagesYaml()
        val yaml = Yaml()

        // Parse the YAML string into a nested map structure
        @Suppress("UNCHECKED_CAST")
        val languagesData = yaml.load<Map<String, Any>>(yamlString) as Map<String, Map<String, Any>>

        // Extract just the language names and their color values
        val newColorMap = mutableMapOf<String, String>()

        for ((languageName, languageProps) in languagesData) {
            // The "color" property contains the hex color code
            var color = languageProps["color"] as? String

            if (color == null) {
                color = "#E0E0E0"
            }

            newColorMap[languageName] = color
        }

        // Assign to our global variable
        colorMap = newColorMap
        Log.d(TAG, "Loaded colors for ${newColorMap.size} languages")
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching or parsing language colors: ${e.message}")
        // Set an empty map as fallback
        colorMap = mapOf()
    }
}