package com.priorityalert.app

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.priorityalert.app.ui.HomeScreen
import com.priorityalert.app.ui.PriorityTheme
import com.priorityalert.app.ui.SetupState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Important-contacts list plus the setup checks the app needs to work reliably. */
class MainActivity : ComponentActivity() {

    private var contacts by mutableStateOf<List<Contact>>(emptyList())
    private var pending by mutableStateOf<Map<String, Pending>>(emptyMap())
    private var setup by mutableStateOf(SetupState(access = true, battery = true, overlay = true))
    private var now by mutableLongStateOf(System.currentTimeMillis())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startDebugTimerIfAsked()
        refresh()

        setContent {
            PriorityTheme {
                HomeScreen(
                    contacts = contacts,
                    pending = pending,
                    setup = setup,
                    now = now,
                    onOpenNotificationAccess = { open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    onOpenBattery = { open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
                    onOpenOverlay = {
                        open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                    },
                    onOpenAutoStart = ::openAutoStartSettings,
                    onTestAlert = {
                        AlertService.start(this, "Test", "Ceci est un test d'alerte.", System.currentTimeMillis(), null)
                    },
                    onSave = ::saveContact,
                    onDelete = ::deleteContact,
                    onToggle = ::toggleContact,
                )
            }
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        // Live state (countdowns, permissions) while the screen is visible.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                WatchService.start(this@MainActivity)
                while (true) {
                    refresh()
                    delay(1_000L)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startDebugTimerIfAsked()
    }

    private fun refresh() {
        now = System.currentTimeMillis()
        contacts = Store.contacts(this)
        pending = Store.allPending(this).associateBy { Store.normalize(it.contact) }
        setup = SetupState(
            access = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
                ?.contains(packageName) == true,
            battery = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName),
            overlay = Settings.canDrawOverlays(this),
        )
    }

    private fun saveContact(index: Int, contact: Contact) {
        val list = Store.contacts(this).toMutableList()
        if (index in list.indices) {
            forgetTimer(list[index])
            list[index] = contact
        } else {
            list.add(contact)
        }
        Store.saveContacts(this, list)
        refresh()
    }

    private fun deleteContact(index: Int) {
        val list = Store.contacts(this).toMutableList()
        if (index !in list.indices) return
        forgetTimer(list.removeAt(index))
        Store.saveContacts(this, list)
        refresh()
    }

    private fun toggleContact(index: Int, enabled: Boolean) {
        val list = Store.contacts(this).toMutableList()
        if (index !in list.indices) return
        if (!enabled) forgetTimer(list[index])
        list[index] = list[index].copy(enabled = enabled)
        Store.saveContacts(this, list)
        refresh()
    }

    /** A running timer belongs to the old settings; drop it when the contact changes or is removed. */
    private fun forgetTimer(contact: Contact) {
        AlertScheduler.cancel(this, contact.name)
        Store.clearPending(this, contact.name)
    }

    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.w("PriorityAlert", "Cannot open settings screen", e)
        }
    }

    /** Infinix/Tecno/itel (Phone Master) autostart screen, or the app's info page elsewhere. */
    private fun openAutoStartSettings() {
        val candidates = listOf(
            Intent("com.cyin.himgr.applicationmanager.view.activities.AUTO_START_ACTIVITY"),
            Intent().setComponent(
                ComponentName("com.transsion.phonemaster", "com.cyin.himgr.autostart.AutoStartActivity"),
            ),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
        )
        for (intent in candidates) {
            try {
                startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }
    }

    // Debug builds only: `adb shell am start -n com.priorityalert.app/.MainActivity --ei debug_timer_seconds 60`
    // starts a fake VIP timer, to test background alerts without a real WhatsApp message.
    private fun startDebugTimerIfAsked() {
        val seconds = intent.getIntExtra("debug_timer_seconds", 0)
        if (seconds <= 0 || applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        val start = System.currentTimeMillis()
        val p = Pending("Test minuteur", "", "Test du minuteur en arrière-plan", start, start + seconds * 1000L)
        Store.setPending(this, p)
        AlertScheduler.schedule(this, p)
        Log.i("PriorityAlert", "Debug timer started: ${seconds}s")
    }
}
