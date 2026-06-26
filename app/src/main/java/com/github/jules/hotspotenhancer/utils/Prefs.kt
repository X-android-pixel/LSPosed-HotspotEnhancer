package com.github.jules.hotspotenhancer.utils

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences
import java.io.File

object Prefs {
    const val PACKAGE_NAME = "com.github.jules.hotspotenhancer"
    const val PREF_NAME = "hotspot_enhancer_prefs"

    const val KEY_MAX_CLIENTS = "max_clients"
    const val KEY_AUTO_OFF_ENABLED = "auto_off_enabled"
    const val KEY_AUTO_OFF_TIMEOUT = "auto_off_timeout"
    const val KEY_NOTIFY_CONNECT = "notify_connect"
    const val KEY_SSID = "hotspot_ssid"
    const val KEY_PASSWORD = "hotspot_password"
    const val KEY_BLOCKED_MACS = "blocked_macs"

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun makeWorldReadable(context: Context) {
        try {
            val prefFile = File(context.applicationInfo.dataDir + "/shared_prefs/" + PREF_NAME + ".xml")
            if (prefFile.exists()) {
                prefFile.setReadable(true, false)
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    fun getXposedPrefs(): XSharedPreferences {
        val prefs = XSharedPreferences(PACKAGE_NAME, PREF_NAME)
        prefs.makeWorldReadable()
        return prefs
    }
}
