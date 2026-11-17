package com.priorityalert.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Full-screen "incoming call" style screen shown over the lock screen while the alert rings. */
class AlertActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        render(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render(intent)
    }

    private fun render(intent: Intent) {
        val contact = intent.getStringExtra(AlertService.EXTRA_CONTACT).orEmpty()
        val message = intent.getStringExtra(AlertService.EXTRA_MESSAGE).orEmpty()
        val receivedAt = intent.getLongExtra(AlertService.EXTRA_RECEIVED_AT, System.currentTimeMillis())
        val minutes = ((System.currentTimeMillis() - receivedAt) / 60_000L).coerceAtLeast(0)
        val pad = (24 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }
        root.addView(TextView(this).apply {
            text = "WhatsApp Alert"
            textSize = 16f
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = contact
            textSize = 34f
            gravity = Gravity.CENTER
            setPadding(0, pad / 2, 0, pad / 2)
        })
        root.addView(TextView(this).apply {
            text = "vous a écrit il y a $minutes min et n'a pas eu de réponse."
            textSize = 18f
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = if (message.isBlank()) "" else "« $message »"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, pad, 0, pad * 2)
        })
        root.addView(Button(this).apply {
            text = "Ouvrir WhatsApp"
            textSize = 18f
            setOnClickListener {
                stopAlert()
                WHATSAPP_PACKAGES.firstNotNullOfOrNull { packageManager.getLaunchIntentForPackage(it) }
                    ?.let { startActivity(it) }
                finish()
            }
        })
        root.addView(Button(this).apply {
            text = "Arrêter l'alerte"
            textSize = 18f
            setOnClickListener {
                stopAlert()
                finish()
            }
        })
        setContentView(root)
    }

    private fun stopAlert() {
        stopService(Intent(this, AlertService::class.java))
    }
}
