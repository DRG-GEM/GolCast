package com.drgia.golcast

import com.drgia.golcast.data.Episode
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*

object RssParser {

    // Cliente único (reutilizable)
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    // Formatos de fecha comunes (RSS/Atom/iTunes)
    private val dateFormats: List<SimpleDateFormat> = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, dd MMM yyyy HH:mm Z",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    ).map {
        SimpleDateFormat(it, Locale.ENGLISH).apply { isLenient = true }
    }

    fun parse(rssUrl: String): List<Episode> {
        val episodes = mutableListOf<Episode>()

        try {
            val request = Request.Builder()
                .url(rssUrl)
                .header("User-Agent", "GolCast/1.0 (Android)")
                .build()

            // CERRAMOS la Response con use { }
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()

                // ✅ Forma correcta en OkHttp 4.x
                val xmlData = response.body?.string() ?: return emptyList()

                // Parse XML
                val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
                val parser = factory.newPullParser().apply { setInput(StringReader(xmlData)) }

                var event = parser.eventType

                var inItem = false
                var title: String? = null
                var audioUrl: String? = null
                var pubDateStr: String? = null

                // soportes extra
                var channelImage: String? = null
                var itemImage: String? = null

                while (event != XmlPullParser.END_DOCUMENT) {
                    val tag = parser.name?.lowercase(Locale.ROOT)

                    when (event) {
                        XmlPullParser.START_TAG -> {
                            when (tag) {
                                "item", "entry" -> {
                                    inItem = true
                                    title = null
                                    audioUrl = null
                                    pubDateStr = null
                                    itemImage = null
                                }
                                "title" -> if (inItem) title = parser.nextTextSafe()
                                "pubdate", "published", "updated" -> if (inItem) pubDateStr = parser.nextTextSafe()

                                // <enclosure url="..." type="audio/mpeg" />
                                "enclosure" -> if (inItem) {
                                    val type = parser.getAttributeValue(null, "type") ?: ""
                                    val url = parser.getAttributeValue(null, "url")
                                    if (!url.isNullOrBlank() && (type.contains("audio") || url.endsWith(".mp3", true) || url.endsWith(".m4a", true))) {
                                        audioUrl = url
                                    }
                                }

                                // Atom: <link rel="enclosure" type="audio/mpeg" href="..." />
                                "link" -> if (inItem && audioUrl == null) {
                                    val rel = parser.getAttributeValue(null, "rel") ?: ""
                                    val type = parser.getAttributeValue(null, "type") ?: ""
                                    val href = parser.getAttributeValue(null, "href")
                                    if (rel == "enclosure" && !href.isNullOrBlank() &&
                                        (type.contains("audio") || href.endsWith(".mp3", true) || href.endsWith(".m4a", true))
                                    ) {
                                        audioUrl = href
                                    }
                                }

                                // iTunes image a nivel de item o channel
                                "itunes:image", "image" -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    val url = parser.getAttributeValue(null, "url")
                                    if (inItem) itemImage = href ?: url
                                    else channelImage = href ?: url
                                }

                                // <image><url>...</url></image>
                                "url" -> if (!inItem && channelImage == null) {
                                    // solo si estamos dentro de <image>
                                    channelImage = parser.nextTextSafe()
                                }
                            }
                        }

                        XmlPullParser.END_TAG -> {
                            when (tag) {
                                "item", "entry" -> {
                                    val t = title?.takeIf { it.isNotBlank() } ?: "Sin título"
                                    val a = audioUrl?.takeIf { it.isNotBlank() }
                                    if (a != null) {
                                        val d = parseDate(pubDateStr) ?: Date()
                                        // ⚠️ Ajusta esta línea si tu Episode tiene otros campos/orden
                                        episodes += Episode(t, a, d)
                                    }
                                    inItem = false
                                }
                            }
                        }
                    }

                    event = parser.next()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }

        // más reciente → más antiguo
        return episodes.sortedByDescending { it.pubDate }
    }

    private fun parseDate(raw: String?): Date? {
        if (raw.isNullOrBlank()) return null
        for (f in dateFormats) {
            try { return f.parse(raw) } catch (_: Throwable) {}
        }
        return null
    }

    private fun XmlPullParser.nextTextSafe(): String =
        try { nextText() } catch (_: Throwable) { "" }
}
