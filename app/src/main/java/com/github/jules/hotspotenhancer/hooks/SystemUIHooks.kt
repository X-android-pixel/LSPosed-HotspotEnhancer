package com.github.jules.hotspotenhancer.hooks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

object SystemUIHooks {
    private const val TAG = "HotspotEnhancer:SystemUI"
    private const val CHANNEL_ID = "hotspot_enhancer_notifications"
    private const val ACTION_NOTIFY = "com.github.jules.hotspotenhancer.ACTION_NOTIFY_CONNECT"

    fun hook(lpparam: LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return

        try {
            val clazz = XposedHelpers.findClass("com.android.systemui.SystemUIService", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(clazz, "onCreate", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val context = param.thisObject as Context
                    val filter = IntentFilter(ACTION_NOTIFY)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.registerReceiver(object : BroadcastReceiver() {
                            override fun onReceive(c: Context, i: Intent) { showNotification(c) }
                        }, filter, Context.RECEIVER_EXPORTED)
                    } else {
                        context.registerReceiver(object : BroadcastReceiver() {
                            override fun onReceive(c: Context, i: Intent) { showNotification(c) }
                        }, filter)
                    }
                }
            })
        } catch (e: Throwable) {
            XposedBridge.log("$TAG SystemUI hook failed: ${e.message}")
        }
    }

    private fun showNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Hotspot Enhancer", NotificationManager.IMPORTANCE_DEFAULT)
            nm.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Hotspot Enhancer")
            .setContentText("New device connected to hotspot")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setAutoCancel(true)
            .build()
        nm.notify(1001, notification)
    }
}
