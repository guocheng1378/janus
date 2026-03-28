package org.pysh.janus.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * Apple Music lyric provider.
 *
 * Captures TTML lyrics from Apple Music's native TTMLParser and feeds
 * timed lines to [LyricInjector] for rear-screen display.
 */
object AppleMusicLyricHook : LyricInjector("Janus-AppleMusic") {

    override fun hookLyricSource(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cls = XposedHelpers.findClass(
                "com.apple.android.music.ttml.javanative.TTMLParser\$TTMLParserNative",
                lpparam.classLoader
            )
            XposedHelpers.findAndHookMethod(
                cls, "songInfoFromTTML", String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val ttml = param.args[0] as? String ?: return
                        try {
                            val (lines, trans) = parseTtml(ttml)
                            setLyrics(lines, trans)
                        } catch (e: Throwable) {
                            XposedBridge.log("[Janus-AppleMusic] TTML parse failed: ${e.message}")
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("[Janus-AppleMusic] hookTtmlParser failed: ${e.message}")
        }
    }

    // ── TTML parsing ──────────────────────────────────────────────────

    private fun parseTtml(ttml: String): Pair<List<TimedLine>, Map<Int, String>> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(StringReader(ttml))

        var primaryLang: String? = null
        val divGroups = mutableListOf<Pair<String?, MutableList<TimedLine>>>()
        var currentDivLang: String? = null
        var currentDivLines: MutableList<TimedLine>? = null

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "tt" -> primaryLang = getXmlLang(parser)
                    "div" -> {
                        currentDivLang = getXmlLang(parser)
                        currentDivLines = mutableListOf()
                    }
                    "p" -> {
                        val begin = parser.getAttributeValue(null, "begin")
                        val end = parser.getAttributeValue(null, "end")
                        if (begin != null && currentDivLines != null) {
                            val beginMs = parseTime(begin)
                            val endMs = if (end != null) parseTime(end) else -1
                            val text = collectText(parser, "p")
                            if (text.isNotBlank() && beginMs >= 0) {
                                currentDivLines.add(TimedLine(beginMs, endMs, text.trim()))
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "div" -> {
                        if (currentDivLines != null && currentDivLines.isNotEmpty()) {
                            divGroups.add(currentDivLang to currentDivLines)
                        }
                        currentDivLines = null
                        currentDivLang = null
                    }
                }
            }
            parser.next()
        }

        if (divGroups.isEmpty()) return emptyList<TimedLine>() to emptyMap()

        // Merge divs by language — same lang = song sections, not translations
        val byLang = mutableMapOf<String?, MutableList<TimedLine>>()
        for ((lang, lines) in divGroups) {
            byLang.getOrPut(lang) { mutableListOf() }.addAll(lines)
        }

        val originalLang = when {
            primaryLang != null && primaryLang in byLang -> primaryLang
            null in byLang -> null
            else -> byLang.keys.first()
        }
        val originalLines = (byLang.remove(originalLang) ?: emptyList()).sortedBy { it.beginMs }

        val transMap = mutableMapOf<Int, String>()
        for ((_, lines) in byLang) {
            for (line in lines) {
                transMap[line.beginMs] = line.text
            }
        }

        return originalLines to transMap
    }

    private fun collectText(parser: XmlPullParser, endTag: String): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            parser.next()
            when (parser.eventType) {
                XmlPullParser.TEXT -> sb.append(parser.text)
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> {
                    depth--
                    if (depth == 0 && parser.name == endTag) break
                }
                XmlPullParser.END_DOCUMENT -> break
            }
        }
        return sb.toString()
    }

    private fun getXmlLang(parser: XmlPullParser): String? {
        return parser.getAttributeValue("http://www.w3.org/XML/1998/namespace", "lang")
            ?: parser.getAttributeValue(null, "xml:lang")
    }

    private fun parseTime(time: String): Int {
        val trimmed = time.trim()
        if (trimmed.endsWith("s", ignoreCase = true)) {
            val secs = trimmed.dropLast(1).toDoubleOrNull() ?: return -1
            return (secs * 1000).toInt()
        }
        val parts = trimmed.split(':')
        return when (parts.size) {
            3 -> {
                val h = parts[0].toIntOrNull() ?: return -1
                val m = parts[1].toIntOrNull() ?: return -1
                val s = parts[2].toDoubleOrNull() ?: return -1
                ((h * 3600 + m * 60) * 1000 + (s * 1000)).toInt()
            }
            2 -> {
                val m = parts[0].toIntOrNull() ?: return -1
                val s = parts[1].toDoubleOrNull() ?: return -1
                (m * 60_000 + (s * 1000)).toInt()
            }
            1 -> {
                val s = parts[0].toDoubleOrNull() ?: return -1
                (s * 1000).toInt()
            }
            else -> -1
        }
    }
}
