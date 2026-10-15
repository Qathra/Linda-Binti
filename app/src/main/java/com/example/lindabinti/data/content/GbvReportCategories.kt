package com.example.lindabinti.data.content

/**
 * Stable GBV/FGM report category ids used by the survivor reporting UI.
 * "other" means other GBV/FGM-related incident — not general crime.
 */
enum class GbvReportGroup {
    GBV,
    FGM,
    RELATED_PROTECTION,
    OTHER
}

data class GbvReportCategory(
    val id: String,
    val group: GbvReportGroup
)

object GbvReportCategories {
    val all = listOf(
        GbvReportCategory("physical", GbvReportGroup.GBV),
        GbvReportCategory("sexual", GbvReportGroup.GBV),
        GbvReportCategory("emotional", GbvReportGroup.GBV),
        GbvReportCategory("domestic", GbvReportGroup.GBV),
        GbvReportCategory("harassment", GbvReportGroup.GBV),
        GbvReportCategory("threats", GbvReportGroup.GBV),
        GbvReportCategory("sos", GbvReportGroup.RELATED_PROTECTION),
        GbvReportCategory("fgm", GbvReportGroup.FGM),
        GbvReportCategory("fgm_incident", GbvReportGroup.FGM),
        GbvReportCategory("fgm_pressure", GbvReportGroup.FGM),
        GbvReportCategory("forced_marriage", GbvReportGroup.RELATED_PROTECTION),
        GbvReportCategory("other", GbvReportGroup.OTHER)
    )

    val ids: List<String> = all.map { it.id }

    /** Categories survivors can pick in the reporting form (excludes system SOS flags). */
    val selectable: List<GbvReportCategory> = all.filter { it.id != "sos" }

    val grouped: List<Pair<GbvReportGroup, List<GbvReportCategory>>> = listOf(
        GbvReportGroup.GBV,
        GbvReportGroup.FGM,
        GbvReportGroup.RELATED_PROTECTION,
        GbvReportGroup.OTHER
    ).map { group -> group to selectable.filter { it.group == group } }

    fun isGbvFgmScope(categoryId: String): Boolean =
        categoryId.trim().lowercase() in ids

    /** Unknown or general-crime ids are stored as other GBV/FGM-related incident. */
    fun normalize(categoryId: String): String {
        val id = categoryId.trim().lowercase()
        return if (id in ids) id else "other"
    }
}
