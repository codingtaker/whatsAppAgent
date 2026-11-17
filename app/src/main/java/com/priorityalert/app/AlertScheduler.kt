package com.priorityalert.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

object AlertScheduler {
    private const val TAG = "PriorityAlert"

    private fun pendingIntent(c: Context, contactName: String): PendingIntent {
        val intent = Intent(c, AlarmReceiver::class.java)
            .setData(Uri.parse("priorityalert://contact/" + Uri.encode(Store.normalize(contactName))))
        return PendingIntent.getBroadcast(
            c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun schedule(c: Context, p: Pending) = scheduleAt(c, p.contact, p.deadline)

    /** Re-arms every running timer (after a reboot or the process being killed or frozen). */
    fun restoreAll(c: Context) {
        val now = System.currentTimeMillis()
        Store.allPending(c).forEach { scheduleAt(c, it.contact, maxOf(it.deadline, now + 5_000L)) }
    }

    /**
     * Uses an "alarm clock" alarm: it is the kind OEM battery managers (Infinix XOS Hiber, ...)
     * are least likely to delay or hold back while the app is frozen.
     */
    private fun scheduleAt(c: Context, contactName: String, at: Long) {
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(c, contactName)
        val show = PendingIntent.getActivity(
            c, 2, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi)
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarms not allowed, using an inexact one", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(c: Context, contactName: String) {
        c.getSystemService(AlarmManager::class.java).cancel(pendingIntent(c, contactName))
    }

    /** Fires every timer whose deadline has passed (backup for a late or held-back alarm). */
    fun fireDue(c: Context) {
        val now = System.currentTimeMillis()
        Store.allPending(c).filter { it.deadline <= now }.forEach { fire(c, it, "watchdog") }
    }

    fun fire(c: Context, p: Pending, source: String) {
        val lateSec = (System.currentTimeMillis() - p.deadline) / 1000
        Log.i(TAG, "Alert for ${p.contact} fired by $source (${lateSec}s after deadline)")
        Store.clearPending(c, p.contact)
        cancel(c, p.contact)
        AlertService.start(c, p.contact, p.message, p.receivedAt, p.notifKey.ifEmpty { null })
    }
}

/** Fires when a VIP contact's timer runs out without the chat having been opened. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.data?.lastPathSegment ?: return
        val pending = Store.pending(context, key) ?: return
        AlertScheduler.fire(context, pending, "alarm")
    }
}
