package com.drgia.golcast.data

import com.drgia.golcast.model.Episode
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object RssParser {

    fun loadEpisodes(rssUrl: String): List<Episode> {
        val list = mutableListOf<Episode>()
        val conn = URL(rssUrl).openConnection().apply {
            connectTimeout = 10000
            readTimeout = 15000
        }
        conn.getInputStream().use { input ->
            val factory = XmlPullParserFactory.newInstance()
            val xpp = factory.newPullParser()
            xpp.setInput(input, "UTF-8")

            var event = xpp.eventType
            var insideItem = false
            var title: String? = null
            var audio: String? = null
            var art: String? = null
            var pub: Long = 0

            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        when (xpp.name.lowercase(Locale.ROOT)) {
                            "item", "entry" -> {
                                insideItem = true
                                title = null; audio = null; art = null; pub = 0
                            }
                            "title" -> if (insideItem && title == null) {
                                title = nextText(xpp)
                            }
                            "pubdate", "published", "updated" -> if (insideItem && pub == 0L) {
                                pub = parseDate(nextText(xpp))
                            }
                            "enclosure" -> if (insideItem && audio == null) {
                                audio = xpp.getAttributeValue(null, "url")
                            }
                            // artwork común
                            "itunes:image" -> if (insideItem && art == null) {
                                art = xpp.getAttributeValue(null, "href")
                            }
                            "image" -> if (insideItem && art == null) {
                                art = xpp.getAttributeValue(null, "href")
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if ((xpp.name.equals("item", true) || xpp.name.equals("entry", true)) && insideItem) {
                            if (!audio.isNullOrBlank()) {
                                list.add(Episode(title ?: "Episodio", audio!!, pub, art))
                            }
                            insideItem = false
                        }
                    }
                }
                event = xpp.next()
            }
        }
        return list.sortedByDescending { it.pubMillis }
    }

    private fun nextText(xpp: XmlPullParser): String {
        val n = xpp.next()
        val t = if (n == XmlPullParser.TEXT) xpp.text else ""
        if (n == XmlPullParser.TEXT) xpp.nextTag()
        return t
    }

    // intenta varios formatos típicos de RSS
    private fun parseDate(txt: String?): Long {
        if (txt.isNullOrBlank()) return 0
        val patterns = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.US)
                return sdf.parse(txt)?.time ?: 0
            } catch (_: Throwable) {}
        }
        return 0
    }
}
