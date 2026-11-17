package com.priorityalert.app

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.priorityalert.app.ui.AlertInfo
import com.priorityalert.app.ui.AlertScreen
import com.priorityalert.app.ui.PriorityTheme

/** Full-screen "incoming call" style screen shown over the lock screen while the alert rings. */
class AlertActivity : ComponentActivity() {

    private var info by mutableStateOf(AlertInfo("", "", 0L))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        info = read(intent)

        setContent {
            PriorityTheme(dark = true) {
                AlertScreen(
                    info = info,
                    onOpenWhatsApp = {
                        stopAlert()
                        WHATSAPP_PACKAGES.firstNotNullOfOrNull { packageManager.getLaunchIntentForPackage(it) }
                            ?.let { startActivity(it) }
                        finish()
                    },
                    onStop = {
                        stopAlert()
                        finish()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        info = read(intent)
    }

    private fun read(intent: Intent) = AlertInfo(
        contact = intent.getStringExtra(AlertService.EXTRA_CONTACT).orEmpty(),
        message = intent.getStringExtra(AlertService.EXTRA_MESSAGE).orEmpty(),
        receivedAt = intent.getLongExtra(AlertService.EXTRA_RECEIVED_AT, System.currentTimeMillis()),
    )

    private fun stopAlert() {
        stopService(Intent(this, AlertService::class.java))
    }
}
