package com.example.lindabinti.data.content

/**
 * Curated YouTube awareness clip for an education topic page.
 * Streaming needs internet; offline lesson text does not depend on this.
 */
data class EducationYoutubeVideo(
    val id: String,
    val youtubeId: String,
    val title: String,
    val description: String
) {
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$youtubeId"
    val embedUrl: String get() = "https://www.youtube.com/embed/$youtubeId"
    val thumbnailUrl: String get() = "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg"
}

/** Shown on the existing FGM education topic (`Understanding FGM`). */
object FgmAwarenessVideos {
    val all: List<EducationYoutubeVideo> = listOf(
        EducationYoutubeVideo(
            id = "jane_story_kajiado",
            youtubeId = "FtmMXMWd0z8",
            title = "Jane's Story: The Fight to End FGM/C in Kajiado, Kenya",
            description = "A Kajiado story on ending FGM/C and protecting girls in the community."
        ),
        EducationYoutubeVideo(
            id = "fgm_awareness_qvt",
            youtubeId = "QVtUmPcpHPg",
            title = "FGM Awareness",
            description = "Awareness film supporting efforts to end FGM and keep girls safe."
        ),
        EducationYoutubeVideo(
            id = "fgm_awareness_clf",
            youtubeId = "cLFrObdiva8",
            title = "Ending FGM — Awareness Film",
            description = "Community awareness on why FGM must end and where to seek help."
        )
    )
}

/** Shown on the existing GBV education topic (`Understanding GBV`). */
object GbvAwarenessVideos {
    val all: List<EducationYoutubeVideo> = listOf(
        EducationYoutubeVideo(
            id = "kiserian_kajiado_gbv",
            youtubeId = "o-A_HczLWvI",
            title = "Community in Kiserian, Kajiado sensitized against gender-based violence",
            description = "GBV community sensitization in Kiserian, Kajiado County."
        ),
        EducationYoutubeVideo(
            id = "gbv_impact_kajiado",
            youtubeId = "r-161cME11E",
            title = "GBV Impact Stories - Kajiado County",
            description = "Impact stories on gender-based violence from Kajiado County."
        )
    )
}
