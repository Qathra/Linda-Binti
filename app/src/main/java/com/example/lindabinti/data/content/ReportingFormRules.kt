package com.example.lindabinti.data.content

import com.example.lindabinti.data.sync.SyncStatus

/**
 * Category-first GBV/FGM reporting rules. Description is optional.
 * No name, phone, or GPS is collected on the report form.
 */
object ReportingFormRules {

    fun canSave(categoryId: String?): Boolean =
        !categoryId.isNullOrBlank() && GbvReportCategories.isGbvFgmScope(categoryId)

    fun storedDescription(optionalText: String): String = optionalText.trim()

    fun isDeviceOnly(syncStatus: String): Boolean = syncStatus == SyncStatus.LOCAL

    fun shouldAttemptSyncAfterSave(syncStatus: String): Boolean =
        SyncStatus.isQueueable(syncStatus)
}
