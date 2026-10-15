package com.lindabinti.backend.services

import com.lindabinti.backend.db.Database
import java.util.UUID

data class EducationContentItem(
    val id: String,
    val contentType: String,
    val title: String,
    val description: String,
    val language: String,
    val category: String,
    val urlOrPath: String,
    val thumbnailUrl: String,
    val durationSeconds: Int,
    val active: Boolean,
    val sortOrder: Int,
    val updatedAt: Long
)

data class EducationContentWrite(
    val contentType: String,
    val title: String,
    val description: String,
    val language: String,
    val category: String,
    val urlOrPath: String,
    val thumbnailUrl: String = "",
    val durationSeconds: Int = 0,
    val active: Boolean = true,
    val sortOrder: Int = 0
)

/**
 * Curated education catalog (text + video metadata). Streaming URLs are external demo samples —
 * non-graphic. Seeded once for panel demos; NGO admins can CRUD via API.
 */
class EducationContentService(
    private val db: Database,
    private val audit: AuditService
) {
    fun seedDemoIfEmpty() {
        if (demoSeedDisabled()) {
            println("Education seed skipped (LINDA_DEMO_SEED=0). Publish videos via NGO dashboard CMS.")
            return
        }
        if (countAll() > 0) {
            // Keep catalog titles/URLs current even on existing databases.
            DEMO_SEED.forEach { (id, write) ->
                upsert(id, write, actorUserId = "system", seed = true)
            }
            COMMUNITY_SEED.forEach { (id, write) ->
                upsert(id, write, actorUserId = "system", seed = true)
            }
            return
        }
        DEMO_SEED.forEach { (id, write) ->
            upsert(id, write, actorUserId = "system", seed = true)
        }
        COMMUNITY_SEED.forEach { (id, write) ->
            upsert(id, write, actorUserId = "system", seed = true)
        }
        audit.log("system", "SEED_EDUCATION", "count=${DEMO_SEED.size + COMMUNITY_SEED.size}")
    }

    private fun demoSeedDisabled(): Boolean {
        val flag = com.lindabinti.backend.EnvFileLoader.get("LINDA_DEMO_SEED")?.trim().orEmpty()
        return flag.equals("0", ignoreCase = true) || flag.equals("false", ignoreCase = true)
    }

    fun list(
        language: String? = null,
        category: String? = null,
        type: String? = null,
        includeInactive: Boolean = false
    ): List<EducationContentItem> {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<Any>()
        if (!includeInactive) {
            clauses += "active = 1"
        }
        if (!language.isNullOrBlank()) {
            clauses += "language = ?"
            args += language.trim().lowercase().take(8)
        }
        if (!category.isNullOrBlank()) {
            clauses += "category = ?"
            args += category.trim().lowercase()
        }
        if (!type.isNullOrBlank()) {
            clauses += "content_type = ?"
            args += type.trim().lowercase()
        }
        val where = if (clauses.isEmpty()) "" else "WHERE " + clauses.joinToString(" AND ")
        val sql = """
            SELECT id, content_type, title, description, language, category, url_or_path,
                   thumbnail_url, duration_seconds, active, sort_order, updated_at
            FROM education_content
            $where
            ORDER BY sort_order ASC, updated_at DESC
        """.trimIndent()
        return db.conn().prepareStatement(sql).use { ps ->
            args.forEachIndexed { i, v -> ps.setObject(i + 1, v) }
            ps.executeQuery().use { rs ->
                val out = mutableListOf<EducationContentItem>()
                while (rs.next()) out += readRow(rs)
                out
            }
        }
    }

    fun getById(id: String): EducationContentItem? =
        db.conn().prepareStatement(
            """
            SELECT id, content_type, title, description, language, category, url_or_path,
                   thumbnail_url, duration_seconds, active, sort_order, updated_at
            FROM education_content WHERE id = ? LIMIT 1
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, id)
            ps.executeQuery().use { rs -> if (rs.next()) readRow(rs) else null }
        }

    fun create(write: EducationContentWrite, actorUserId: String): EducationContentItem {
        val id = "edu_" + UUID.randomUUID().toString().replace("-", "").take(12)
        return upsert(id, write, actorUserId)
    }

    fun update(id: String, write: EducationContentWrite, actorUserId: String): EducationContentItem {
        require(getById(id) != null) { "not_found" }
        return upsert(id, write, actorUserId)
    }

    fun setActive(id: String, active: Boolean, actorUserId: String): EducationContentItem {
        val existing = getById(id) ?: error("not_found")
        return upsert(
            id,
            EducationContentWrite(
                contentType = existing.contentType,
                title = existing.title,
                description = existing.description,
                language = existing.language,
                category = existing.category,
                urlOrPath = existing.urlOrPath,
                thumbnailUrl = existing.thumbnailUrl,
                durationSeconds = existing.durationSeconds,
                active = active,
                sortOrder = existing.sortOrder
            ),
            actorUserId
        )
    }

    private fun upsert(
        id: String,
        write: EducationContentWrite,
        actorUserId: String,
        seed: Boolean = false
    ): EducationContentItem {
        val type = write.contentType.trim().lowercase().ifBlank { "video" }
        require(type in setOf("text", "video")) { "invalid_type" }
        val title = write.title.trim()
        require(title.isNotBlank()) { "title_required" }
        val language = write.language.trim().lowercase().ifBlank { "en" }.take(8)
        val category = write.category.trim().lowercase().ifBlank { "general" }
        val url = write.urlOrPath.trim()
        if (type == "video") require(url.isNotBlank()) { "url_required" }
        val now = System.currentTimeMillis()
        db.conn().prepareStatement(
            """
            INSERT INTO education_content (
              id, content_type, title, description, language, category, url_or_path,
              thumbnail_url, duration_seconds, active, sort_order, updated_at
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET
              content_type=excluded.content_type,
              title=excluded.title,
              description=excluded.description,
              language=excluded.language,
              category=excluded.category,
              url_or_path=excluded.url_or_path,
              thumbnail_url=excluded.thumbnail_url,
              duration_seconds=excluded.duration_seconds,
              active=excluded.active,
              sort_order=excluded.sort_order,
              updated_at=excluded.updated_at
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, id)
            ps.setString(2, type)
            ps.setString(3, title)
            ps.setString(4, write.description.trim())
            ps.setString(5, language)
            ps.setString(6, category)
            ps.setString(7, url)
            ps.setString(8, write.thumbnailUrl.trim())
            ps.setInt(9, write.durationSeconds.coerceIn(0, 86_400))
            ps.setInt(10, if (write.active) 1 else 0)
            ps.setInt(11, write.sortOrder)
            ps.setLong(12, now)
            ps.executeUpdate()
        }
        if (!seed) {
            audit.log(actorUserId, "EDUCATION_UPSERT", "id=$id;type=$type;active=${write.active}")
        }
        return getById(id)!!
    }

    private fun countAll(): Int =
        db.conn().createStatement().use { st ->
            st.executeQuery("SELECT COUNT(*) AS c FROM education_content").use { rs ->
                if (rs.next()) rs.getInt("c") else 0
            }
        }

    private fun readRow(rs: java.sql.ResultSet) = EducationContentItem(
        id = rs.getString("id"),
        contentType = rs.getString("content_type"),
        title = rs.getString("title"),
        description = rs.getString("description"),
        language = rs.getString("language"),
        category = rs.getString("category"),
        urlOrPath = rs.getString("url_or_path"),
        thumbnailUrl = rs.getString("thumbnail_url"),
        durationSeconds = rs.getInt("duration_seconds"),
        active = rs.getInt("active") == 1,
        sortOrder = rs.getInt("sort_order"),
        updatedAt = rs.getLong("updated_at")
    )

    companion object {
        // Playable in-app MP4 streams (neutral sample media) with Kajiado education titles.
        private val DEMO_SEED: List<Pair<String, EducationContentWrite>> = listOf(
            "edu_demo_gbv" to EducationContentWrite(
                contentType = "video",
                title = "What is Gender-Based Violence?",
                description = "Learn the forms of GBV — physical, sexual, emotional, and economic — and why survivors in Kajiado deserve safety and support.",
                language = "en",
                category = "what_is_gbv",
                urlOrPath = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                durationSeconds = 15,
                sortOrder = 10
            ),
            "edu_demo_fgm" to EducationContentWrite(
                contentType = "video",
                title = "FGM is illegal — protect our girls",
                description = "FGM is prohibited in Kenya. Families and communities can choose education and dignity instead of harmful cutting.",
                language = "en",
                category = "what_is_fgm",
                urlOrPath = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                durationSeconds = 15,
                sortOrder = 20
            ),
            "edu_demo_help" to EducationContentWrite(
                contentType = "video",
                title = "Getting help in Kajiado",
                description = "If you or someone you know is at risk, call 999, 112, 1195, or 116. Use Emergency Assistance in Linda Binti.",
                language = "en",
                category = "how_to_get_help",
                urlOrPath = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                durationSeconds = 15,
                sortOrder = 30
            ),
            "edu_demo_safety_text" to EducationContentWrite(
                contentType = "text",
                title = "Safety planning reminder",
                description = "If you feel unsafe: go to a trusted place, call 1195 or 999, use Emergency Assistance in Linda Binti, and tell someone you trust.",
                language = "en",
                category = "safety_planning",
                urlOrPath = "",
                durationSeconds = 0,
                sortOrder = 40
            )
        )

        /**
         * Community awareness videos (open in YouTube / browser).
         * Replace these URLs in the NGO Education CMS with your own filmed Maasai/Kajiado community clips.
         */
        private val COMMUNITY_SEED: List<Pair<String, EducationContentWrite>> = listOf(
            "edu_community_fgm_voices" to EducationContentWrite(
                contentType = "video",
                title = "Community voices: Ending FGM together",
                description = "Public awareness film on ending FGM. Replace with a Kajiado/Maasai community recording via the NGO Education CMS when ready.",
                language = "en",
                category = "what_is_fgm",
                urlOrPath = "https://www.youtube.com/watch?v=Z7j8J9gq7bA",
                durationSeconds = 0,
                sortOrder = 5
            ),
            "edu_community_gbv_rights" to EducationContentWrite(
                contentType = "video",
                title = "Know your rights — GBV support",
                description = "Public awareness on violence against women and girls. Swap for local Kajiado messaging through the dashboard CMS.",
                language = "en",
                category = "what_is_gbv",
                urlOrPath = "https://www.youtube.com/watch?v=V1yW5IsnSjo",
                durationSeconds = 0,
                sortOrder = 6
            ),
            "edu_community_fgm_kenya" to EducationContentWrite(
                contentType = "video",
                title = "Kenya: Protecting girls from FGM",
                description = "National awareness on FGM prevention. Best used alongside local community dialogue in Kajiado.",
                language = "en",
                category = "what_is_fgm",
                urlOrPath = "https://www.youtube.com/watch?v=1Y6h1q4q0vE",
                durationSeconds = 0,
                sortOrder = 7
            )
        )
    }
}
