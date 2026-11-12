package com.vknxworks.ridectrl.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.Notification
import com.vknxworks.ridectrl.MainActivity
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.data.DueState
import com.vknxworks.ridectrl.data.Fmt
import com.vknxworks.ridectrl.data.LogRepository
import com.vknxworks.ridectrl.data.dueList
import com.vknxworks.ridectrl.data.state
import com.vknxworks.ridectrl.data.daysLeft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Local reminders, fully offline. Once a day (an inexact alarm, so no exact-alarm permission and little
 * battery) RideCtrl checks every service item and document and notifies when one becomes due soon or
 * overdue. Each item notifies once per state, not every day.
 */
object Reminders {
    const val CHANNEL = "reminders"
    const val EXTRA_ACTION = "com.vknxworks.ridectrl.ACTION"
    private const val ACTION_CHECK = "com.vknxworks.ridectrl.CHECK_REMINDERS"
    private const val PREFS = "reminders_sent"
    private const val CHECK_HOUR = 9

    fun createChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val ch = NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
        ch.description = context.getString(R.string.reminder_channel_desc)
        nm.createNotificationChannel(ch)
    }

    private fun checkIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        1,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_CHECK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Sets the next daily check (around 09:00), or cancels it when reminders are off. */
    suspend fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = checkIntent(context)
        val settings = context.container.settings.current()
        if (!settings.reminders) {
            am.cancel(pi)
            return
        }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(CHECK_HOUR, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Inexact and allowed while idle: Android may shift it a little, which is fine for a daily check.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    /** Checks everything now and posts notifications for what changed. */
    suspend fun check(context: Context) {
        val settings = context.container.settings.current()
        if (!settings.reminders) return
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        createChannel(context)
        val data = LogRepository.read(context)
        val fmt = Fmt(settings)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val sent = prefs.edit()
        val today = LocalDate.now()
        val live = mutableSetOf<String>()

        data.vehicles.forEach { v ->
            data.dueList(v.id, settings.warnDays, settings.warnKm, today).forEach { d ->
                val key = "item:${d.item.id}"
                live += key
                if (d.state == DueState.SOON || d.state == DueState.OVERDUE) {
                    if (prefs.getString(key, null) != d.state.name) {
                        val detail = buildList {
                            d.kmLeft?.let { add(if (it <= 0) context.getString(R.string.due_km_over, fmt.distance(-it)) else context.getString(R.string.due_km_left, fmt.distance(it))) }
                            d.dueDate?.let { add(context.getString(R.string.due_on, fmt.date(it.toString()))) }
                        }.joinToString(" · ")
                        val title = context.getString(
                            if (d.state == DueState.OVERDUE) R.string.reminder_overdue else R.string.reminder_soon,
                            d.item.name,
                        )
                        post(context, nm, key, v.name, title, detail, "maintenance:${v.id}")
                        sent.putString(key, d.state.name)
                    }
                } else {
                    sent.remove(key)
                }
            }
        }
        data.docs.forEach { doc ->
            val key = "doc:${doc.id}"
            live += key
            val st = doc.state(today)
            val vehicle = data.vehicles.firstOrNull { it.id == doc.vehicleId }?.name ?: ""
            if (st == DueState.SOON || st == DueState.OVERDUE) {
                if (prefs.getString(key, null) != st.name) {
                    val days = doc.daysLeft(today) ?: 0
                    val title = context.getString(
                        if (st == DueState.OVERDUE) R.string.reminder_doc_expired else R.string.reminder_doc_soon,
                        doc.title.ifBlank { context.getString(R.string.doc_untitled) },
                    )
                    val detail = if (days < 0) context.getString(R.string.doc_expired_on, fmt.date(doc.expiry))
                    else context.resources.getQuantityString(R.plurals.doc_expires_in, days.toInt(), days.toInt(), fmt.date(doc.expiry))
                    post(context, nm, key, vehicle, title, detail, "docs:${doc.vehicleId}")
                    sent.putString(key, st.name)
                }
            } else {
                sent.remove(key)
            }
        }
        // Forget items and documents that were deleted.
        prefs.all.keys.filter { it !in live }.forEach { sent.remove(it) }
        sent.apply()
    }

    private fun post(context: Context, nm: NotificationManager, key: String, vehicle: String, title: String, text: String, action: String) {
        val open = PendingIntent.getActivity(
            context,
            key.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_ACTION, action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // On the lock screen only "RideCtrl reminder" shows, not what's due on which vehicle.
        val public = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_ride)
            .setContentTitle(context.getString(R.string.reminder_public))
            .build()
        // With the app lock on, the notification itself doesn't say what's due either: only after unlocking.
        val locked = context.container.vault.enabled
        val shownTitle = if (locked) context.getString(R.string.reminder_public) else title
        val shownText = if (locked) context.getString(R.string.reminder_locked) else text
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_ride)
            .setContentTitle(shownTitle)
            .setContentText(shownText)
            .setSubText(if (locked) null else vehicle.ifBlank { null })
            .setStyle(Notification.BigTextStyle().bigText(shownText))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()
        nm.notify(key.hashCode(), n)
    }

    /**
     * The app lock was just turned on: reminders already in the shade still say what's due on which vehicle.
     * Posts them again with the same generic text new ones get while locked; tapping one still opens the same screen.
     */
    fun hideDetails(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val active = runCatching { nm.activeNotifications }.getOrNull() ?: return
        val public = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_ride)
            .setContentTitle(context.getString(R.string.reminder_public))
            .build()
        for (sbn in active) {
            val old = sbn.notification
            if (sbn.packageName != context.packageName || old.channelId != CHANNEL) continue
            val text = context.getString(R.string.reminder_locked)
            val n = Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_ride)
                .setContentTitle(context.getString(R.string.reminder_public))
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(old.contentIntent)
                .setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setPublicVersion(public)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setOnlyAlertOnce(true)
                .build()
            runCatching { nm.notify(sbn.tag, sbn.id, n) }.onFailure { nm.cancel(sbn.tag, sbn.id) }
        }
    }
}

/** The daily check, and re-arming after a reboot, an app update or a time change. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == "com.vknxworks.ridectrl.CHECK_REMINDERS") {
                    runCatching { Reminders.check(context) }
                }
                runCatching { Reminders.schedule(context) }
            } finally {
                pending.finish()
            }
        }
    }
}
