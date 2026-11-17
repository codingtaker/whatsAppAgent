package com.priorityalert.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.util.Log

/**
 * Keeps the app process alive with a silent, permanent notification, and makes sure the
 * notification listener is (re)connected. Some ROMs (Infinix XOS, Xiaomi, ...) kill the app
 * when it is swiped from recents and never rebind the listener on their own.
 */
class WatchService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val watchdog = object : Runnable {
        override fun run() {
            AlertScheduler.fireDue(this@WatchService)
            handler.postDelayed(this, WATCHDOG_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Timers may have been lost if the process was killed or the phone restarted.
        AlertScheduler.restoreAll(this)
        handler.post(watchdog)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification(this)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
        ensureListener()
        return START_STICKY
    }

    private fun ensureListener() {
        if (WhatsAppListener.connected) return
        val cn = ComponentName(this, WhatsAppListener::class.java)
        Log.i(TAG, "Listener not connected, requesting rebind")
        runCatching { NotificationListenerService.requestRebind(cn) }
        // If the ROM ignores requestRebind, toggling the component forces the system to bind again.
        handler.postDelayed({
            if (!WhatsAppListener.connected) {
                Log.i(TAG, "Still not connected, toggling listener component")
                packageManager.setComponentEnabledSetting(
                    cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP,
                )
                packageManager.setComponentEnabledSetting(
                    cn, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP,
                )
            }
        }, 5_000L)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PriorityAlert"
        private const val CHANNEL_ID = "watch"
        private const val NOTIF_ID = 2
        private const val WATCHDOG_MS = 10_000L

        fun start(c: Context) {
            try {
                c.startForegroundService(Intent(c, WatchService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Could not start WatchService", e)
            }
        }

        private fun buildNotification(c: Context): Notification {
            val nm = c.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Surveillance", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "Garde la surveillance active en arrière-plan"
                    setShowBadge(false)
                },
            )
            val open = PendingIntent.getActivity(
                c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
            )
            return Notification.Builder(c, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Surveillance des contacts importants active")
                .setOngoing(true)
                .setContentIntent(open)
                .build()
        }
    }
}

/** Restarts the watch after a reboot or an app update. */
class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("PriorityAlert", "Boot/update: ${intent.action}")
        WatchService.start(context)
    }
}
