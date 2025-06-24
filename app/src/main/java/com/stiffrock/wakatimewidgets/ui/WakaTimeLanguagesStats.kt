package com.stiffrock.wakatimewidgets.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.stiffrock.wakatimewidgets.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Implementation of App Widget functionality.
 */
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
    // Get current time to show in widget
    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    val randomValue = String.format(Locale.getDefault(), "%.4f", Math.random())

    // Add this logging line
    Log.d("WidgetDebug", "Widget updated at $timestamp with value $randomValue")

    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.waka_time_languages_stats)
    views.setTextViewText(R.id.appwidget_text, randomValue)

    // Crear intent para abrir la actividad principal
    val intent = Intent(context, MainActivity::class.java)  // Cambia MainActivity por tu actividad principal
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    // Crear PendingIntent
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Asignar el PendingIntent al layout completo del widget
    views.setOnClickPendingIntent(R.id.langWidgetRoot, pendingIntent)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}