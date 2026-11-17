package com.priorityalert.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

val WHATSAPP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")

/** A VIP contact. [name] must match the name WhatsApp shows in its notifications. */
data class Contact(val name: String, val timeoutMinutes: Int, val enabled: Boolean = true)

/** An unanswered message whose timer is running. */
data class Pending(
    val contact: String,
    val notifKey: String,
    val message: String,
    val receivedAt: Long,
    /** When the alert must ring if the chat has not been opened. */
    val deadline: Long,
)

/** Everything is kept on the device, in SharedPreferences. */
object Store {
    private const val KEY_CONTACTS = "contacts"
    private const val KEY_PENDING = "pending"
    private const val KEY_SIGNATURE = "sig_"

    private fun prefs(c: Context): SharedPreferences =
        c.applicationContext.getSharedPreferences("priority_alert", Context.MODE_PRIVATE)

    /** WhatsApp sometimes appends "(3 messages)" to the title and pads names with invisible marks. */
    fun normalize(name: String): String =
        name.replace(Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069]"), "")
            .replace(Regex("\\s*\\(\\d+ [^)]*\\)\\s*$"), "")
            .trim()
            .lowercase()

    // ---- contacts ----

    @Synchronized
    fun contacts(c: Context): List<Contact> {
        val raw = prefs(c).getString(KEY_CONTACTS, null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Contact(o.getString("name"), o.getInt("timeout"), o.optBoolean("enabled", true))
        }
    }

    @Synchronized
    fun saveContacts(c: Context, list: List<Contact>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("name", it.name).put("timeout", it.timeoutMinutes).put("enabled", it.enabled))
        }
        prefs(c).edit().putString(KEY_CONTACTS, arr.toString()).apply()
    }

    fun findContact(c: Context, sender: String): Contact? {
        val key = normalize(sender)
        return contacts(c).firstOrNull { it.enabled && normalize(it.name) == key }
    }

    // ---- pending timers ----

    @Synchronized
    fun allPending(c: Context): List<Pending> {
        val raw = prefs(c).getString(KEY_PENDING, null) ?: return emptyList()
        val obj = JSONObject(raw)
        return obj.keys().asSequence().map { k ->
            val o = obj.getJSONObject(k)
            val at = o.getLong("at")
            Pending(
                o.getString("contact"), o.getString("key"), o.getString("message"),
                at, o.optLong("deadline", at + 60_000L),
            )
        }.toList()
    }

    fun pending(c: Context, contact: String): Pending? {
        val key = normalize(contact)
        return allPending(c).firstOrNull { normalize(it.contact) == key }
    }

    @Synchronized
    fun setPending(c: Context, p: Pending) {
        val obj = JSONObject(prefs(c).getString(KEY_PENDING, null) ?: "{}")
        obj.put(
            normalize(p.contact),
            JSONObject().put("contact", p.contact).put("key", p.notifKey)
                .put("message", p.message).put("at", p.receivedAt).put("deadline", p.deadline),
        )
        prefs(c).edit().putString(KEY_PENDING, obj.toString()).apply()
    }

    @Synchronized
    fun clearPending(c: Context, contact: String) {
        val obj = JSONObject(prefs(c).getString(KEY_PENDING, null) ?: "{}")
        obj.remove(normalize(contact))
        prefs(c).edit().putString(KEY_PENDING, obj.toString()).apply()
    }

    // ---- last notification already taken into account, per contact ----

    fun signature(c: Context, contact: String): String? =
        prefs(c).getString(KEY_SIGNATURE + normalize(contact), null)

    fun setSignature(c: Context, contact: String, signature: String) {
        prefs(c).edit().putString(KEY_SIGNATURE + normalize(contact), signature).apply()
    }
}
