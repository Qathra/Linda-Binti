package com.example.lindabinti.sos

import android.content.Context
import android.util.Log
import com.example.lindabinti.data.local.TrustedContact
import com.example.lindabinti.data.prefs.SecurePrefs
import com.example.lindabinti.data.repository.ContactRepository
import com.example.lindabinti.data.repository.SosRepository
import com.example.lindabinti.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * SOS SMS pipeline. Independent of the foreground service so Equals still
 * sends when Android refuses to start a short foreground service.
 */
class SosDispatcher(
    private val appContext: Context,
    private val contactRepository: ContactRepository,
    private val sosRepository: SosRepository,
    private val locationProvider: LocationProvider,
    private val smsSender: SmsSender,
    private val securePrefs: SecurePrefs
) {
    suspend fun dispatch(category: SosEmergencyCategory) = withContext(Dispatchers.IO) {
        if (!PermissionHelper.smsGranted(appContext)) {
            recordBlocked("SMS", "Permission", "SMS permission not granted")
            return@withContext
        }

        val contacts = contactRepository.getContacts()
        if (contacts.isEmpty()) {
            recordBlocked("", "No contacts", "No trusted contacts configured")
            return@withContext
        }

        // Last-known GPS only for the first SMS — do not wait 35s before alerting.
        locationProvider.beginEmergencyFix()
        val location = locationProvider.getForSos(timeoutMs = SosLocationRules.SOS_FIRST_SMS_WAIT_MS)
        val now = System.currentTimeMillis()
        val message = SmsSender.buildAlertMessage(location, now, category)

        val includeCoords = location.hasFix()
        val lat = if (includeCoords) location.latitude else null
        val lng = if (includeCoords) location.longitude else null
        val acc = if (includeCoords) location.accuracyMeters else null
        val locationStatus = if (includeCoords) location.status else "unavailable"

        val eventId = sosRepository.createEvent(lat, lng, acc, locationStatus)

        val accepted = sendToContacts(eventId, contacts, message)

        // Optional NGO/helpline escalation AFTER trusted contacts (complement, not replace 999).
        maybeSendEscalation(eventId, message)

        sosRepository.refreshEventStatus(eventId)

        if (!includeCoords && accepted) {
            maybeSendLocationFollowUp(eventId, contacts)
        }
        Log.i("LindaSos", "dispatch done event=$eventId accepted=$accepted loc=$locationStatus")
    }

    private suspend fun maybeSendLocationFollowUp(
        eventId: Long,
        contacts: List<TrustedContact>
    ) {
        val location = locationProvider.getForSos(
            timeoutMs = SosLocationRules.SOS_FOLLOWUP_TIMEOUT_MS
        )
        if (!location.hasFix()) return

        sosRepository.updateEventLocation(
            eventId = eventId,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracyMeters,
            locationStatus = location.status
        )
        val message = SosAlertMessage.buildLocationUpdate(location, System.currentTimeMillis())
        sendToContacts(eventId, contacts, message)
        maybeSendEscalation(eventId, message)
        sosRepository.refreshEventStatus(eventId)
    }

    private suspend fun sendToContacts(
        eventId: Long,
        contacts: List<TrustedContact>,
        message: String
    ): Boolean {
        var anyAccepted = false
        contacts.forEachIndexed { index, contact ->
            if (index > 0) delay(SosSmsSendPolicy.BETWEEN_CONTACTS_MS)
            if (sendOneContact(eventId, contact, message)) anyAccepted = true
        }
        return anyAccepted
    }

    private suspend fun sendOneContact(
        eventId: Long,
        contact: TrustedContact,
        message: String
    ): Boolean {
        val destinations = ContactRepository.smsDestinationCandidates(contact.phoneE164)
        val deliveryId = sosRepository.addDelivery(
            eventId = eventId,
            contactId = contact.id,
            phone = contact.phoneE164,
            name = contact.name
        )
        if (destinations.isEmpty()) {
            sosRepository.markDelivery(deliveryId, "failed", "Invalid phone number")
            return false
        }

        var attempt = 0
        while (true) {
            attempt++
            val dest = SosSmsSendPolicy.pickDestination(destinations, attempt)
            sosRepository.markDelivery(deliveryId, "queued")
            val waiter = SmsSendWaiters.register(deliveryId, attempt)
            val submitted = smsSender.sendText(dest, message, deliveryId, attempt)
            if (submitted.isSuccess) {
                val sentCode = withTimeoutOrNull(SosSmsSendPolicy.SENT_WAIT_MS) { waiter.await() }
                if (SosSmsSendPolicy.isSentOk(sentCode)) return true
                if (!SosSmsSendPolicy.shouldRetry(attempt, sentCode)) {
                    val code = sentCode ?: SosSmsSendPolicy.SENT_NETWORK_ERROR
                    if (SosSmsSendPolicy.isHardRadioFailure(code) || sentCode == null) {
                        sosRepository.markDelivery(
                            deliveryId,
                            "failed",
                            "SMS was not accepted by the network ($code)"
                        )
                        return false
                    }
                    sosRepository.markDelivery(
                        deliveryId,
                        "sent_unconfirmed",
                        "Radio did not confirm ($code)"
                    )
                    return true
                }
                delay(SosSmsSendPolicy.retryDelayMs(sentCode))
            } else if (attempt >= SosSmsSendPolicy.MAX_ATTEMPTS) {
                sosRepository.markDelivery(
                    deliveryId,
                    "failed",
                    submitted.exceptionOrNull()?.message ?: "SMS send failed"
                )
                return false
            } else {
                delay(SosSmsSendPolicy.RETRY_DELAY_MS)
            }
        }
    }

    private suspend fun maybeSendEscalation(eventId: Long, message: String) {
        if (!securePrefs.escalationSmsEnabled) return
        val raw = securePrefs.escalationPhoneE164.trim()
        if (raw.isEmpty()) return
        val normalized = ContactRepository.normalizePhone(raw) ?: run {
            val deliveryId = sosRepository.addDelivery(
                eventId = eventId,
                contactId = null,
                phone = raw,
                name = "Escalation"
            )
            sosRepository.markDelivery(deliveryId, "failed", "Invalid escalation phone")
            return
        }
        // Skip if already in trusted contacts.
        val trusted = contactRepository.getContacts().mapNotNull {
            ContactRepository.normalizePhone(it.phoneE164)
        }
        if (normalized in trusted) return

        val deliveryId = sosRepository.addDelivery(
            eventId = eventId,
            contactId = null,
            phone = raw,
            name = "Escalation"
        )
        val result = smsSender.sendText(normalized, message, deliveryId)
        if (result.isSuccess) {
            sosRepository.markDelivery(deliveryId, "queued")
        } else {
            sosRepository.markDelivery(
                deliveryId,
                "failed",
                result.exceptionOrNull()?.message ?: "Escalation SMS failed"
            )
        }
    }

    private suspend fun recordBlocked(phoneLabel: String, nameLabel: String, error: String) {
        val eventId = sosRepository.createEvent(
            latitude = null,
            longitude = null,
            accuracy = null,
            locationStatus = "unavailable"
        )
        val deliveryId = sosRepository.addDelivery(
            eventId = eventId,
            contactId = null,
            phone = phoneLabel,
            name = nameLabel
        )
        sosRepository.markDelivery(deliveryId, "failed", error)
        sosRepository.refreshEventStatus(eventId)
    }
}
