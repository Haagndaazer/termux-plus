package com.termux.app.plus.settings

import android.content.Context

class TermuxPlusPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("termux_plus_prefs", Context.MODE_PRIVATE)

    var defaultServerAliveInterval: Int
        get() = prefs.getInt("default_server_alive_interval", 60)
        set(value) = prefs.edit().putInt("default_server_alive_interval", value).apply()

    var defaultServerAliveCountMax: Int
        get() = prefs.getInt("default_server_alive_count_max", 3)
        set(value) = prefs.edit().putInt("default_server_alive_count_max", value).apply()

    var autoAcquireWakelock: Boolean
        get() = prefs.getBoolean("auto_acquire_wakelock", true)
        set(value) = prefs.edit().putBoolean("auto_acquire_wakelock", value).apply()
}
