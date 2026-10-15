package com.example.lindabinti.sos

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.MainActivity
import com.example.lindabinti.R
import com.example.lindabinti.util.PermissionHelper
import kotlinx.coroutines.launch

/**
 * Sends SOS SMS to all trusted contacts with location when available.
 * Notification title stays generic ("Calculator") for discretion.
 */
class SosForegroundService : LifecycleService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val incoming = intent
        if (incoming == null || incoming.action != ACTION_TRIGGER_SOS) {
            stopSelf()
            return START_NOT_STICKY
        }

        val category = SosEmergencyCategory.fromWire(incoming.getStringExtra(EXTRA_CATEGORY))
        lastStartedCategory = category

        val notification = buildNotification()
        var fgsOk = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // shortService cannot be combined with other types on Android 14+.
                val type = if (PermissionHelper.locationGranted(this)) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
                }
                startForeground(NOTIFICATION_ID, notification, type)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val type = if (PermissionHelper.locationGranted(this)) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else {
                    0
                }
                startForeground(NOTIFICATION_ID, notification, type)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            fgsOk = true
        } catch (_: Exception) {
            fgsOk = false
        }

        if (!fgsOk) {
            // Android killed FGS start — still send SMS from the application scope.
            LindaBintiApp.instance.enqueueSos(category)
            stopSelf()
            return START_NOT_STICKY
        }

        lifecycleScope.launch {
            val app = LindaBintiApp.instance
            app.locationProvider.beginSosHold()
            try {
                app.sosDispatcher.dispatch(category)
            } finally {
                app.notifyNgoAfterSos()
                app.locationProvider.endSosHold()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    /** Android 14+ shortService time limit — stop cleanly rather than crash. */
    override fun onTimeout(startId: Int) {
        LindaBintiApp.instance.locationProvider.endSosHold()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val launch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, LindaBintiApp.SOS_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_working))
            .setSmallIcon(R.drawable.ic_calculator_notification)
            .setContentIntent(launch)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_TRIGGER_SOS = "com.example.lindabinti.action.TRIGGER_SOS"
        const val EXTRA_CATEGORY = "sos_emergency_category"
        private const val NOTIFICATION_ID = 2101

        @Volatile
        private var lastStartedCategory: SosEmergencyCategory = SosEmergencyCategory.GENERAL

        /**
         * @param category omitted on resend so the last explicit category is reused.
         * Calculator SOS must pass [SosEmergencyCategory.GENERAL].
         */
        fun start(
            context: android.content.Context,
            category: SosEmergencyCategory = lastStartedCategory
        ) {
            lastStartedCategory = category
            val appContext = context.applicationContext
            val intent = Intent(appContext, SosForegroundService::class.java).apply {
                action = ACTION_TRIGGER_SOS
                putExtra(EXTRA_CATEGORY, category.name)
            }
            try {
                androidx.core.content.ContextCompat.startForegroundService(appContext, intent)
            } catch (_: Exception) {
                (appContext as? LindaBintiApp)?.enqueueSos(category)
            }
        }
    }
}
