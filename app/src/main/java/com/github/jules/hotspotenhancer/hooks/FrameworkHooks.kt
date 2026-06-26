package com.github.jules.hotspotenhancer.hooks

import android.content.Context
import android.content.Intent
import android.net.MacAddress
import android.net.wifi.SoftApConfiguration
import android.net.wifi.WifiManager
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
    private val prefs = XSharedPreferences(Prefs.PACKAGE_NAME, Prefs.PREF_NAME)
    private val handler = Handler(Looper.getMainLooper())
    private var autoOffRunnable: Runnable? = null
    private var wifiService: Any? = null
    private var appContext: Context? = null

    const val ACTION_CLIENTS_UPDATED = "com.github.jules.hotspotenhancer.ACTION_CLIENTS_UPDATED"

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
            builderClazz, "setMaxNumberOfClients", Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    prefs.reload()
                    if (prefs.contains(Prefs.KEY_MAX_CLIENTS)) {
                        param.args[0] = prefs.getInt(Prefs.KEY_MAX_CLIENTS, 10)
                    }
                }
            }
        )

        XposedHelpers.findAndHookMethod(
            builderClazz, "build",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    prefs.reload()

                    // Apply SSID/Password if present in prefs
                    val ssid = prefs.getString(Prefs.KEY_SSID, null)
                    val pass = prefs.getString(Prefs.KEY_PASSWORD, null)
                    if (ssid != null) XposedHelpers.callMethod(param.thisObject, "setSsid", ssid)
                    if (pass != null) XposedHelpers.callMethod(param.thisObject, "setPassphrase", pass, SoftApConfiguration.SECURITY_TYPE_WPA2_PSK)

                    // Apply Blacklist
                    val blockedMacs = prefs.getStringSet(Prefs.KEY_BLOCKED_MACS, emptySet()) ?: emptySet()
                    if (blockedMacs.isNotEmpty()) {
                        val macAddresses = blockedMacs.map { MacAddress.fromString(it) }
                        XposedHelpers.callMethod(param.thisObject, "setBlockedClientList", macAddresses)
                    }
                }
            }
        )
    }

    private fun hookWifiServiceImpl(lpparam: LoadPackageParam) {
        try {
            val wifiServiceClazz = XposedHelpers.findClass("com.android.server.wifi.WifiServiceImpl", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(
                wifiServiceClazz, "registerSoftApCallback", "android.net.wifi.ISoftApCallback",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val originalCallback = param.args[0]
                        if (appContext == null) {
                            appContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                        }
                        wifiService = param.thisObject

                        val callbackClazz = XposedHelpers.findClass("android.net.wifi.ISoftApCallback", lpparam.classLoader)
                        param.args[0] = Proxy.newProxyInstance(
                            lpparam.classLoader, arrayOf(callbackClazz),
                            SoftApCallbackProxy(originalCallback)
                        )
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
            return try { method.invoke(original, *(args ?: emptyArray())) } catch (e: Exception) { null }
        }
    }

    private fun onClientsChanged(clients: List<*>) {
        val clientData = ArrayList<String>()
        for (client in clients) {
            try {
                val mac = XposedHelpers.callMethod(client, "getMacAddress").toString().lowercase()
                clientData.add(mac)
            } catch (e: Exception) {}
        }

        if (clients.isEmpty()) {
            wifiService?.let { startAutoOffTimer(it) }
        } else {
            cancelAutoOffTimer()
        }

        appContext?.let { context ->
            val intent = Intent(ACTION_CLIENTS_UPDATED)
            intent.putStringArrayListExtra("clients", clientData)
            intent.setPackage(Prefs.PACKAGE_NAME)
            context.sendBroadcast(intent)

            // Notification intent
            val sysUiIntent = Intent("com.github.jules.hotspotenhancer.ACTION_NOTIFY_CONNECT")
            sysUiIntent.setPackage("com.android.systemui")
            context.sendBroadcast(sysUiIntent)
        }
    }

    private fun startAutoOffTimer(service: Any) {
        prefs.reload()
        if (!prefs.getBoolean(Prefs.KEY_AUTO_OFF_ENABLED, false)) return
        val timeout = prefs.getString(Prefs.KEY_AUTO_OFF_TIMEOUT, "600")?.toLong() ?: 600L
        if (timeout <= 0) return

        cancelAutoOffTimer()
        autoOffRunnable = Runnable {
            try { XposedHelpers.callMethod(service, "stopSoftAp", 0) } catch (e: Exception) {}
        }
        handler.postDelayed(autoOffRunnable!!, timeout * 1000)
    }

    private fun cancelAutoOffTimer() {
        autoOffRunnable?.let { handler.removeCallbacks(it) }
        autoOffRunnable = null
    }
}
