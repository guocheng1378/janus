package org.pysh.janus.util

object RootUtils {

    fun hasRoot(): Boolean = exec("id")

    fun restartBackScreen(): Boolean =
        exec("am force-stop com.xiaomi.subscreencenter")

    fun exec(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            // 必须同时消费 stdout 和 stderr，防止管道缓冲区满导致死锁
            val stdout = Thread { process.inputStream.bufferedReader().use { it.readText() } }.apply { isDaemon = true }
            val stderr = Thread { process.errorStream.bufferedReader().use { it.readText() } }.apply { isDaemon = true }
            stdout.start()
            stderr.start()
            val exitCode = process.waitFor()
            stdout.join(5_000)
            stderr.join(5_000)
            if (exitCode != 0) {
                android.util.Log.e("Janus-Root", "exec failed ($exitCode): $command")
            }
            exitCode == 0
        } catch (e: Exception) {
            android.util.Log.e("Janus-Root", "exec exception: $command", e)
            false
        }
    }

    fun ensureDir(path: String): Boolean {
        if (!exec("mkdir -p '$path' && chmod 777 '$path'")) return false
        // Inherit parent's full SELinux context (preserves MCS categories like c512,c768).
        // Hardcoding s0 without MCS would strip categories and break access on strict devices.
        val parent = path.substringBeforeLast('/')
        exec("chcon --reference='$parent' '$path' 2>/dev/null || true")
        return true
    }

    fun execWithOutput(command: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            // 同时消费 stdout 和 stderr，防止缓冲区满导致死锁
            val outputRef = arrayOf("")
            val stdout = Thread {
                process.inputStream.bufferedReader().use { outputRef[0] = it.readText().trim() }
            }.apply { isDaemon = true }
            val stderr = Thread { process.errorStream.bufferedReader().use { it.readText() } }.apply { isDaemon = true }
            stdout.start()
            stderr.start()
            val exitCode = process.waitFor()
            stdout.join(5_000)
            stderr.join(5_000)
            if (exitCode == 0) outputRef[0] else null
        } catch (_: Exception) {
            null
        }
    }
}
