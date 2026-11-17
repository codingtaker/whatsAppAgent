package com.priorityalert.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

object AlertScheduler {

    private fun pendingIntent(c: Context, contactName: String): PendingIntent {
        val intent = Intent(c, AlarmReceiver::class.java)
            .setData(Uri.parse("priorityalert://contact/" + Uri.encode(Store.normalize(contactName))))
        return PendingIntent.getBroadcast(
            c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun schedule(c: Context, contact: Contact) {
        val am = c.getSystemService(AlarmManager::class.java)
        val at = System.currentTimeMillis() + contact.timeoutMinutes * 60_000L
        val pi = pendingIntent(c, contact.name)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(c: Context, contactName: String) {
        c.getSystemService(AlarmManager::class.java).cancel(pendingIntent(c, contactName))
    }
}

/** Fires when a VIP contact's timer runs out without the chat having been opened. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.data?.lastPathSegment ?: return
        val pending = Store.pending(context, key) ?: return
        Store.clearPending(context, pending.contact)
        AlertService.start(context, pending.contact, pending.message, pending.receivedAt, pending.notifKey)
    }
}
