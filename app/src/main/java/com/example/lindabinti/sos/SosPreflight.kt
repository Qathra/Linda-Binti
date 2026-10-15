package com.example.lindabinti.sos

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.lindabinti.util.PermissionHelper

/**
 * Preflight checks before starting SOS SMS.
 * Location is recommended but never blocks send.
 */
data class SosPreflightResult(
    val canSendSms: Boolean,
    val smsGranted: Boolean,
    val hasTrustedContacts: Boolean,
    val contactCount: Int,
    val locationGranted: Boolean,
    val notificationsGranted: Boolean,
    val smsPermanentlyDenied: Boolean,
    val locationPermanentlyDenied: Boolean
) {
    val blockers: List<Blocker>
        get() = buildList {
            if (!smsGranted) add(Blocker.SmsPermission)
            if (!hasTrustedContacts) add(Blocker.NoContacts)
        }

    enum class Blocker {
        SmsPermission,
        NoContacts
    }
}

object SosPreflight {

    fun evaluate(
        context: Context,
        contactCount: Int,
        activity: Activity? = null,
        smsAskedBefore: Boolean = false,
        locationAskedBefore: Boolean = false
    ): SosPreflightResult {
        val smsGranted = PermissionHelper.smsGranted(context)
        val locationGranted = PermissionHelper.locationGranted(context)
        val notificationsGranted = PermissionHelper.notificationsGranted(context)
        val hasContacts = contactCount >= 1

        val smsPermanent = !smsGranted &&
            smsAskedBefore &&
            isPermanentlyDenied(activity, Manifest.permission.SEND_SMS)
        val locationPermanent = !locationGranted &&
            locationAskedBefore &&
            (
                isPermanentlyDenied(activity, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    isPermanentlyDenied(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                )

        return SosPreflightResult(
            canSendSms = smsGranted && hasContacts,
            smsGranted = smsGranted,
            hasTrustedContacts = hasContacts,
            contactCount = contactCount,
            locationGranted = locationGranted,
            notificationsGranted = notificationsGranted,
            smsPermanentlyDenied = smsPermanent,
            locationPermanentlyDenied = locationPermanent
        )
    }

    private fun isPermanentlyDenied(activity: Activity?, permission: String): Boolean {
        if (activity == null) return false
        if (ContextCompat.checkSelfPermission(activity, permission) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        // After user denies with “Don’t ask again”, rationale returns false.
        return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    fun requiresPostNotificationsRuntime(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
