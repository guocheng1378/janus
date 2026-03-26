package org.pysh.janus.util

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object WallpaperUtils {

    private const val RUNTIME_JSON =
        "/data/system/theme_magic/users/0/rearScreen/runtime.json"
    private const val AI_MP4_ENTRY = "assets/ai/ai.mp4"
    private const val MANIFEST_ENTRY = "manifest.xml"
    private const val BACKUP_SUFFIX = ".janus_bak"
    private const val OWNER = "system_theme:ext_data_rw"

    data class WallpaperPaths(
        val localPath: String,
        val snapshotPath: String,
    )

    fun detectWallpaper(): WallpaperPaths? {
        val json = RootUtils.execWithOutput("cat '$RUNTIME_JSON'") ?: return null
        return try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val entry = arr.getJSONObject(i)
                if (entry.optString("resSubType", "") != "ai") continue
                val local = entry.getString("resLocalPath")
                val snapshot = entry.getString("resSnapshotPath")
                if (!RootUtils.exec("test -f '$snapshot'")) continue
                return WallpaperPaths(local, snapshot)
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun isLoopEnabled(context: Context): Boolean? {
        val paths = detectWallpaper() ?: return null
        val workZip = copyToCache(context, paths.snapshotPath, "check.zip") ?: return null
        return try {
            readManifestLoop(workZip)
        } finally {
            workZip.delete()
        }
    }

    fun replaceVideo(context: Context, videoUri: Uri, enableLoop: Boolean): Boolean {
        val paths = detectWallpaper() ?: return false
        val cacheDir = ensureCacheDir(context)

        try {
            val userVideo = File(cacheDir, "user_video.mp4")
            context.contentResolver.openInputStream(videoUri)?.use { input ->
                userVideo.outputStream().use { output -> input.copyTo(output) }
            } ?: return false

            backupIfNeeded(paths.snapshotPath)

            val workZip = copyToCache(context, paths.snapshotPath, "work.zip") ?: return false
            val resultZip = File(cacheDir, "result.zip")

            rebuildZip(workZip, resultZip, userVideo, enableLoop)

            return writeBack(resultZip, paths)
        } finally {
            cacheDir.deleteRecursively()
        }
    }

    fun setLoop(context: Context, enabled: Boolean): Boolean {
        val paths = detectWallpaper() ?: return false
        val cacheDir = ensureCacheDir(context)

        try {
            val workZip = copyToCache(context, paths.snapshotPath, "work.zip") ?: return false
            val resultZip = File(cacheDir, "result.zip")

            rebuildZipLoopOnly(workZip, resultZip, enabled)

            return writeBack(resultZip, paths)
        } finally {
            cacheDir.deleteRecursively()
        }
    }

    fun restoreBackup(): Boolean {
        val paths = detectWallpaper() ?: return false
        val backup = "${paths.snapshotPath}$BACKUP_SUFFIX"
        if (!RootUtils.exec("test -f '$backup'")) return false
        val ok = RootUtils.exec("cp '$backup' '${paths.snapshotPath}'") &&
            RootUtils.exec("cp '$backup' '${paths.localPath}'")
        if (ok) {
            fixOwnership(paths.snapshotPath)
            fixOwnership(paths.localPath)
            RootUtils.restartBackScreen()
        }
        return ok
    }

    fun hasBackup(): Boolean {
        val paths = detectWallpaper() ?: return false
        return RootUtils.exec("test -f '${paths.snapshotPath}$BACKUP_SUFFIX'")
    }

    private fun ensureCacheDir(context: Context): File {
        val dir = File(context.cacheDir, "wallpaper_work")
        dir.mkdirs()
        return dir
    }

    private fun copyToCache(context: Context, remotePath: String, name: String): File? {
        val cacheFile = File(ensureCacheDir(context), name)
        if (!RootUtils.exec("cp '$remotePath' '${cacheFile.absolutePath}'")) return null
        if (!RootUtils.exec("chmod 644 '${cacheFile.absolutePath}'")) return null
        return if (cacheFile.exists() && cacheFile.length() > 0) cacheFile else null
    }

    private fun backupIfNeeded(snapshotPath: String) {
        val backup = "$snapshotPath$BACKUP_SUFFIX"
        if (!RootUtils.exec("test -f '$backup'")) {
            RootUtils.exec("cp '$snapshotPath' '$backup'")
            fixOwnership(backup)
        }
    }

    private fun writeBack(resultZip: File, paths: WallpaperPaths): Boolean {
        val src = resultZip.absolutePath
        // 快照是关键路径（subscreencenter 实际读取），必须成功
        if (!RootUtils.exec("cp '$src' '${paths.snapshotPath}'")) return false
        fixOwnership(paths.snapshotPath)
        // localPath 是同步副本，失败不影响生效
        RootUtils.exec("cp '$src' '${paths.localPath}'")
        fixOwnership(paths.localPath)
        RootUtils.restartBackScreen()
        return true
    }

    private fun fixOwnership(path: String) {
        RootUtils.exec("chown $OWNER '$path'")
        RootUtils.exec("chmod 775 '$path'")
    }

    /** 创建 STORED（不压缩）的 ZipEntry，需要预计算 size 和 CRC32 */
    private fun storedEntry(name: String, file: File): ZipEntry {
        val crc = CRC32()
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            var len: Int
            while (input.read(buf).also { len = it } != -1) {
                crc.update(buf, 0, len)
            }
        }
        return ZipEntry(name).apply {
            method = ZipEntry.STORED
            size = file.length()
            compressedSize = file.length()
            setCrc(crc.value)
        }
    }

    private fun readManifestLoop(zipFile: File): Boolean {
        val zip = ZipFile(zipFile)
        val entry = zip.getEntry(MANIFEST_ENTRY) ?: run { zip.close(); return false }
        val content = zip.getInputStream(entry).bufferedReader().readText()
        zip.close()
        val regex = Regex("""loop="(\d+)"""")
        val lastMatch = regex.findAll(content).lastOrNull()
        return lastMatch?.groupValues?.get(1) == "1"
    }

    private fun modifyLoop(content: String, enabled: Boolean): String {
        val target = if (enabled) """loop="0"""" else """loop="1""""
        val replacement = if (enabled) """loop="1"""" else """loop="0""""
        val lastIndex = content.lastIndexOf(target)
        if (lastIndex < 0) return content
        return content.substring(0, lastIndex) +
            replacement +
            content.substring(lastIndex + target.length)
    }

    private fun rebuildZip(
        source: File,
        dest: File,
        newVideo: File,
        enableLoop: Boolean,
    ) {
        val zip = ZipFile(source)
        ZipOutputStream(FileOutputStream(dest)).use { zos ->
            for (entry in zip.entries()) {
                when (entry.name) {
                    AI_MP4_ENTRY -> {
                        zos.putNextEntry(storedEntry(AI_MP4_ENTRY, newVideo))
                        newVideo.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                    MANIFEST_ENTRY -> {
                        val content = zip.getInputStream(entry).bufferedReader().readText()
                        val modified = modifyLoop(content, enableLoop)
                        zos.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                        zos.write(modified.toByteArray())
                        zos.closeEntry()
                    }
                    else -> {
                        zos.putNextEntry(ZipEntry(entry.name))
                        if (!entry.isDirectory) {
                            zip.getInputStream(entry).use { it.copyTo(zos) }
                        }
                        zos.closeEntry()
                    }
                }
            }
        }
        zip.close()
    }

    private fun rebuildZipLoopOnly(source: File, dest: File, enableLoop: Boolean) {
        val zip = ZipFile(source)
        ZipOutputStream(FileOutputStream(dest)).use { zos ->
            for (entry in zip.entries()) {
                if (entry.name == MANIFEST_ENTRY) {
                    val content = zip.getInputStream(entry).bufferedReader().readText()
                    val modified = modifyLoop(content, enableLoop)
                    zos.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                    zos.write(modified.toByteArray())
                    zos.closeEntry()
                } else {
                    zos.putNextEntry(ZipEntry(entry.name))
                    if (!entry.isDirectory) {
                        zip.getInputStream(entry).use { it.copyTo(zos) }
                    }
                    zos.closeEntry()
                }
            }
        }
        zip.close()
    }
}
