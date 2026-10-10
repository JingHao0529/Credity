package com.example.credity

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Locale

class CredityWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences("credity_prefs", Context.MODE_PRIVATE)
            val income = prefs.getFloat("incomeRM", 5000.0f).toDouble()
            val expenses = prefs.getFloat("totalExpensesRM", 2350.0f).toDouble()
            val spent = prefs.getFloat("totalSpentThisMonthRM", 0.0f).toDouble()

            val scoreResult = DebtScoreCalculator.calculateScore(
                incomeRM = income,
                fixedExpensesRM = expenses,
                spentThisMonthRM = spent
            )

            // Penny the Penguin Mascot
            val penguinEmoji = "🐧"

            val views = RemoteViews(context.packageName, R.layout.credity_widget_layout)

            views.setTextViewText(R.id.widget_title, "Debt score")
            views.setTextViewText(R.id.widget_status, scoreResult.statusLabel)
            views.setTextViewText(R.id.widget_score, scoreResult.score.toString())
            views.setTextViewText(R.id.widget_cat_avatar, penguinEmoji)
            views.setTextViewText(
                R.id.widget_allowance,
                String.format(Locale.US, "RM %,d", scoreResult.dailyAllowanceRM.toInt())
            )

            // Accent text colors based on tier
            views.setTextColor(R.id.widget_title, scoreResult.textHex.toInt())
            views.setTextColor(R.id.widget_score, scoreResult.textHex.toInt())

            // Set Background Color based on Score Tier
            views.setInt(R.id.widget_container, "setBackgroundColor", scoreResult.colorHex.toInt())

            // Clicking widget launches MainActivity directly into AI Chatbot screen
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("extra_open_screen", "AI_CHAT")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CredityWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}
