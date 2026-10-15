package com.example.lindabinti.sos

import com.example.lindabinti.data.repository.SosRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SosStatusUpdaterImpl(
    private val sosRepository: SosRepository
) : SosStatusUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onSmsSent(deliveryId: Long) {
        scope.launch {
            // Do not overwrite failed (e.g. earlier multipart part failed).
            sosRepository.markDeliveryIfNotTerminal(deliveryId, "sent")
            sosRepository.refreshEventStatusForDelivery(deliveryId)
        }
    }

    override fun onSmsSendFailed(deliveryId: Long, error: String) {
        scope.launch {
            sosRepository.markDelivery(deliveryId, "failed", error)
            sosRepository.refreshEventStatusForDelivery(deliveryId)
        }
    }

    override fun onSmsDelivered(deliveryId: Long) {
        scope.launch {
            // Only confirm delivery from DELIVERED callback; never from SENT alone.
            sosRepository.markDeliveryIfStatus(deliveryId, from = "sent", to = "delivered", error = null)
            sosRepository.markDeliveryIfStatus(
                deliveryId,
                from = "sent_unconfirmed",
                to = "delivered",
                error = null
            )
            sosRepository.markDeliveryIfStatus(deliveryId, from = "queued", to = "delivered", error = null)
            sosRepository.refreshEventStatusForDelivery(deliveryId)
        }
    }

    override fun onSmsDeliveryFailed(deliveryId: Long, error: String) {
        scope.launch {
            // Keep honesty: radio may have accepted (sent) but delivery was not confirmed.
            sosRepository.markDeliveryIfStatus(deliveryId, from = "sent", to = "sent_unconfirmed", error)
            sosRepository.markDeliveryIfStatus(deliveryId, from = "queued", to = "sent_unconfirmed", error)
            sosRepository.refreshEventStatusForDelivery(deliveryId)
        }
    }
}
