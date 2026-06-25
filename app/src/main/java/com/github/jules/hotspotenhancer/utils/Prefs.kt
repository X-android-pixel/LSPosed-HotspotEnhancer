package com.github.jules.hotspotenhancer.utils

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val PREF_NAME = "hotspot_enhancer_prefs"

    const val KEY_MAX_CLIENTS = "max_clients"
    const val KEY_AUTO_OFF_ENABLED = "auto_off_enabled"
    const val KEY_AUTO_OFF_TIMEOUT = "auto_off_timeout"
    const val KEY_NOTIFY_CONNECT = "notify_connect"

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Note: For Xposed, we usually use XSharedPreferences
}
