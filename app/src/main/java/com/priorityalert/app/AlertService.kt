package com.priorityalert.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log

/**
 * The "phone call": rings on the alarm stream (audible even when the ringer is silent),
 * vibrates, and shows a full-screen alert over the lock screen until stopped.
 */
class AlertService : Service() {

    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stopSelf() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val contact = intent.getStringExtra(EXTRA_CONTACT).orEmpty()
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        val receivedAt = intent.getLongExtra(EXTRA_RECEIVED_AT, System.currentTimeMillis())
        ringingKey = intent.getStringExtra(EXTRA_NOTIF_KEY)

        val notification = buildNotification(this, contact, message, receivedAt, ongoing = true)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, notification)
        }

        startRinging()
        // With "display over other apps" granted, open the alert screen directly: some ROMs
        // downgrade full-screen notifications to a simple banner on the lock screen.
        if (Settings.canDrawOverlays(this)) {
            try {
                startActivity(alertIntent(this, contact, message, receivedAt))
            } catch (e: Exception) {
                Log.w(TAG, "Could not open alert screen", e)
            }
        }
        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, MAX_RING_MS)
        return START_NOT_STICKY
    }

    private fun startRinging() {
        stopRinging()
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(this@AlertService, uri)
                setWakeMode(this@AlertService, PowerManager.PARTIAL_WAKE_LOCK)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not play alarm sound", e)
        }
        @Suppress("DEPRECATION")
        getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0), attrs)
    }

    private fun stopRinging() {
        player?.runCatching { stop(); release() }
        player = null
        getSystemService(Vibrator::class.java)?.cancel()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoStop)
        stopRinging()
        ringingKey = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PriorityAlert"
        private const val CHANNEL_ID = "priority_alerts"
        private const val NOTIF_ID = 1
        private const val MAX_RING_MS = 2 * 60_000L

        const val ACTION_STOP = "com.priorityalert.app.STOP"
        const val EXTRA_CONTACT = "contact"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_RECEIVED_AT = "receivedAt"
        const val EXTRA_NOTIF_KEY = "notifKey"

        /** Key of the WhatsApp notification the current alert is about, or null when not ringing. */
        @Volatile
        var ringingKey: String? = null

        fun start(c: Context, contact: String, message: String, receivedAt: Long, notifKey: String?) {
            val intent = Intent(c, AlertService::class.java)
                .putExtra(EXTRA_CONTACT, contact)
                .putExtra(EXTRA_MESSAGE, message)
                .putExtra(EXTRA_RECEIVED_AT, receivedAt)
                .putExtra(EXTRA_NOTIF_KEY, notifKey)
            try {
                c.startForegroundService(intent)
            } catch (e: Exception) {
                // The system refused a background start (exact alarms not allowed): fall back to
                // a plain high-priority notification so the alert is not lost.
                Log.w(TAG, "Foreground start refused, falling back to notification", e)
                c.getSystemService(NotificationManager::class.java)
                    .notify(NOTIF_ID, buildNotification(c, contact, message, receivedAt, ongoing = false))
            }
        }

        private fun alertIntent(c: Context, contact: String, message: String, receivedAt: Long): Intent =
            Intent(c, AlertActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_CONTACT, contact)
                .putExtra(EXTRA_MESSAGE, message)
                .putExtra(EXTRA_RECEIVED_AT, receivedAt)

        private fun buildNotification(
            c: Context, contact: String, message: String, receivedAt: Long, ongoing: Boolean,
        ): Notification {
            val nm = c.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Alertes prioritaires", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Message important resté sans réponse"
                    setBypassDnd(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
            )
            val immutable = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val fullScreen = PendingIntent.getActivity(
                c, 0, alertIntent(c, contact, message, receivedAt), immutable,
            )
            val stop = PendingIntent.getService(
                c, 1, Intent(c, AlertService::class.java).setAction(ACTION_STOP), immutable,
            )
            val minutes = ((System.currentTimeMillis() - receivedAt) / 60_000L).coerceAtLeast(0)
            return Notification.Builder(c, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Message de $contact sans réponse")
                .setContentText("Reçu il y a $minutes min : $message")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(ongoing)
                .setContentIntent(fullScreen)
                .setFullScreenIntent(fullScreen, true)
                .addAction(Notification.Action.Builder(null, "Arrêter", stop).build())
                .build()
        }
    }
}
