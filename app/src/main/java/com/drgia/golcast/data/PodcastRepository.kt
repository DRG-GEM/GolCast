package com.drgia.golcast.data

import com.drgia.golcast.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PodcastRepository {

    fun getPodcasts(): List<Podcast> {
        return listOf(
            Podcast(name = "A dos bandas", rssUrl = "https://www.omnycontent.com/d/playlist/7e594858-42ba-4ecb-86d8-ad1a002e5281/1d39fbf3-b41a-4c54-88ec-adcd0136c774/df3205fa-ee92-4df4-9308-adcd0136c791/podcast.rss", artworkUrl = "https://www.omnycontent.com/d/playlist/7e594858-42ba-4ecb-86d8-ad1a002e5281/1d39fbf3-b41a-4c54-88ec-adcd0136c774/df3205fa-ee92-4df4-9308-adcd0136c791/image.jpg?t=1729520499&size=Large"),
            Podcast(name = "Brazalete Negro", rssUrl = "https://www.primaverasound.com/radio/shows/brazalete-negro?action=rss", artworkUrl = "https://assets-img.primaverasound.com/1400x1400/2022/ps-single/images/noticias/2022/psb/facebook/brazaletenegro_ed_2400x1256_20220516085356.jpg"),
            Podcast(name = "El Larguero", rssUrl = "https://fapi-top.prisasd.com/podcast/playser/el_larguero/itunestfp/podcast.xml", artworkUrl = "https://sdmedia.playser.cadenaser.com/playser/image/20243/21/1711022132571_246.jpeg"),
            Podcast(name = "El Morning de Axel y Rulo", rssUrl = "https://www.ivoox.com/feed_fg_f1305937_filtro_1.xml", artworkUrl = "https://static-1.ivoox.com/canales/8/a/b/1/8ab19414779a2ef30569006334fb11b4_XXL.jpg"),
            Podcast(name = "El Partidazo de COPE", rssUrl = "https://www.cope.es/api/es/programas/el-partidazo-de-cope/audios/rss.xml", artworkUrl = "https://imagenes.cope.es/uploads/2024/07/03/original_668565c4a89c2.jpeg"),
            Podcast(name = "La Libreta de Van Gaal", rssUrl = "https://feeds.megaphone.fm/LALIBRETA2492905101", artworkUrl = "https://megaphone.imgix.net/podcasts/128c35b8-42b3-11ef-9652-43c1220d92c4/image/55fac97bcbc48537a86ffd8390c3772f.jpeg?ixlib=rails-4.3.1&max-w=3000&max-h=3000&fit=crop&auto=format,compress"),
            Podcast(name = "La Pizarra de Quintana", rssUrl = "https://www.omnycontent.com/d/playlist/4ac2c3a8-bb0c-499b-bf22-ac8400df1983/23858072-d78f-4b2c-870e-ad95016caf18/272fdbf7-9428-4781-92e9-ad95016eb929/podcast.rss", artworkUrl = "https://www.omnycontent.com/d/playlist/4ac2c3a8-bb0c-499b-bf22-ac8400df1983/23858072-d78f-4b2c-870e-ad95016caf18/272fdbf7-9428-4781-92e9-ad95016eb929/image.jpg?t=1659636005&size=Large"),
            Podcast(name = "La Tribu con Raúl Varela", rssUrl = "https://www.omnycontent.com/d/playlist/4ac2c3a8-bb0c-499b-bf22-ac8400df1983/7345e2c8-3edf-4530-b882-ac8700b152e1/a6c99e71-a9ce-48d3-97d9-acaa0101581c/podcast.rss", artworkUrl = "https://www.omnycontent.com/d/playlist/4ac2c3a8-bb0c-499b-bf22-ac8400df1983/7345e2c8-3edf-4530-b882-ac8700b152e1/a6c99e71-a9ce-48d3-97d9-acaa0101581c/image.jpg?t=1610120517&size=Large"),
            Podcast(name = "Offsiders", rssUrl = "https://feeds.megaphone.fm/HOT2717170364", artworkUrl = "https://megaphone.imgix.net/podcasts/6782cc76-780b-11ee-9313-9fce8c598dee/image/355dc2fb431dec68d91bae797c9410e1.png?ixlib=rails-4.3.1&max-w=3000&max-h=3000&fit=crop&auto=format,compress"),
            Podcast(name = "Tertulia Blanquinegra - Efesista APP", rssUrl = "https://www.ivoox.com/feed_fg_f1156320_filtro_1.xml", artworkUrl = "https://static-2.ivoox.com/canales/7/3/5/9/7051641909537_XXL.jpg"),
            Podcast(name = "Tiempo de Juego", rssUrl = "https://www.cope.es/api/es/programas/tiempo-de-deportes/audios/rss.xml", artworkUrl = "https://imagenes.cope.es/uploads/2024/07/28/original_66a6557b42551.jpeg"),
            Podcast(name = "¡El Chiringuito de Jugones!", rssUrl = "https://www.ivoox.com/feed_fg_f11226820_filtro_1.xml", artworkUrl = "https://static-2.ivoox.com/canales/1/1/2/7/3351617207211_XXL.jpg")
        )
    }

    suspend fun fetchEpisodes(rssUrl: String): List<Episode> {
        return withContext(Dispatchers.IO) { // Ejecuta en un hilo secundario
            RssParser.parse(rssUrl)
        }
    }
}