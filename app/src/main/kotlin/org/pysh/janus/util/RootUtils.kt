package org.pysh.janus.util

object RootUtils {

    fun hasRoot(): Boolean = exec("id")

    fun restartBackScreen(): Boolean =
        exec("am force-stop com.xiaomi.subscreencenter")

    fun exec(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    fun execWithOutput(command: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            // 先读完 stdout 再 waitFor，避免缓冲区满导致死锁
            val output = process.inputStream.bufferedReader().use { it.readText().trim() }
            process.errorStream.bufferedReader().use { it.readText() }
            if (process.waitFor() == 0) output else null
        } catch (_: Exception) {
            null
        }
    }
}
