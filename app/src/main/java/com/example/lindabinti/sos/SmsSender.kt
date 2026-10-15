package com.example.lindabinti.sos

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import com.example.lindabinti.LindaBintiApp
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SmsSender(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun sendText(
        phone: String,
        message: String,
        deliveryId: Long,
        attempt: Int = 0
    ): Result<Unit> {
        val managers = resolveSmsManagers()
        var lastError: Exception? = null
        for (smsManager in managers) {
            try {
                submitOnMain(smsManager, phone, message, deliveryId, attempt)
                return Result.success(Unit)
            } catch (e: Exception) {
                Log.w(TAG, "SMS submit failed for $deliveryId: ${e.message}")
                lastError = e
            }
        }
        return Result.failure(lastError ?: IllegalStateException("No SmsManager"))
    }

    private fun submitOnMain(
        smsManager: SmsManager,
        phone: String,
        message: String,
        deliveryId: Long,
        attempt: Int
    ) {
        val latch = CountDownLatch(1)
        val error = java.util.concurrent.atomic.AtomicReference<Exception?>()
        val task = Runnable {
            try {
                submit(smsManager, phone, message, deliveryId, attempt)
            } catch (e: Exception) {
                error.set(e)
            } finally {
                latch.countDown()
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run()
        } else {
            mainHandler.post(task)
            if (!latch.await(8, TimeUnit.SECONDS)) {
                throw IllegalStateException("SMS submit timed out")
            }
        }
        error.get()?.let { throw it }
    }

    /**
     * Ordinary GSM text: default SIM SMSC, no delivery report
     * (delivery reports force SMS-over-IMS on this Samsung).
     */
    private fun submit(
        smsManager: SmsManager,
        phone: String,
        message: String,
        deliveryId: Long,
        attempt: Int
    ) {
        val parts = smsManager.divideMessage(message)
        if (parts.isNullOrEmpty()) {
            throw IllegalStateException("SMS divideMessage returned no parts")
        }
        val dest = android.telephony.PhoneNumberUtils.stripSeparators(phone).ifBlank { phone }
        val prefix = dest.take(3)
        Log.i(
            TAG,
            "submit gsm parts=${parts.size} destLen=${dest.length} destPrefix=$prefix " +
                "id=$deliveryId attempt=$attempt"
        )

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or mutablePiFlag()
        val sentIntents = ArrayList<PendingIntent>(parts.size)
        for (i in parts.indices) {
            sentIntents.add(
                PendingIntent.getBroadcast(
                    context,
                    pendingRequestCode(deliveryId, i, delivered = false, attempt),
                    Intent(context, SmsStatusReceiver::class.java)
                        .setAction(ACTION_SMS_SENT)
                        .putExtra(EXTRA_DELIVERY_ID, deliveryId)
                        .putExtra(EXTRA_PART_INDEX, i)
                        .putExtra(EXTRA_PART_COUNT, parts.size)
                        .putExtra(EXTRA_ATTEMPT, attempt),
                    flags
                )
            )
        }

        if (parts.size == 1) {
            smsManager.sendTextMessage(dest, null, parts[0], sentIntents[0], null)
        } else {
            smsManager.sendMultipartTextMessage(dest, null, parts, sentIntents, null)
        }
    }

    /**
     * Default SMS SIM first (Safaricom on this phone), then platform default.
     */
    @Suppress("DEPRECATION")
    private fun resolveSmsManagers(): List<SmsManager> {
        val out = ArrayList<SmsManager>(2)
        val subId = try {
            SmsManager.getDefaultSmsSubscriptionId()
        } catch (_: Exception) {
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        }
        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            forSubscription(subId)?.let(out::add)
        }
        val fallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            SmsManager.getDefault()
        }
        if (out.none { it === fallback }) out += fallback
        return out
    }

    @Suppress("DEPRECATION")
    private fun forSubscription(subId: Int): SmsManager? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
                ?.createForSubscriptionId(subId)
                ?: SmsManager.getSmsManagerForSubscriptionId(subId)
        } else {
            SmsManager.getSmsManagerForSubscriptionId(subId)
        }
    } catch (_: Exception) {
        null
    }

    companion object {
        const val ACTION_SMS_SENT = "com.example.lindabinti.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.example.lindabinti.SMS_DELIVERED"
        const val EXTRA_DELIVERY_ID = "delivery_id"
        const val EXTRA_PART_INDEX = "part_index"
        const val EXTRA_PART_COUNT = "part_count"
        const val EXTRA_ATTEMPT = "attempt"

        fun pendingRequestCode(
            deliveryId: Long,
            partIndex: Int,
            delivered: Boolean,
            attempt: Int = 0
        ): Int {
            val mixed = deliveryId * 31L +
                partIndex * 7L +
                (if (delivered) 1L else 0L) +
                attempt * 1009L
            return (mixed and 0x7FFFFFFF).toInt()
        }

        private fun mutablePiFlag(): Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }

        fun buildAlertMessage(
            location: SosLocation,
            triggeredAtMillis: Long,
            category: SosEmergencyCategory = SosEmergencyCategory.GENERAL
        ): String = SosAlertMessage.build(location, triggeredAtMillis, category)

        private const val TAG = "LindaSos"
    }
}

