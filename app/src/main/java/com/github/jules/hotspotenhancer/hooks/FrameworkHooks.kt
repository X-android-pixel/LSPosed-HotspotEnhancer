package com.github.jules.hotspotenhancer.hooks

import android.content.Context
import android.content.Intent
import android.net.MacAddress
import android.net.wifi.SoftApConfiguration
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import com.github.jules.hotspotenhancer.utils.Prefs
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

object FrameworkHooks {
    private const val TAG = "HotspotEnhancer:Framework"
    private val prefs = XSharedPreferences("com.github.jules.hotspotenhancer", "hotspot_enhancer_prefs")
    private val handler = Handler(Looper.getMainLooper())
    private var autoOffRunnable: Runnable? = null
    private var wifiService: Any? = null
    private var appContext: Context? = null

    fun hook(lpparam: LoadPackageParam) {
        try {
            hookSoftApConfiguration(lpparam)
            hookWifiServiceImpl(lpparam)
        } catch (e: Throwable) {
            XposedBridge.log("$TAG Error: ${e.message}")
        }
    }

    private fun hookSoftApConfiguration(lpparam: LoadPackageParam) {
        val builderClazz = XposedHelpers.findClass("android.net.wifi.SoftApConfiguration\$Builder", lpparam.classLoader)

        XposedHelpers.findAndHookMethod(
            builderClazz,
            "setMaxNumberOfClients",
            Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    prefs.reload()
                    if (prefs.contains(Prefs.KEY_MAX_CLIENTS)) {
                        val userMax = prefs.getInt(Prefs.KEY_MAX_CLIENTS, 10)
                        param.args[0] = userMax
                    }
                }
            }
        )

        XposedHelpers.findAndHookMethod(
            builderClazz,
            "build",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    prefs.reload()
                    val blockedMacs = prefs.getStringSet("blocked_macs", emptySet()) ?: emptySet()
                    if (blockedMacs.isNotEmpty()) {
                        try {
                            val macAddresses = blockedMacs.map { MacAddress.fromString(it) }
                            XposedHelpers.callMethod(param.thisObject, "setBlockedClientList", macAddresses)
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG Failed to set blacklist: ${e.message}")
                        }
                    }
                }
            }
        )
    }

    private fun hookWifiServiceImpl(lpparam: LoadPackageParam) {
        try {
            val wifiServiceClazz = XposedHelpers.findClass("com.android.server.wifi.WifiServiceImpl", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(
                wifiServiceClazz,
                "registerSoftApCallback",
                "android.net.wifi.ISoftApCallback",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val originalCallback = param.args[0]
                        if (appContext == null) {
                            appContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                        }
                        wifiService = param.thisObject

                        val callbackClazz = XposedHelpers.findClass("android.net.wifi.ISoftApCallback", lpparam.classLoader)
                        param.args[0] = Proxy.newProxyInstance(
                            lpparam.classLoader,
                            arrayOf(callbackClazz),
                            SoftApCallbackProxy(originalCallback)
                        )
                    }
                }
            )

            // Hook for Quick Edit logic
            XposedHelpers.findAndHookMethod(
                wifiServiceClazz,
                "setSoftApConfiguration",
                "android.net.wifi.SoftApConfiguration",
                "java.lang.String",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        XposedBridge.log("$TAG setSoftApConfiguration called - SSID/Pass might be changing")
                    }
                }
            )
        } catch (e: Exception) {
            XposedBridge.log("$TAG WifiServiceImpl hook failed: ${e.message}")
        }
    }

    private class SoftApCallbackProxy(private val original: Any) : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
            if (method.name == "onConnectedClientsChanged") {
                val clients = args?.get(0) as? List<*>
                clients?.let { onClientsChanged(it) }
            }
            return try {
                method.invoke(original, *(args ?: emptyArray()))
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun onClientsChanged(clients: List<*>) {
        val clientMacs = ArrayList<String>()
        for (client in clients) {
            try {
                val mac = XposedHelpers.callMethod(client, "getMacAddress").toString()
                clientMacs.add(mac)
            } catch (e: Exception) {}
        }

        if (clients.isEmpty()) {
            wifiService?.let { startAutoOffTimer(it) }
        } else {
            cancelAutoOffTimer()
            appContext?.let { context ->
                val intent = Intent("com.github.jules.hotspotenhancer.ACTION_CLIENT_CONNECTED")
                intent.putStringArrayListExtra("clients", clientMacs)
                intent.setPackage("com.github.jules.hotspotenhancer")
                context.sendBroadcast(intent)

                // Also notify SystemUI
                val sysUiIntent = Intent("com.github.jules.hotspotenhancer.ACTION_CLIENT_CONNECTED")
                sysUiIntent.setPackage("com.android.systemui")
                context.sendBroadcast(sysUiIntent)
            }
        }
    }

    private fun startAutoOffTimer(service: Any) {
        prefs.reload()
        if (!prefs.getBoolean(Prefs.KEY_AUTO_OFF_ENABLED, false)) return
        val timeout = prefs.getString(Prefs.KEY_AUTO_OFF_TIMEOUT, "600")?.toLong() ?: 600L
        if (timeout <= 0) return

        cancelAutoOffTimer()
        autoOffRunnable = Runnable {
            try {
                XposedHelpers.callMethod(service, "stopSoftAp", 0)
            } catch (e: Exception) {
                XposedBridge.log("$TAG Auto-off failed: ${e.message}")
            }
        }
        handler.postDelayed(autoOffRunnable!!, timeout * 1000)
    }

    private fun cancelAutoOffTimer() {
        autoOffRunnable?.let { handler.removeCallbacks(it) }
        autoOffRunnable = null
    }
}
