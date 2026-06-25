package com.github.jules.hotspotenhancer.hooks

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.lang.reflect.Proxy

object SettingsHooks {
    private const val TAG = "HotspotEnhancer:Settings"

    fun hook(lpparam: LoadPackageParam) {
        if (lpparam.packageName != "com.android.settings") return

        try {
            val clazz = XposedHelpers.findClass("com.android.settings.wifi.tether.WifiTetherSettings", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(
                clazz,
                "onCreate",
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val fragment = param.thisObject
                        try {
                            val prefScreen = XposedHelpers.callMethod(fragment, "getPreferenceScreen")
                            val context = XposedHelpers.callMethod(fragment, "getContext") as android.content.Context

                            val prefClazz = XposedHelpers.findClass("androidx.preference.Preference", lpparam.classLoader)
                            val newPref = XposedHelpers.newInstance(prefClazz, context)

                            XposedHelpers.callMethod(newPref, "setTitle", "Hotspot Enhancer Settings")
                            XposedHelpers.callMethod(newPref, "setSummary", "Configure advanced hotspot features")

                            val listenerClazz = XposedHelpers.findClass("androidx.preference.Preference\$OnPreferenceClickListener", lpparam.classLoader)
                            val proxyListener = Proxy.newProxyInstance(lpparam.classLoader, arrayOf(listenerClazz)) { _, _, _ ->
                                val intent = Intent().apply {
                                    component = ComponentName("com.github.jules.hotspotenhancer", "com.github.jules.hotspotenhancer.ui.MainActivity")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                true
                            }
                            XposedHelpers.callMethod(newPref, "setOnPreferenceClickListener", proxyListener)
                            XposedHelpers.callMethod(prefScreen, "addPreference", newPref)
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG Injection failed: ${e.message}")
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("$TAG Settings hook failed: ${e.message}")
        }
    }
}
