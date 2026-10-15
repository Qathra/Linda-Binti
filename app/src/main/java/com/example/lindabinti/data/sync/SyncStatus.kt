package com.example.lindabinti.data.sync

/**
 * Report synchronization lifecycle. Codes never leave the device.
 * LOCAL: saved on device only (default / no consent)
 * PENDING: queued for secure upload
 * SYNCED: acknowledged by backend (idempotent clientUuid)
 * FAILED: upload attempted; will retry
 */
object SyncStatus {
    const val LOCAL = "local"
    const val PENDING = "pending"
    const val SYNCED = "synced"
    const val FAILED = "failed"

    fun isQueueable(status: String): Boolean =
        status == PENDING || status == FAILED

    fun afterEnqueueFromLocal(consentEnabled: Boolean, authenticated: Boolean): String =
        PENDING
}