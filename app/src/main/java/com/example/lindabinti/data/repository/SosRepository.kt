package com.example.lindabinti.data.repository

import com.example.lindabinti.data.local.SosDao
import com.example.lindabinti.data.local.SosDelivery
import com.example.lindabinti.data.local.SosEvent
import com.example.lindabinti.sos.SosStatusAggregator
import kotlinx.coroutines.flow.Flow

class SosRepository(private val dao: SosDao) {

    fun observeLatestEvent(): Flow<SosEvent?> = dao.observeLatestEvent()

    suspend fun createEvent(
        latitude: Double?,
        longitude: Double?,
        accuracy: Float?,
        locationStatus: String
    ): Long {
        return dao.insertEvent(
            SosEvent(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = accuracy,
                locationStatus = locationStatus,
                overallStatus = "sending"
            )
        )
    }

    suspend fun addDelivery(
        eventId: Long,
        contactId: Long?,
        phone: String,
        name: String
    ): Long {
        return dao.insertDelivery(
            SosDelivery(
                eventId = eventId,
                contactIdSnapshot = contactId,
                phoneSnapshot = phone,
                nameSnapshot = name,
                status = "pending"
            )
        )
    }

    suspend fun markDelivery(id: Long, status: String, error: String? = null) {
        dao.updateDelivery(id, status, error)
    }

    suspend fun markDeliveryIfNotTerminal(id: Long, status: String) {
        val current = dao.getDelivery(id)?.status ?: return
        if (current == "delivered" || current == "failed") return
        dao.updateDelivery(id, status, null)
    }

    suspend fun markDeliveryIfStatus(id: Long, from: String, to: String, error: String?) {
        val current = dao.getDelivery(id)?.status ?: return
        if (current == from) {
            dao.updateDelivery(id, to, error)
        }
    }

    suspend fun markEvent(id: Long, status: String) {
        dao.updateEventStatus(id, status)
    }

    suspend fun updateEventLocation(
        eventId: Long,
        latitude: Double?,
        longitude: Double?,
        accuracy: Float?,
        locationStatus: String
    ) {
        dao.updateEventLocation(eventId, latitude, longitude, accuracy, locationStatus)
    }

    suspend fun latestEvent(): SosEvent? = dao.latestEvent()

    suspend fun refreshEventStatusForDelivery(deliveryId: Long) {
        val delivery = dao.getDelivery(deliveryId) ?: return
        refreshEventStatus(delivery.eventId)
    }

    suspend fun refreshEventStatus(eventId: Long) {
        val deliveries = dao.getDeliveriesForEvent(eventId)
        if (deliveries.isEmpty()) return
        val overall = SosStatusAggregator.overallFromDeliveries(deliveries.map { it.status })
        dao.updateEventStatus(eventId, overall)
    }
}
