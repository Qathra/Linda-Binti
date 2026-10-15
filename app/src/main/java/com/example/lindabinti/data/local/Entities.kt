package com.example.lindabinti.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_contacts")
data class TrustedContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneE164: String,
    val relationship: String = "",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sos_events")
data class SosEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val triggeredAt: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val locationStatus: String = "unavailable",
    val overallStatus: String = "sending"
)

@Entity(tableName = "sos_deliveries")
data class SosDelivery(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val contactIdSnapshot: Long?,
    val phoneSnapshot: String,
    val nameSnapshot: String,
    val status: String = "pending",
    val error: String? = null
)
