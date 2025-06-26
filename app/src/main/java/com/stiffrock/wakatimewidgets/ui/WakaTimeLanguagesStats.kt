package com.stiffrock.wakatimewidgets.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.stiffrock.wakatimewidgets.R
import com.stiffrock.wakatimewidgets.data.WakaTimeApi
import com.stiffrock.wakatimewidgets.data.model.Language
import com.stiffrock.wakatimewidgets.ui.MainActivity.Companion.API_KEY_PREF
import com.stiffrock.wakatimewidgets.ui.MainActivity.Companion.PREFS_FILENAME
import com.stiffrock.wakatimewidgets.utils.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val langTextViews = arrayOf(
    R.id.lang1,
    R.id.lang2,
    R.id.lang3,
    R.id.lang4,
    R.id.lang5,
    R.id.lang6,
    R.id.lang7,
    R.id.lang8,
    R.id.lang9,
    R.id.lang10,
)

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
    Log.d(TAG, "Updating widget")

    val sharedPreferences = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    val savedApiKey = sharedPreferences.getString(API_KEY_PREF, "") ?: ""

    // Create initial views with loading state
    val views = RemoteViews(context.packageName, R.layout.waka_time_languages_stats)

    // Set initial loading state if needed
    // views.setTextViewText(R.id.lang1, "Loading...")

    // Set up click intent
    setupClickIntent(context, views)

    // Update widget initially (can show loading state)
    appWidgetManager.updateAppWidget(appWidgetId, views)

    // Launch a coroutine to fetch data
    CoroutineScope(Dispatchers.IO).launch {
        val langStats = getLangStats(savedApiKey)

        // Update UI on the Main dispatcher
        withContext(Dispatchers.Main) {
            updateWidgetUI(appWidgetManager, appWidgetId, views, langStats)
        }
    }
}

// Separated UI update logic
private fun updateWidgetUI(
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    views: RemoteViews,
    langStats: Array<Language>
) {
    for (i in langStats.indices) {
        if (i >= langTextViews.size) break

        val tvId = langTextViews[i]
        val lang = langStats[i]
        val langName = lang.name
        val digitalTime = lang.digital
        val text = "$langName: $digitalTime"

        Log.d(TAG, "Updating textView #$i with <'$text'>")
        views.setTextViewText(tvId, text)
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
                langStats
            }
    } catch (e: Exception) {
        Log.e(TAG, "Error obtaining last 7 days lang stats: ${e.message}")
        fallbackLanguages
    }
}