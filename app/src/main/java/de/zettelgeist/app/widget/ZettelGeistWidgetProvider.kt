package de.zettelgeist.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import de.zettelgeist.app.MainActivity
import de.zettelgeist.app.R
import de.zettelgeist.app.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ZettelGeistWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_QUICK_ADD) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_editor", true)
                putExtra("workspace", "inbox")
            }
            context.startActivity(launchIntent)
        }
    }

    companion object {
        const val ACTION_QUICK_ADD = "de.zettelgeist.app.widget.QUICK_ADD"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_zettelgeist)

            // Open app on widget tap
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // Quick add button
            val quickAddIntent = Intent(context, ZettelGeistWidgetProvider::class.java).apply {
                action = ACTION_QUICK_ADD
            }
            val quickAddPendingIntent = PendingIntent.getBroadcast(
                context, 1, quickAddIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_add, quickAddPendingIntent)

            // Load inbox count asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val count = db.noteDao().getCountByWorkspace("inbox").first()
                    views.setTextViewText(
                        R.id.widget_inbox_count,
                        if (count > 0) "$count Inbox" else "Inbox leer"
                    )

                    // Get latest note title
                    val notes = db.noteDao().getNotesByWorkspace("inbox").first()
                    val latestTitle = notes.firstOrNull()?.title ?: "Keine Notizen"
                    views.setTextViewText(R.id.widget_latest_note, latestTitle)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (_: Exception) {
                    views.setTextViewText(R.id.widget_inbox_count, "Inbox")
                    views.setTextViewText(R.id.widget_latest_note, "ZettelGeist")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }

            // Initial update (before async completes)
            views.setTextViewText(R.id.widget_inbox_count, "Inbox")
            views.setTextViewText(R.id.widget_latest_note, "ZettelGeist")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
