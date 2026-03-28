package org.pysh.janus.hook

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Generic lyric injector for the rear screen.
 *
 * Writes the current lyric line into MediaMetadata's TITLE field so that
 * subscreencenter's music template displays it. This is the same mechanism
 * used by QQ Music (Bluetooth lyrics) and 汽水音乐.
 *
 * Subclasses only need to:
 * 1. Call [hookLyricSource] to install app-specific hooks that capture lyrics
 * 2. Call [setLyrics] when timed lyrics become available
 *
 * The base class handles MediaSession hooks, scheduling, song-change
 * detection, and title replacement automatically.
 */
abstract class LyricInjector(protected val tag: String) {

    data class TimedLine(val beginMs: Int, val endMs: Int, val text: String)

    @Volatile
    var lyrics: List<TimedLine> = emptyList()
        private set

    @Volatile
    var translations: Map<Int, String> = emptyMap()
        private set

    @Volatile
    private var originalTitle: String? = null

    @Volatile
    private var currentSongKey: String? = null

    @Volatile
    private var lastLyricLine: String? = null

    private var mediaSessionRef: MediaSession? = null
    private var controllerRef: MediaController? = null
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var updaterRunning = false

    // ── Public API for subclasses ─────────────────────────────────────

    fun hook(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookSetMetadata()
        hookPlaybackState()
        hookLyricSource(lpparam)
        XposedBridge.log("[$tag] Hooks installed")
    }

    /** Override to install app-specific hooks that capture lyrics. */
    protected abstract fun hookLyricSource(lpparam: XC_LoadPackage.LoadPackageParam)

    /** Call from subclass when timed lyrics are available. */
    protected fun setLyrics(
        lines: List<TimedLine>,
        trans: Map<Int, String> = emptyMap()
    ) {
        lyrics = lines
        translations = trans
        XposedBridge.log("[$tag] Loaded ${lines.size} lines, ${trans.size} translations")
        if (lines.isNotEmpty()) startLyricUpdater()
    }

    // ── MediaSession hooks (generic) ──────────────────────────────────

    private fun hookSetMetadata() {
        try {
            XposedHelpers.findAndHookMethod(
                MediaSession::class.java, "setMetadata",
                MediaMetadata::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val metadata = param.args[0] as? MediaMetadata ?: return
                        val session = param.thisObject as MediaSession

                        if (mediaSessionRef !== session) {
                            mediaSessionRef = session
                            controllerRef = session.controller
                        }

                        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)

                        // Detect song change → clear stale lyrics
                        if (title != null && title != lastLyricLine) {
                            val songKey = "$title|$artist"
                            if (songKey != currentSongKey) {
                                currentSongKey = songKey
                                lyrics = emptyList()
                                translations = emptyMap()
                                lastLyricLine = null
                                stopLyricUpdater()
                            }
                            originalTitle = title
                        }

                        // If lyrics are active, overwrite title with current lyric
                        if (lyrics.isNotEmpty() && lastLyricLine != null) {
                            try {
                                val bundle = XposedHelpers.getObjectField(metadata, "mBundle")
                                        as android.os.Bundle
                                bundle.putString(MediaMetadata.METADATA_KEY_TITLE, lastLyricLine)
                                bundle.putString(
                                    "android.media.metadata.CUSTOM_FIELD_TITLE", lastLyricLine)
                            } catch (_: Throwable) { }
                        }

                        if (lyrics.isNotEmpty()) startLyricUpdater()
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("[$tag] hookSetMetadata failed: ${e.message}")
        }
    }