/** Lets [SosDispatcher] wait for the SENT radio callback before the next contact. */
object SmsSendWaiters {
    private data class Slot(
        val attempt: Int,
        val deferred: kotlinx.coroutines.CompletableDeferred<Int>
    )

    private val waiters = java.util.concurrent.ConcurrentHashMap<Long, Slot>()

    fun register(
        deliveryId: Long,
        attempt: Int
    ): kotlinx.coroutines.CompletableDeferred<Int> {
        val next = kotlinx.coroutines.CompletableDeferred<Int>()
        waiters.put(deliveryId, Slot(attempt, next))?.deferred?.cancel()
        return next
    }

    fun complete(deliveryId: Long, attempt: Int, resultCode: Int): Boolean {
        val slot = waiters[deliveryId] ?: return false
        if (slot.attempt != attempt) return false
        if (!waiters.remove(deliveryId, slot)) return false
        if (slot.deferred.isActive) slot.deferred.complete(resultCode)
        return true
    }
}

/**
 * Updates per-contact SOS delivery status from radio callbacks.
 * Never marks delivered unless the DELIVERED broadcast reports success.
 */
class SmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val deliveryId = intent.getLongExtra(SmsSender.EXTRA_DELIVERY_ID, -1L)
        if (deliveryId < 0) return
        val partIndex = intent.getIntExtra(SmsSender.EXTRA_PART_INDEX, 0)
        val partCount = intent.getIntExtra(SmsSender.EXTRA_PART_COUNT, 1).coerceAtLeast(1)

        val attempt = intent.getIntExtra(SmsSender.EXTRA_ATTEMPT, 0)

        val app = context.applicationContext as? LindaBintiApp ?: return
        when (intent.action) {
            SmsSender.ACTION_SMS_SENT -> {
                android.util.Log.i(
                    "LindaSos",
                    "SMS_SENT id=$deliveryId attempt=$attempt part=$partIndex/$partCount " +
                        "code=$resultCode err=${intent.getIntExtra("errorCode", Int.MIN_VALUE)}"
                )
                when (
                    MultipartSmsPartRules.onSentResult(
                        partIndex,
                        partCount,
                        resultCode = resultCode
                    )
                ) {
                    MultipartSmsPartRules.Outcome.MarkSent -> {
                        if (SmsSendWaiters.complete(deliveryId, attempt, resultCode)) {
                            app.sosStatusUpdater.onSmsSent(deliveryId)
                        }
                    }
                    MultipartSmsPartRules.Outcome.MarkSendFailed -> {
                        // Dispatcher owns failed vs retry vs unconfirmed.
                        SmsSendWaiters.complete(deliveryId, attempt, resultCode)
                    }
                    else -> {
                        if (partIndex == partCount - 1 && resultCode == 0) {
                            SmsSendWaiters.complete(deliveryId, attempt, 0)
                        }
                    }
                }
            }
            SmsSender.ACTION_SMS_DELIVERED -> {
                when (
                    MultipartSmsPartRules.onDeliveredResult(
                        partIndex,
                        partCount,
                        resultOk = resultCode == Activity.RESULT_OK
                    )
                ) {
                    MultipartSmsPartRules.Outcome.MarkDelivered ->
                        app.sosStatusUpdater.onSmsDelivered(deliveryId)
                    MultipartSmsPartRules.Outcome.MarkDeliveryFailed ->
                        app.sosStatusUpdater.onSmsDeliveryFailed(
                            deliveryId,
                            "Delivery not confirmed ($resultCode)"
                        )
                    else -> Unit
                }
            }
        }
    }
}

interface SosStatusUpdater {
    fun onSmsSent(deliveryId: Long)
    fun onSmsSendFailed(deliveryId: Long, error: String)
    fun onSmsDelivered(deliveryId: Long)
    fun onSmsDeliveryFailed(deliveryId: Long, error: String)
}
