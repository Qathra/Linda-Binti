package com.example.lindabinti.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val areaCode: String? = null,
    val areaLabel: String? = null,
    val areaType: String? = null,
    val syncStatus: String = "local",
    val clientUuid: String = "",
    val categoryId: String = "other",
    val lastSyncAttemptAt: Long? = null,
    val serverReportId: String? = null,
    val syncAttempts: Int = 0,
    val includeBodyOnSync: Boolean = false,
    val aiAnalysisJson: String? = null,
    val aiAnalyzedAt: Long? = null,
    val aiSource: String? = null
)

@Entity(tableName = "risk_assessments")
data class RiskAssessment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val answersJson: String,
    val score: Int,
    val band: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "education_progress")
data class EducationProgress(
    @PrimaryKey val moduleId: String,
    val completedAt: Long = System.currentTimeMillis()
)

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Report?

    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Report>>

    @Query("SELECT * FROM reports ORDER BY createdAt ASC")
    suspend fun getAll(): List<Report>

    @Query("SELECT * FROM reports WHERE syncStatus IN ('pending','failed') ORDER BY createdAt ASC")
    suspend fun getPendingOrFailed(): List<Report>

    @Insert
    suspend fun insert(report: Report): Long

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        UPDATE reports SET syncStatus = :status,
          serverReportId = COALESCE(:serverReportId, serverReportId),
          syncAttempts = :attempts,
          lastSyncAttemptAt = :lastAttemptAt
        WHERE id = :id
        """
    )
    suspend fun updateSyncState(
        id: Long,
        status: String,
        serverReportId: String?,
        attempts: Int,
        lastAttemptAt: Long
    )

    @Query("UPDATE reports SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatusOnly(id: Long, status: String)

    @Query("UPDATE reports SET syncStatus = 'pending' WHERE syncStatus = 'local'")
    suspend fun markAllLocalAsPending(): Int

    @Query("UPDATE reports SET aiAnalysisJson = :json, aiAnalyzedAt = :analyzedAt, aiSource = :source WHERE id = :id")
    suspend fun updateAiAnalysis(id: Long, json: String, analyzedAt: Long, source: String)

    @Query("SELECT COUNT(*) FROM reports WHERE clientUuid = :clientUuid AND syncStatus = 'synced'")
    suspend fun countSyncedByClientUuid(clientUuid: String): Int
}

@Dao
interface RiskDao {
    @Query("SELECT * FROM risk_assessments ORDER BY createdAt DESC LIMIT 1")
    fun observeLatest(): Flow<RiskAssessment?>

    @Query("SELECT * FROM risk_assessments ORDER BY createdAt DESC LIMIT 8")
    fun observeRecent(): Flow<List<RiskAssessment>>

    @Query("SELECT * FROM risk_assessments ORDER BY createdAt ASC")
    suspend fun getAll(): List<RiskAssessment>

    @Insert
    suspend fun insert(assessment: RiskAssessment): Long
}

@Dao
interface EducationDao {
    @Query("SELECT * FROM education_progress")
    fun observeAll(): Flow<List<EducationProgress>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: EducationProgress)
}