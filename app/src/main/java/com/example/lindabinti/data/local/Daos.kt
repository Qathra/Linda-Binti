package com.example.lindabinti.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedContactDao {
    @Query("SELECT * FROM trusted_contacts ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<TrustedContact>>

    @Query("SELECT * FROM trusted_contacts ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<TrustedContact>

    @Query("SELECT COUNT(*) FROM trusted_contacts")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(contact: TrustedContact): Long

    @Update
    suspend fun update(contact: TrustedContact)

    @Delete
    suspend fun delete(contact: TrustedContact)

    @Query("DELETE FROM trusted_contacts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SosDao {
    @Insert
    suspend fun insertEvent(event: SosEvent): Long

    @Query("UPDATE sos_events SET overallStatus = :status WHERE id = :id")
    suspend fun updateEventStatus(id: Long, status: String)

    @Query(
        """
        UPDATE sos_events
        SET latitude = :latitude,
            longitude = :longitude,
            accuracyMeters = :accuracy,
            locationStatus = :locationStatus
        WHERE id = :id
        """
    )
    suspend fun updateEventLocation(
        id: Long,
        latitude: Double?,
        longitude: Double?,
        accuracy: Float?,
        locationStatus: String
    )

    @Insert
    suspend fun insertDelivery(delivery: SosDelivery): Long

    @Query("UPDATE sos_deliveries SET status = :status, error = :error WHERE id = :id")
    suspend fun updateDelivery(id: Long, status: String, error: String?)

    @Query("SELECT * FROM sos_events ORDER BY triggeredAt DESC LIMIT 1")
    suspend fun latestEvent(): SosEvent?

    @Query("SELECT * FROM sos_events ORDER BY triggeredAt DESC LIMIT 1")
    fun observeLatestEvent(): Flow<SosEvent?>

    @Query("SELECT * FROM sos_events ORDER BY triggeredAt ASC")
    suspend fun getAllEvents(): List<SosEvent>

    @Query("SELECT * FROM sos_deliveries WHERE id = :id LIMIT 1")
    suspend fun getDelivery(id: Long): SosDelivery?

    @Query("SELECT * FROM sos_deliveries WHERE eventId = :eventId")
    suspend fun getDeliveriesForEvent(eventId: Long): List<SosDelivery>
}
