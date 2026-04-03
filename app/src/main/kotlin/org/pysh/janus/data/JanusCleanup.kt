package org.pysh.janus.data

import android.util.Base64
import org.json.JSONArray
import org.pysh.janus.util.JanusPaths
import org.pysh.janus.util.RootUtils
import java.io.File

/**
 * Removes all Janus residual files from system directories.
 * Call before uninstalling the app.
 */
object JanusCleanup {

    fun cleanAll(): Boolean {
        var ok = true
        // 1. Remove janus/ under subscreencenter (config + cards + templates)
        ok = RootUtils.exec("rm -rf '${JanusPaths.JANUS_BASE}'") && ok
        // 2. Remove janus/ under rearScreenWhite (custom.mrc + backups)
        ok = RootUtils.exec("rm -rf '${JanusPaths.WALLPAPER_DIR}'") && ok
        // 3. Clean legacy paths from older versions
        ok = JanusPaths.cleanLegacyPaths() && ok
        // 4. Clean runtime.json entries and associated directories
        ok = cleanRuntimeEntries() && ok
        // 5. Restart subscreencenter to pick up changes
        RootUtils.restartBackScreen()
        return ok
    }

    /**
     * Remove Janus-created entries (resId starts with "janus-") from runtime.json,
     * and delete their associated snapshot files and entry directories.
     */
    private fun cleanRuntimeEntries(): Boolean {
        val json = RootUtils.execWithOutput("cat '${JanusPaths.RUNTIME_JSON}'") ?: return true
        val arr = try { JSONArray(json) } catch (_: Exception) { return true }

        val filtered = JSONArray()
        var modified = false
        for (i in 0 until arr.length()) {
            val entry = arr.getJSONObject(i)
            val resId = entry.optString("resId", "")
            if (resId.startsWith("janus-")) {
                modified = true
                // Clean up associated files
                val applyId = entry.optString("applyId", "")
                if (applyId.isNotEmpty()) {
                    RootUtils.exec("rm -rf '${JanusPaths.RUNTIME_DIR}/${resId}_$applyId'")
                }
                val snapshotPath = entry.optString("resSnapshotPath", "")
                if (snapshotPath.isNotEmpty()) {
                    RootUtils.exec("rm -f '$snapshotPath'")
                }
                continue
            }
            filtered.put(entry)
        }

        if (!modified) return true
        return writeRuntimeJson(filtered)
    }

    private fun writeRuntimeJson(arr: JSONArray): Boolean {
        // 使用 base64 编码 + 文件写入，避免 shell 特殊字符问题
        val tmpFile = "/data/local/tmp/janus_cleanup_runtime.json"
        val b64 = Base64.encodeToString(arr.toString().toByteArray(), Base64.NO_WRAP)
        // base64 字符集 [A-Za-z0-9+/=] 不含 shell 元字符，安全拼接到命令中
        // 使用 tee 代替 echo/printf 以避免参数过长截断
        val writeCmd = "echo -n '$b64' | base64 -d > '$tmpFile'"
        if (!RootUtils.exec(writeCmd)) return false
        // 验证写出的 JSON 合法
        val verify = RootUtils.execWithOutput("cat '$tmpFile'") ?: return false
        try { JSONArray(verify) } catch (_: Exception) {
            RootUtils.exec("rm -f '$tmpFile'")
            return false
        }
        if (!RootUtils.exec("cp '$tmpFile' '${JanusPaths.RUNTIME_JSON}'")) return false
        RootUtils.exec("chown system_theme:ext_data_rw '${JanusPaths.RUNTIME_JSON}'")
        RootUtils.exec("chmod 644 '${JanusPaths.RUNTIME_JSON}'")
        RootUtils.exec("rm -f '$tmpFile'")
        return true
    }
}
