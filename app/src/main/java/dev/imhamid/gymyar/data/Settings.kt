package dev.imhamid.gymyar.data

import android.content.Context
import dev.imhamid.gymyar.GymyarApp
import dev.imhamid.gymyar.ui.theme.ThemeMode

/** Small on-device preferences (not tied to the account). */
object Settings {
    private val prefs get() = GymyarApp.instance.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = runCatching { ThemeMode.valueOf(prefs.getString("theme", "SYSTEM") ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM)
        set(v) { prefs.edit().putString("theme", v.name).apply() }

    var remindersEnabled: Boolean
        get() = prefs.getBoolean("reminders", true)
        set(v) { prefs.edit().putBoolean("reminders", v).apply() }

    /** True once the in-app notification permission prompt has been shown (it is only shown once). */
    var notificationsPrompted: Boolean
        get() = prefs.getBoolean("notif_prompted", false)
        set(v) { prefs.edit().putBoolean("notif_prompted", v).apply() }
}
