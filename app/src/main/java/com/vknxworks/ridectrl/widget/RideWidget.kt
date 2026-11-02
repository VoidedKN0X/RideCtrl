package com.vknxworks.ridectrl.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.vknxworks.ridectrl.MainActivity
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.data.DueState
import com.vknxworks.ridectrl.data.Fmt
import com.vknxworks.ridectrl.data.LogRepository
import com.vknxworks.ridectrl.data.currentKm
import com.vknxworks.ridectrl.data.dueList
import com.vknxworks.ridectrl.reminders.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The glance widget: the main vehicle's odometer and next maintenance due, with an "Add fuel" button.
 * The main vehicle is chosen in Settings (the first vehicle until then). Nothing here goes online.
 * While the app lock is on it only shows "Locked"; its buttons open RideCtrl's lock screen first.
 */
class RideWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { updateAll(context) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, RideWidget::class.java))
            if (ids.isEmpty()) return
            val settings = context.container.settings.current()
            val data = LogRepository.read(context)
            val fmt = Fmt(settings)
            // With the app lock on, the widget shows nothing from the logs (no vehicle, odometer or due item):
            // anyone holding the unlocked phone would otherwise read them on the home screen.
            val locked = context.container.vault.enabled
            val vehicle = if (locked) null else data.vehicles.firstOrNull { it.id == settings.mainVehicleId } ?: data.vehicles.firstOrNull()
            val views = RemoteViews(context.packageName, R.layout.widget_ride)
            if (locked) {
                views.setTextViewText(R.id.widget_name, context.getString(R.string.app_name))
                views.setTextViewText(R.id.widget_odo, "–")
                views.setTextViewText(R.id.widget_due, context.getString(R.string.widget_locked))
            } else if (vehicle == null) {
                views.setTextViewText(R.id.widget_name, context.getString(R.string.app_name))
                views.setTextViewText(R.id.widget_odo, "–")
                views.setTextViewText(R.id.widget_due, context.getString(R.string.widget_no_vehicle))
            } else {
                views.setTextViewText(R.id.widget_name, vehicle.name)
                views.setTextViewText(R.id.widget_odo, fmt.distance(data.currentKm(vehicle.id)))
                val next = data.dueList(vehicle.id, settings.warnDays, settings.warnKm).firstOrNull { it.state != DueState.UNKNOWN }
                val due = when {
                    next == null -> context.getString(R.string.widget_nothing_due)
                    else -> {
                        val parts = buildList {
                            next.kmLeft?.let { add(if (it <= 0) context.getString(R.string.due_km_over, fmt.distance(-it)) else context.getString(R.string.due_km_left, fmt.distance(it))) }
                            next.dueDate?.let { add(fmt.shortDate(it.toString())) }
                        }.joinToString(" · ")
                        val prefix = when (next.state) {
                            DueState.OVERDUE -> context.getString(R.string.state_overdue)
                            DueState.SOON -> context.getString(R.string.state_soon)
                            else -> context.getString(R.string.widget_next)
                        }
                        "$prefix: ${next.item.name}" + if (parts.isNotEmpty()) "\n$parts" else ""
                    }
                }
                views.setTextViewText(R.id.widget_due, due)
            }
            views.setOnClickPendingIntent(R.id.widget_root, open(context, 10, vehicle?.let { "vehicle:${it.id}" } ?: "garage"))
            views.setOnClickPendingIntent(R.id.widget_add_fuel, open(context, 11, vehicle?.let { "fuel:${it.id}" } ?: "fuel"))
            manager.updateAppWidget(ids, views)
        }

        private fun open(context: Context, code: Int, action: String): PendingIntent = PendingIntent.getActivity(
            context,
            code,
            Intent(context, MainActivity::class.java)
                .putExtra(Reminders.EXTRA_ACTION, action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
