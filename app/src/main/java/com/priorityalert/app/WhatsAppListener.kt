package com.priorityalert.app

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Watches WhatsApp notifications.
 *
 * - A message notification from a VIP contact starts that contact's timer.
 * - When WhatsApp itself removes the notification (the chat was opened or answered,
 *   on the phone or on WhatsApp Web) or the user taps it, the timer is cancelled.
 * - Swiping the notification away does NOT cancel the timer.
 */
class WhatsAppListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in WHATSAPP_PACKAGES) return
        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (n.category == Notification.CATEGORY_CALL) return
        val extras = n.extras ?: return
        if (extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)) return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val contact = Store.findContact(this, title) ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val signature = "${n.`when`}|$text"

        val existing = Store.pending(this, contact.name)
        if (existing != null) {
            // Timer already running: keep the original deadline, just track the latest notification.
            if (existing.notifKey != sbn.key) Store.setPending(this, existing.copy(notifKey = sbn.key))
            Store.setSignature(this, contact.name, signature)
            return
        }

        // WhatsApp sometimes re-posts an unchanged notification; don't alert twice for it.
        if (signature == Store.signature(this, contact.name)) return
        Store.setSignature(this, contact.name, signature)

        Store.setPending(this, Pending(contact.name, sbn.key, text, System.currentTimeMillis()))
        AlertScheduler.schedule(this, contact)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap, reason: Int) {
        if (sbn.packageName !in WHATSAPP_PACKAGES) return
        val handled = reason == REASON_APP_CANCEL || reason == REASON_APP_CANCEL_ALL || reason == REASON_CLICK
        if (!handled) return

        // Stop a ringing alert for this conversation, if any.
        if (AlertService.ringingKey == sbn.key) {
            startService(Intent(this, AlertService::class.java).setAction(AlertService.ACTION_STOP))
        }
        Store.allPending(this).filter { it.notifKey == sbn.key }.forEach {
            AlertScheduler.cancel(this, it.contact)
            Store.clearPending(this, it.contact)
        }
    }
}
