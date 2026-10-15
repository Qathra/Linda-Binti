package com.example.lindabinti.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.example.lindabinti.ui.common.LindaProtectedScaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.R
import com.example.lindabinti.ui.common.ModuleTile
import com.example.lindabinti.ui.common.ConnectivityStatusRow
import com.example.lindabinti.ui.common.ProtectedTopBar
import com.example.lindabinti.ui.common.StatusBanner
import com.example.lindabinti.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HomeModule(
    val id: String,
    val titleRes: Int,
    val subtitleRes: Int,
    val iconRes: Int,
    val emphasized: Boolean = false,
    val enabled: Boolean = true
)

@Composable
fun ProtectedHomeScreen(
    sosJustTriggered: Boolean,
    onQuickHide: () -> Unit,
    onOpenModule: (String) -> Unit,
    onResendSos: () -> Unit
) {
    val mod = androidx.compose.ui.Modifier
    val context = LocalContext.current
    val app = LindaBintiApp.instance
    val latestSos by app.sosRepository.observeLatestEvent()
        .collectAsStateWithLifecycle(initialValue = null)

    // Warm GPS/network cache so the next SOS already has coordinates.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching { app.locationProvider.warmCache() }
        }
    }

    val modules = listOf(
        HomeModule("emergency", R.string.module_emergency, R.string.module_emergency_sub, R.drawable.ic_edu_emergency, emphasized = true),
        HomeModule("education", R.string.module_education, R.string.module_education_sub, R.drawable.ic_edu_gbv),
        HomeModule("contacts", R.string.module_contacts, R.string.module_contacts_sub, R.drawable.ic_home_contacts),
        HomeModule("reporting", R.string.module_reporting, R.string.module_reporting_sub, R.drawable.ic_edu_reporting),
        HomeModule("risk", R.string.module_risk, R.string.module_risk_sub, R.drawable.ic_edu_safety),
        HomeModule("safety_compass", R.string.module_compass, R.string.module_compass_sub, R.drawable.ic_compass, emphasized = true),
        HomeModule("support", R.string.module_support, R.string.module_support_sub, R.drawable.ic_edu_survivor),
        HomeModule("ai", R.string.module_ai, R.string.module_ai_sub, R.drawable.ic_compass, emphasized = true),
        HomeModule("settings", R.string.module_settings, R.string.module_settings_sub, R.drawable.ic_home_settings)
    )

    val contentAlpha by animateFloatAsState(targetValue = 1f, label = "homeAlpha")
    val userName = app.authRepository.getUserDisplayName()

    val recent = latestSos != null &&
        (System.currentTimeMillis() - (latestSos?.triggeredAt ?: 0L)) < 15 * 60 * 1000L
    val showBanner = sosJustTriggered || recent
    val status = latestSos?.overallStatus ?: if (sosJustTriggered) "sending" else null
    val (titleRes, bodyRes) = when (status) {
        "delivered" -> R.string.sos_status_delivered_title to R.string.sos_status_delivered_body
        "submitted", "sent_unconfirmed" ->
            R.string.sos_status_submitted_title to R.string.sos_status_submitted_body
        "queued" -> R.string.sos_status_queued_title to R.string.sos_status_queued_body
        "partial" -> R.string.sos_status_partial_title to R.string.sos_status_partial_body
        "failed" -> R.string.sos_status_failed_title to R.string.sos_status_failed_body
        else -> R.string.sos_status_submitting to R.string.sos_status_help
    }
    val locationNote = when (latestSos?.locationStatus) {
        "ok", "cached" -> stringResource(R.string.sos_status_location_note_ok)
        "unavailable" -> stringResource(R.string.sos_status_location_note_unavailable)
        else -> null
    }
    val bodyText = buildString {
        append(stringResource(bodyRes))
        if (!locationNote.isNullOrBlank()) {
            append("\n")
            append(locationNote)
        }
    }
    val showSettingsAction = status == "failed" && !PermissionHelper.smsGranted(context)

    LindaProtectedScaffold(
        topBar = {
            ProtectedTopBar(
                title = stringResource(R.string.app_brand_protected),
                onQuickHide = onQuickHide
            )
        }
    ) { padding ->
        Column(
            modifier = mod
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .alpha(contentAlpha),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatusBanner(
                visible = showBanner && status != null,
                title = stringResource(titleRes),
                body = bodyText,
                actionLabel = stringResource(R.string.sos_send_again),
                onAction = onResendSos,
                secondaryActionLabel = if (showSettingsAction) {
                    stringResource(R.string.sos_open_settings)
                } else null,
                onSecondaryAction = if (showSettingsAction) {
                    { PermissionHelper.openAppSettings(context) }
                } else null
            )

            ConnectivityStatusRow()

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = mod.fillMaxWidth()
            ) {
                Column(
                    modifier = mod.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_brand_protected),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (userName.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.home_greeting, userName),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.home_modules_heading),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = mod.fillMaxSize()
            ) {
                items(modules, key = { it.id }) { module ->
                    ModuleTile(
                        title = stringResource(module.titleRes),
                        subtitle = stringResource(module.subtitleRes),
                        emphasized = module.emphasized,
                        enabled = module.enabled,
                        iconRes = module.iconRes,
                        onClick = { if (module.enabled) onOpenModule(module.id) }
                    )
                }
            }
        }
    }
}
