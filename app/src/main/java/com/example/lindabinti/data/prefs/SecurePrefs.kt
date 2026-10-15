package com.example.lindabinti.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted storage for Access Code / SOS hashes, lockout counters, and privacy flags.
 * Uses AndroidX EncryptedSharedPreferences (AES256-GCM values, AES256-SIV keys).
 * Combined with allowBackup=false so secrets are not included in cloud backup.
 */
class SecurePrefs(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var setupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETE, false)
        set(value) {
            commitBoolean(KEY_SETUP_COMPLETE, value)
        }

    var userDisplayName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var accessPinHash: String?
        get() = prefs.getString(KEY_ACCESS_PIN_HASH, null)
        set(value) {
            commitString(KEY_ACCESS_PIN_HASH, value)
        }

    var sosCodeHash: String?
        get() = prefs.getString(KEY_SOS_CODE_HASH, null)
        set(value) {
            commitString(KEY_SOS_CODE_HASH, value)
        }

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var shareAnonymousStats: Boolean
        get() = prefs.getBoolean(KEY_SHARE_ANON_STATS, false)
        set(value) = prefs.edit().putBoolean(KEY_SHARE_ANON_STATS, value).apply()

    /** Default on; the user can turn this off in Settings. SOS may still flag NGOs. */
    var syncReportsConsent: Boolean
        get() = prefs.getBoolean(KEY_SYNC_REPORTS_CONSENT, true)
        set(value) = prefs.edit().putBoolean(KEY_SYNC_REPORTS_CONSENT, value).apply()

    /** When true and syncing, include report body text. Default false (category + time only). */
    var syncReportBodies: Boolean
        get() = prefs.getBoolean(KEY_SYNC_REPORT_BODIES, false)
        set(value) = prefs.edit().putBoolean(KEY_SYNC_REPORT_BODIES, value).apply()

    /** Default on; the user can turn this off in Settings. */
    var cloudAiConsent: Boolean
        get() = prefs.getBoolean(KEY_CLOUD_AI_CONSENT, true)
        set(value) = prefs.edit().putBoolean(KEY_CLOUD_AI_CONSENT, value).apply()

    var backendAccessToken: String?
        get() = prefs.getString(KEY_BACKEND_ACCESS, null)
        set(value) = prefs.edit().putString(KEY_BACKEND_ACCESS, value).apply()

    var backendRefreshToken: String?
        get() = prefs.getString(KEY_BACKEND_REFRESH, null)
        set(value) = prefs.edit().putString(KEY_BACKEND_REFRESH, value).apply()

    var backendUserId: String?
        get() = prefs.getString(KEY_BACKEND_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_BACKEND_USER_ID, value).apply()

    var backendEmail: String?
        get() = prefs.getString(KEY_BACKEND_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_BACKEND_EMAIL, value).apply()

    var backendRole: String?
        get() = prefs.getString(KEY_BACKEND_ROLE, null)
        set(value) = prefs.edit().putString(KEY_BACKEND_ROLE, value).apply()

    var backendExpiresAt: Long
        get() = prefs.getLong(KEY_BACKEND_EXPIRES, 0L)
        set(value) = prefs.edit().putLong(KEY_BACKEND_EXPIRES, value).apply()

    /** Stable phone identity for automatic USER session (not shown to NGOs). */
    var deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var accessPinFailCount: Int
        get() = prefs.getInt(KEY_PIN_FAIL_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_PIN_FAIL_COUNT, value).apply()

    var accessPinLockUntilMillis: Long
        get() = prefs.getLong(KEY_PIN_LOCK_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_PIN_LOCK_UNTIL, value).apply()

    /** Last successful support-catalog / content pull from server. */
    var lastContentSyncAt: Long
        get() = prefs.getLong(KEY_LAST_CONTENT_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CONTENT_SYNC, value).apply()

    /** Last full sync cycle (push + pull) timestamp. */
    var lastSyncCycleAt: Long
        get() = prefs.getLong(KEY_LAST_SYNC_CYCLE, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_CYCLE, value).apply()

    /**
     * Optional NGO / helpline E.164. When [escalationSmsEnabled], SOS also SMS this number
     * after trusted contacts. Complements services — does not replace 999/1195.
     */
    var escalationPhoneE164: String
        get() = prefs.getString(KEY_ESCALATION_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ESCALATION_PHONE, value.trim()).apply()

    var escalationSmsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ESCALATION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ESCALATION_ENABLED, value).apply()

    fun clearAll() {
        prefs.edit().clear().commit()
    }

    private fun commitString(key: String, value: String?) {
        val editor = prefs.edit()
        if (value == null) editor.remove(key) else editor.putString(key, value)
        if (!editor.commit()) editor.apply()
    }

    private fun commitBoolean(key: String, value: Boolean) {
        val editor = prefs.edit().putBoolean(key, value)
        if (!editor.commit()) editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "linda_secure_prefs"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_USER_NAME = "user_display_name"
        private const val KEY_ACCESS_PIN_HASH = "access_pin_hash"
        private const val KEY_SOS_CODE_HASH = "sos_code_hash"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_SHARE_ANON_STATS = "share_anon_stats"
        private const val KEY_SYNC_REPORTS_CONSENT = "sync_reports_consent"
        private const val KEY_SYNC_REPORT_BODIES = "sync_report_bodies"
        private const val KEY_CLOUD_AI_CONSENT = "cloud_ai_consent"
        private const val KEY_BACKEND_ACCESS = "backend_access_token"
        private const val KEY_BACKEND_REFRESH = "backend_refresh_token"
        private const val KEY_BACKEND_USER_ID = "backend_user_id"
        private const val KEY_BACKEND_EMAIL = "backend_email"
        private const val KEY_BACKEND_ROLE = "backend_role"
        private const val KEY_BACKEND_EXPIRES = "backend_expires_at"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_PIN_FAIL_COUNT = "pin_fail_count"
        private const val KEY_PIN_LOCK_UNTIL = "pin_lock_until"
        private const val KEY_LAST_CONTENT_SYNC = "last_content_sync_at"
        private const val KEY_LAST_SYNC_CYCLE = "last_sync_cycle_at"
        private const val KEY_ESCALATION_PHONE = "escalation_phone_e164"
        private const val KEY_ESCALATION_ENABLED = "escalation_sms_enabled"

        const val DEFAULT_SOS_CODE = "1102"
        const val MAX_PIN_FAILURES = 5
        const val PIN_LOCK_DURATION_MS = 60_000L
    }
}
