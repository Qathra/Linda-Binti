package com.example.lindabinti.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Explicit Room migrations — avoid wiping user SOS/contacts/reports on upgrade.
 * Version history:
 * - 1: trusted_contacts, sos_events, sos_deliveries
 * - 2: + reports, risk_assessments, education_progress
 * - 3: schema freeze (no structural change)
 * - 4: report sync fields (clientUuid, categoryId, server ids, attempts)
 * - 5: privacy-safe report area fields
 * - 6: persisted local AI analysis metadata
 */
object LindaMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS reports (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    category TEXT NOT NULL,
                    body TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    latitude REAL,
                    longitude REAL,
                    syncStatus TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS risk_assessments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    answersJson TEXT NOT NULL,
                    score INTEGER NOT NULL,
                    band TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS education_progress (
                    moduleId TEXT NOT NULL PRIMARY KEY,
                    completedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Intentionally empty — documents that v2→v3 preserves all tables.
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE reports ADD COLUMN clientUuid TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE reports ADD COLUMN categoryId TEXT NOT NULL DEFAULT 'other'")
            db.execSQL("ALTER TABLE reports ADD COLUMN lastSyncAttemptAt INTEGER")
            db.execSQL("ALTER TABLE reports ADD COLUMN serverReportId TEXT")
            db.execSQL("ALTER TABLE reports ADD COLUMN syncAttempts INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE reports ADD COLUMN includeBodyOnSync INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE reports SET clientUuid = 'legacy-' || id WHERE clientUuid = '' OR clientUuid IS NULL")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE reports ADD COLUMN areaCode TEXT")
            db.execSQL("ALTER TABLE reports ADD COLUMN areaLabel TEXT")
            db.execSQL("ALTER TABLE reports ADD COLUMN areaType TEXT")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE reports ADD COLUMN aiAnalysisJson TEXT")
            db.execSQL("ALTER TABLE reports ADD COLUMN aiAnalyzedAt INTEGER")
            db.execSQL("ALTER TABLE reports ADD COLUMN aiSource TEXT")
        }
    }
}