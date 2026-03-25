package org.pysh.janus.hook

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookEntry : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "Janus"
        private const val TARGET_PACKAGE = "com.xiaomi.subscreencenter"
        private const val TARGET_CLASS = "p2.a"
        private const val SELF_PACKAGE = "org.pysh.janus"
        private const val PREFS_NAME = "janus_config"
        private const val KEY_WHITELIST = "music_whitelist"
        private const val KEY_DISABLE_TRACKING = "disable_tracking"
    }

    private val prefs by lazy { XSharedPreferences(SELF_PACKAGE, PREFS_NAME) }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            SELF_PACKAGE -> hookSelf(lpparam)
            TARGET_PACKAGE -> {
                hookMusicWhitelist(lpparam)
                hookTracking(lpparam)
            }
        }
    }

    private fun hookSelf(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "$SELF_PACKAGE.MainActivity",
                lpparam.classLoader,
                "isModuleActive",
                XC_MethodReplacement.returnConstant(true)
            )
        } catch (e: Throwable) {
            XposedBridge.log("[$TAG] hookSelf failed: ${e.message}")
        }
    }

    private fun hookMusicWhitelist(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val targetClass = XposedHelpers.findClass(TARGET_CLASS, lpparam.classLoader)

            // Hook c(String) -> boolean : single package whitelist check
            XposedHelpers.findAndHookMethod(
                targetClass, "c", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val result = param.result as? Boolean ?: return
                        if (result) return

                        val packageName = param.args[0] as? String ?: return
                        val customWhitelist = getCustomWhitelist()
                        if (packageName in customWhitelist) {
                            param.result = true
                            XposedBridge.log("[$TAG] Allowed: $packageName")
                        }
                    }
                }
            )

            // Hook b() -> HashSet<String> : full whitelist retrieval
            XposedHelpers.findAndHookMethod(
                targetClass, "b",
                object : XC_MethodHook() {
                    @Suppress("UNCHECKED_CAST")
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val result = (param.result as? HashSet<String>)
                            ?: HashSet<String>().also { param.result = it }
                        val customWhitelist = getCustomWhitelist()
                        result.addAll(customWhitelist)
                        param.result = result
                    }
                }
            )

            XposedBridge.log("[$TAG] Hooks installed successfully")
        } catch (e: Throwable) {
            XposedBridge.log("[$TAG] hookMusicWhitelist failed: ${e.message}")
        }
    }

    private fun hookTracking(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val receiverClass = XposedHelpers.findClass(
                "$TARGET_PACKAGE.track.DailyTrackReceiver",
                lpparam.classLoader
            )
            XposedHelpers.findAndHookMethod(
                receiverClass, "onReceive",
                android.content.Context::class.java,
                android.content.Intent::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (isTrackingDisabled()) {
                            param.result = null
                            XposedBridge.log("[$TAG] Blocked daily tracking report")
                        }
                    }
                }
            )
            XposedBridge.log("[$TAG] Tracking hook installed")
        } catch (e: Throwable) {
            XposedBridge.log("[$TAG] hookTracking failed: ${e.message}")
        }
    }

    private fun isTrackingDisabled(): Boolean {
        return try {
            prefs.reload()
            prefs.getBoolean(KEY_DISABLE_TRACKING, false)
        } catch (e: Throwable) {
            false
        }
    }

    private fun getCustomWhitelist(): Set<String> {
        return try {
            prefs.reload()
            val raw = prefs.getString(KEY_WHITELIST, "") ?: ""
            raw.split(",").filter { it.isNotBlank() }.toSet()
        } catch (e: Throwable) {
            XposedBridge.log("[$TAG] Failed to read whitelist: ${e.message}")
            emptySet()
        }
    }
}