    private fun hookPlaybackState() {
        try {
            XposedHelpers.findAndHookMethod(
                MediaSession::class.java, "setPlaybackState",
                PlaybackState::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val state = param.args[0] as? PlaybackState ?: return
                        when (state.state) {
                            PlaybackState.STATE_PLAYING,
                            PlaybackState.STATE_BUFFERING,
                            PlaybackState.STATE_FAST_FORWARDING,
                            PlaybackState.STATE_REWINDING -> {
                                startLyricUpdater()
                                if (lyrics.isNotEmpty()) {
                                    handler.post { scheduleNextUpdate() }
                                }
                            }
                            else -> stopLyricUpdater()
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("[$tag] hookPlaybackState failed: ${e.message}")
        }
    }

    // ── Lyric updater ─────────────────────────────────────────────────

    private val updateRunnable = Runnable {
        if (updaterRunning) scheduleNextUpdate()
    }

    private fun startLyricUpdater() {
        if (updaterRunning) return
        if (lyrics.isEmpty()) return
        updaterRunning = true
        handler.post { scheduleNextUpdate() }
    }

    private fun stopLyricUpdater() {
        updaterRunning = false
        handler.removeCallbacks(updateRunnable)
    }

    private fun scheduleNextUpdate() {
        if (!updaterRunning) return
        val controller = controllerRef ?: return
        val currentLyrics = lyrics
        if (currentLyrics.isEmpty()) return

        val playbackState = controller.playbackState ?: return
        val position = getPosition(playbackState)

        val idx = findLyricIndex(currentLyrics, position)
        val line = if (idx >= 0) currentLyrics[idx] else null
        val trans = if (line != null) translations[line.beginMs] else null
        val lyricText = when {
            line == null -> originalTitle
            trans != null -> "${line.text}\n${trans}"
            else -> line.text
        }

        // Calculate marquee speed for this line so scrolling completes in sync
        val nextIdx = idx + 1
        val nextBeginMs = if (nextIdx in currentLyrics.indices)
            currentLyrics[nextIdx].beginMs.toLong() else -1L
        val lineDurationMs = if (nextBeginMs > 0 && line != null)
            nextBeginMs - line.beginMs else 0L
        val marqueeSpeed = calculateMarqueeSpeed(line?.text, lineDurationMs)

        if (lyricText != lastLyricLine) {
            lastLyricLine = lyricText
            val session = mediaSessionRef ?: return
            val metadata = controller.metadata
            if (metadata != null) try {
                val bundle = XposedHelpers.getObjectField(metadata, "mBundle")
                        as android.os.Bundle
                bundle.putString(MediaMetadata.METADATA_KEY_TITLE, lyricText)
                bundle.putString("android.media.metadata.CUSTOM_FIELD_TITLE", lyricText)
                bundle.putInt(MARQUEE_SPEED_KEY, marqueeSpeed)
                session.setMetadata(metadata)
            } catch (_: Throwable) { }
        }

        val delayMs = if (nextBeginMs > 0) {
            (nextBeginMs - getPosition(playbackState)).coerceIn(200, 10_000)
        } else {
            2000L
        }
        handler.postDelayed(updateRunnable, delayMs)
    }

    /**
     * Calculate MAML marqueeSpeed so that text overflow scrolls to completion
     * within the lyric line duration (minus a small buffer).
     *
     * Returns 0 if the text fits or no scrolling is needed.
     */
    private fun calculateMarqueeSpeed(text: String?, durationMs: Long): Int {
        if (text == null || durationMs <= 0) return 0
        // Estimate text width in sr-units (CJK ≈ fontSize, ASCII ≈ 0.55 * fontSize)
        val textWidthSr = text.sumOf { ch ->
            if (ch.code > 0x7F) MamlConstants.FONT_SIZE_SR else MamlConstants.FONT_SIZE_SR * 0.55
        }
        val overflowSr = textWidthSr - MamlConstants.ELEM_WIDTH_SR
        if (overflowSr <= 0) return 0
        // Convert to MAML pixels and include 50px initial offset
        val totalScrollMaml = (overflowSr * MamlConstants.SR) + MamlConstants.MARQUEE_START_OFFSET
        // Complete 200ms before next line so end state is briefly visible
        val effectiveDurationSec = (durationMs - 200).coerceAtLeast(200) / 1000.0
        return (totalScrollMaml / effectiveDurationSec).toInt().coerceIn(1, 1000)
    }

    companion object {
        /** Metadata key for passing calculated marquee speed to subscreencenter. */
        const val MARQUEE_SPEED_KEY = "janus.lyric.marquee_speed"
    }

    private fun getPosition(state: PlaybackState): Long {
        return state.position +
                ((android.os.SystemClock.elapsedRealtime() - state.lastPositionUpdateTime)
                        * state.playbackSpeed).toLong()
    }

    private fun findLyricIndex(lines: List<TimedLine>, positionMs: Long): Int {
        var lo = 0
        var hi = lines.size - 1
        var result = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (lines[mid].beginMs <= positionMs) {
                result = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        if (result >= 0) {
            val line = lines[result]
            if (line.endMs > 0 && positionMs > line.endMs) return -1
        }
        return result
    }
}
