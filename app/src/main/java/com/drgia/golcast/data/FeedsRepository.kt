package com.drgia.golcast.data

import com.drgia.golcast.model.PodcastFeed

object FeedsRepository {
    val feeds: List<PodcastFeed> = listOf(
        PodcastFeed("A dos bandas",
            "https://www.omnycontent.com/d/playlist/7e594858-42ba-4ecb-86d8-ad1a002e5281/1d39fbf3-b41a-4c54-88ec-adcd0136c774/df3205fa-ee92-4df4-9308-adcd0136c791/podcast.rss",
            "https://www.omnycontent.com/d/playlist/7e594858-42ba-4ecb-86d8-ad1a002e5281/1d39fbf3-b41a-4c54-88ec-adcd0136c774/df3205fa-ee92-4df4-9308-adcd0136c791/image.jpg?t=1729520499&size=Large"),
        PodcastFeed("Brazalete Negro",
            "https://www.primaverasound.com/radio/shows/brazalete-negro?action=rss",
            "https://assets-img.primaverasound.com/1400x1400/2022/ps-single/images/noticias/2022/psb/facebook/brazaletenegro_ed_2400x1256_20220516085356.jpg"),
        PodcastFeed("El Larguero",
            "https://fapi-top.prisasd.com/podcast/playser/el_larguero/itunestfp/podcast.xml",
            "https://sdmedia.playser.cadenaser.com/playser/image/20243/21/1711022132571_246.jpeg"),
        PodcastFeed("El Morning de Axel y Rulo",
            "https://www.ivoox.com/feed_fg_f1305937_filtro_1.xml",
            "https://static-1.ivoox.com/canales/8/a/b/1/8ab19414779a2ef30569006334fb11b4_XXL.jpg"),
        PodcastFeed("El Partidazo de COPE",
            "https://www.cope.es/api/es/programas/el-partidazo-de-cope/audios/rss.xml",
            "https://imagenes.cope.es/uploads/2024/07/03/original_668565c4a89c2.jpeg"),
        PodcastFeed("La Libreta de Van Gaal",
            "https://feeds.megaphone.fm/LALIBRETA2492905101",
            "https://megaphone.imgix.net/podcasts/128c35b8-42b3-11ef-9652-43c1220d92c4/image/55fac97bcbc48537a86ffd8390c3772f.jpeg"),
        PodcastFeed("La Pizarra de Quintana",
            "https://www.omnycontent.com/d/playlist/4ac2c3a8-bb0c-499b-bf22-ac8400df1983/23858072-d78f-4b2c-870e-ad95016caf18/272fdbf7-9428-4781-92e9-ad95016eb929/podcast.rss",
            "https://www.omnycontent.com/d/playlist/4ac2c3a8-bb0c-499b-bf22-ac8400df1983/23858072-d78f-4b2c-870e-ad95016caf18/272fdbf7-9428-4781-92e9-ad95016eb929/image.jpg"),
        PodcastFeed("La Tribu con Raúl Varela",
            "https://www.omnycontent.com/d/playlist/4ac2c3a8-bb0c-499b-bf22-ac8400df1983/7345e2c8-3edf-4530-b882-ac8700b152e1/a6c99e71-a9ce-48d3-97d9-acaa0101581c/podcast.rss",
            "https://www.omnycontent.com/d/playlist/4ac2c3a8-bb0c-499b-bf22-ac8400df1983/7345e2c8-3edf-4530-b882-ac8700b152e1/a6c99e71-a9ce-48d3-97d9-acaa0101581c/image.jpg"),
        PodcastFeed("Offsiders",
            "https://feeds.megaphone.fm/HOT2717170364",
            "https://megaphone.imgix.net/podcasts/6782cc76-780b-11ee-9313-9fce8c598dee/image/355dc2fb431dec68d91bae797c9410e1.png"),
        PodcastFeed("Tertulia Blanquinegra - Efesista APP",
            "https://www.ivoox.com/feed_fg_f1156320_filtro_1.xml",
            "https://static-2.ivoox.com/canales/7/3/5/9/7051641909537_XXL.jpg"),
        PodcastFeed("Tiempo de Juego",
            "https://www.cope.es/api/es/programas/tiempo-de-juego/audios/rss.xml",
            "https://imagenes.cope.es/uploads/2025/01/20/original_678e8defc69bc.jpeg"),
        PodcastFeed("¡El Chiringuito de Jugones!",
            "https://www.ivoox.com/feed_fg_f11226820_filtro_1.xml",
            "https://static-2.ivoox.com/canales/1/1/2/7/3351617207211_XXL.jpg")
    )
}
