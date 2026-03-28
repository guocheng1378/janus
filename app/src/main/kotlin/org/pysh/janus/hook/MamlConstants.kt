package org.pysh.janus.hook

/**
 * Shared constants for MAML template marquee speed calculation.
 * Used by both [LyricInjector] and [MusicTemplatePatch].
 */
object MamlConstants {
    /** Screen width ratio: screenWidth(1080) / sr-base(334). */
    const val SR = 1080.0 / 334.0
    /** Font size in sr-units for the album_name_text element. */
    const val FONT_SIZE_SR = 20.0
    /** Element width in sr-units for the album_name_text element. */
    const val ELEM_WIDTH_SR = 173.0
    /** MAML marquee initial x-offset in pixels. */
    const val MARQUEE_START_OFFSET = 50.0

    /**
     * Read an integer value from a flag file.
     * Returns [default] if the file doesn't exist, is empty, or isn't a valid integer.
     */
    fun readIntFlag(path: String, default: Int): Int = try {
        val f = java.io.File(path)
        if (!f.exists()) default else f.readText().trim().toIntOrNull() ?: default
    } catch (_: Throwable) {
        default
    }
}
