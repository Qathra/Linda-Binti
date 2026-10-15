package com.example.lindabinti.ui.locale

import android.content.Context
import com.example.lindabinti.R
import com.example.lindabinti.data.repository.ContactSaveException
import com.example.lindabinti.data.sync.ReportSyncManager

fun Throwable.localizedContactMessage(context: Context): String {
    val code = (this as? ContactSaveException)?.code ?: message
    val res = when (code) {
        ContactSaveException.INVALID_PHONE -> R.string.setup_contacts_invalid_phone
        ContactSaveException.MAX -> R.string.setup_contacts_max
        ContactSaveException.DUPLICATE -> R.string.setup_contacts_duplicate
        else -> null
    }
    return if (res != null) context.getString(res) else (message ?: context.getString(R.string.setup_failed))
}

fun ReportSyncManager.SyncResult.localizedMessage(context: Context): String = when (code) {
    ReportSyncManager.CODE_NOT_CONFIGURED -> context.getString(R.string.sync_err_not_configured)
    ReportSyncManager.CODE_CONSENT_OFF -> context.getString(R.string.sync_err_consent_off)
    ReportSyncManager.CODE_NEED_USER -> context.getString(R.string.sync_err_need_user)
    ReportSyncManager.CODE_MISSING_TOKEN -> context.getString(R.string.sync_err_missing_token)
    ReportSyncManager.CODE_NOTHING_PENDING -> context.getString(R.string.sync_err_nothing_pending)
    ReportSyncManager.CODE_FINISHED -> context.getString(R.string.sync_finished, synced, failed, skipped)
    else -> message
}
