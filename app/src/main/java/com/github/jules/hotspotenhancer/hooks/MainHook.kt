package com.github.jules.hotspotenhancer.hooks

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import de.robv.android.xposed.XposedBridge

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        // Initialization for system-wide hooks if needed
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        when (lpparam.packageName) {
            "android" -> {
                FrameworkHooks.hook(lpparam)
            }
            "com.android.settings" -> {
                SettingsHooks.hook(lpparam)
            }
            "com.android.systemui" -> {
                SystemUIHooks.hook(lpparam)
            }
        }
    }
}
