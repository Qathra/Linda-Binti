package com.example.lindabinti.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrustedContact::class,
        SosEvent::class,
        SosDelivery::class,
        Report::class,
        RiskAssessment::class,
        EducationProgress::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun sosDao(): SosDao
    abstract fun reportDao(): ReportDao
    abstract fun riskDao(): RiskDao
    abstract fun educationDao(): EducationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "linda_binti.db"
                )
                    .addMigrations(
                        LindaMigrations.MIGRATION_1_2,
                        LindaMigrations.MIGRATION_2_3,
                        LindaMigrations.MIGRATION_3_4,
                        LindaMigrations.MIGRATION_4_5,
                        LindaMigrations.MIGRATION_5_6
                    )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}