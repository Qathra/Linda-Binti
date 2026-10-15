package com.lindabinti.backend.db

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class Database(jdbcUrl: String) {
    private val connection: Connection = DriverManager.getConnection(jdbcUrl).also { it.autoCommit = true }

    fun conn(): Connection = connection

    fun migrate() {
        conn().createStatement().use { st ->
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS users (
                  id TEXT PRIMARY KEY,
                  email TEXT NOT NULL UNIQUE,
                  password_hash TEXT NOT NULL,
                  display_name TEXT NOT NULL,
                  role TEXT NOT NULL,
                  created_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS reports (
                  server_id TEXT PRIMARY KEY,
                  user_id TEXT NOT NULL,
                  client_uuid TEXT NOT NULL,
                  category_id TEXT NOT NULL,
                  category_label TEXT NOT NULL,
                  body TEXT NOT NULL,
                  area_code TEXT NOT NULL DEFAULT '',
                  area_label TEXT NOT NULL DEFAULT '',
                  area_type TEXT NOT NULL DEFAULT '',
                  created_at INTEGER NOT NULL,
                  received_at INTEGER NOT NULL,
                  urgency TEXT NOT NULL DEFAULT 'MEDIUM',
                  explanation TEXT NOT NULL DEFAULT '',
                  classifier_source TEXT NOT NULL DEFAULT 'rules',
                  ai_status TEXT NOT NULL DEFAULT 'pending',
                  gbv_fgm_relevance TEXT NOT NULL DEFAULT '',
                  incident_category TEXT NOT NULL DEFAULT '',
                  urgency_indicators TEXT NOT NULL DEFAULT '',
                  safety_concerns TEXT NOT NULL DEFAULT '',
                  recommended_priority TEXT NOT NULL DEFAULT '',
                  case_status TEXT NOT NULL DEFAULT 'NEW',
                  UNIQUE(user_id, client_uuid)
                )
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS analytics_events (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  user_id TEXT NOT NULL,
                  event_type TEXT NOT NULL,
                  category_or_band TEXT NOT NULL,
                  week_label TEXT NOT NULL,
                  count INTEGER NOT NULL,
                  received_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS audit_log (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  actor_user_id TEXT NOT NULL,
                  action TEXT NOT NULL,
                  detail TEXT NOT NULL,
                  created_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS refresh_tokens (
                  token_id TEXT PRIMARY KEY,
                  user_id TEXT NOT NULL,
                  revoked INTEGER NOT NULL DEFAULT 0,
                  created_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
        addColumnIfMissing(
            "reports",
            "area_code",
            "ALTER TABLE reports ADD COLUMN area_code TEXT NOT NULL DEFAULT ''"
        )
        addColumnIfMissing(
            "reports",
            "area_label",
            "ALTER TABLE reports ADD COLUMN area_label TEXT NOT NULL DEFAULT ''"
        )
        addColumnIfMissing(
            "reports",
            "area_type",
            "ALTER TABLE reports ADD COLUMN area_type TEXT NOT NULL DEFAULT ''"
        )
        addColumnIfMissing(
            "reports",
            "urgency",
            "ALTER TABLE reports ADD COLUMN urgency TEXT NOT NULL DEFAULT 'elevated'"
        )
        addColumnIfMissing(
            "reports",
            "explanation",
            "ALTER TABLE reports ADD COLUMN explanation TEXT NOT NULL DEFAULT ''"
        )
        addColumnIfMissing(
            "reports",
            "classifier_source",
            "ALTER TABLE reports ADD COLUMN classifier_source TEXT NOT NULL DEFAULT 'rules'"
        )
        addColumnIfMissing(
            "reports",
            "ai_status",
            "ALTER TABLE reports ADD COLUMN ai_status TEXT NOT NULL DEFAULT 'pending'"
        )
        addColumnIfMissing(
            "reports",
            "gbv_fgm_relevance",
            "ALTER TABLE reports ADD COLUMN gbv_fgm_relevance TEXT NOT NULL DEFAULT ''"
        )
        addColumnIfMissing(
            "reports",
            "incident_category",
            "ALTER TABLE reports ADD COLUMN incident_category TEXT NOT NULL DEFAULT ''"
        )
        addColumnIfMissing(
            "reports",
            "urgency_indicators",
            "ALTER TABLE reports ADD COLUMN urgency_indicators TEXT NOT NULL DEFAULT ''"
        )
        addColumnIfMissing(
            "reports",
            "safety_concerns",
            "ALTER TABLE reports ADD COLUMN safety_concerns TEXT NOT NULL DEFAULT ''"
        )
        addColumnIfMissing(
            "reports",
            "recommended_priority",
            "ALTER TABLE reports ADD COLUMN recommended_priority TEXT NOT NULL DEFAULT ''"
        )
        addColumnIfMissing(
            "reports",
            "case_status",
            "ALTER TABLE reports ADD COLUMN case_status TEXT NOT NULL DEFAULT 'NEW'"
        )
        addColumnIfMissing(
            "users",
            "must_change_password",
            "ALTER TABLE users ADD COLUMN must_change_password INTEGER NOT NULL DEFAULT 0"
        )
        conn().createStatement().use { st ->
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS education_content (
                  id TEXT PRIMARY KEY,
                  content_type TEXT NOT NULL,
                  title TEXT NOT NULL,
                  description TEXT NOT NULL,
                  language TEXT NOT NULL,
                  category TEXT NOT NULL,
                  url_or_path TEXT NOT NULL DEFAULT '',
                  thumbnail_url TEXT NOT NULL DEFAULT '',
                  duration_seconds INTEGER NOT NULL DEFAULT 0,
                  active INTEGER NOT NULL DEFAULT 1,
                  sort_order INTEGER NOT NULL DEFAULT 0,
                  updated_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    private fun addColumnIfMissing(table: String, column: String, alterSql: String) {
        val existing = mutableSetOf<String>()
        conn().createStatement().use { st ->
            st.executeQuery("PRAGMA table_info($table)").use { rs ->
                while (rs.next()) existing += rs.getString("name")
            }
        }
        if (column !in existing) {
            conn().createStatement().use { it.executeUpdate(alterSql) }
        }
    }

    companion object {
        fun fileDb(path: String): Database {
            File(path).parentFile?.mkdirs()
            return Database("jdbc:sqlite:$path")
        }

        fun memory(): Database = Database("jdbc:sqlite::memory:")
    }
}