package com.priorityalert.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

/** Important-contacts list plus the setup checks the app needs to work reliably. */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var accessButton: Button
    private lateinit var batteryButton: Button
    private lateinit var list: ListView
    private var contacts: List<Contact> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Contacts importants"
        val pad = (16 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        status = TextView(this).apply { textSize = 15f }
        accessButton = Button(this).apply {
            text = "Autoriser l'accès aux notifications"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
        batteryButton = Button(this).apply {
            text = "Désactiver l'optimisation de batterie"
            setOnClickListener { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }
        list = ListView(this).apply {
            setOnItemClickListener { _, _, position, _ -> editContact(position) }
        }
        val add = Button(this).apply {
            text = "+ Ajouter un contact important"
            setOnClickListener { editContact(-1) }
        }
        val test = Button(this).apply {
            text = "Tester l'alerte"
            setOnClickListener {
                AlertService.start(
                    this@MainActivity, "Test", "Ceci est un test d'alerte.", System.currentTimeMillis(), null,
                )
            }
        }

        root.addView(status)
        root.addView(accessButton)
        root.addView(batteryButton)
        root.addView(list, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(add)
        root.addView(test)
        setContentView(root)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun hasNotificationAccess(): Boolean =
        Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true

    private fun refresh() {
        val access = hasNotificationAccess()
        val battery = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        status.text = when {
            !access -> "⚠ L'app ne peut pas encore voir les notifications WhatsApp."
            !battery -> "⚠ Android peut endormir l'app : désactivez l'optimisation de batterie pour Priority Alert."
            else -> "✓ Surveillance active."
        }
        accessButton.visibility = if (access) View.GONE else View.VISIBLE
        batteryButton.visibility = if (battery) View.GONE else View.VISIBLE

        contacts = Store.contacts(this)
        val rows = contacts.map {
            (if (it.enabled) "✓ " else "✗ ") + it.name + "\nDélai : ${it.timeoutMinutes} min · Alerte : sonnerie"
        }
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
    }

    /** [index] = -1 to add a new contact. */
    private fun editContact(index: Int) {
        val current = contacts.getOrNull(index)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val name = EditText(this).apply {
            hint = "Nom exact affiché dans WhatsApp"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setText(current?.name.orEmpty())
        }
        val minutes = EditText(this).apply {
            hint = "Délai en minutes"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText((current?.timeoutMinutes ?: 5).toString())
        }
        val enabled = CheckBox(this).apply {
            text = "Actif"
            isChecked = current?.enabled ?: true
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(name)
            addView(minutes)
            addView(enabled)
        }

        val builder = AlertDialog.Builder(this)
            .setTitle(if (current == null) "Nouveau contact" else "Modifier le contact")
            .setView(form)
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Enregistrer") { _, _ ->
                val newName = name.text.toString().trim()
                val newMinutes = minutes.text.toString().toIntOrNull()
                if (newName.isEmpty() || newMinutes == null || newMinutes < 1) {
                    Toast.makeText(this, "Nom et délai (≥ 1 min) requis", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val updated = contacts.toMutableList()
                val contact = Contact(newName, newMinutes, enabled.isChecked)
                if (current == null) updated.add(contact) else updated[index] = contact
                current?.let { forgetTimer(it) }
                Store.saveContacts(this, updated)
                refresh()
            }
        if (current != null) {
            builder.setNeutralButton("Supprimer") { _, _ ->
                forgetTimer(current)
                Store.saveContacts(this, contacts.filterIndexed { i, _ -> i != index })
                refresh()
            }
        }
        builder.show()
    }

    /** A running timer belongs to the old settings; drop it when the contact changes or is removed. */
    private fun forgetTimer(contact: Contact) {
        AlertScheduler.cancel(this, contact.name)
        Store.clearPending(this, contact.name)
    }
}
