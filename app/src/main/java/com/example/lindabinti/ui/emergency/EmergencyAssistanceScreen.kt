package com.example.lindabinti.ui.emergency

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.example.lindabinti.ui.common.LindaProtectedScaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.R
import com.example.lindabinti.data.content.LocalContent
import com.example.lindabinti.sos.SosEmergencyCategory
import com.example.lindabinti.sos.SosLocationPeek
import com.example.lindabinti.sos.SosPreflight
import com.example.lindabinti.ui.common.LindaSectionCard
import com.example.lindabinti.ui.common.ProtectedTopBar
import com.example.lindabinti.ui.common.dialNumber
import com.example.lindabinti.ui.locale.rememberAppLanguage
import com.example.lindabinti.ui.theme.LindaDanger
import com.example.lindabinti.util.PermissionHelper
import com.example.lindabinti.util.findActivity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmergencyAssistanceScreen(
    onBack: () -> Unit,
    onQuickHide: () -> Unit,
    onSendSos: (SosEmergencyCategory) -> Unit,
    onOpenContacts: () -> Unit = {}
) {
    val mod = androidx.compose.ui.Modifier
    val context = LocalContext.current
    val activity = context.findActivity()
    val app = LindaBintiApp.instance
    val contacts by app.contactRepository.observeContacts()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val firstContact = contacts.firstOrNull()
    val lang = rememberAppLanguage()
    val hotlines = remember(lang) { LocalContent.hotlines(lang) }

    var smsAsked by remember { mutableStateOf(false) }
    var locationAsked by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableStateOf(0) }
    var category by remember { mutableStateOf(SosEmergencyCategory.GENERAL) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        smsAsked = true
        locationAsked = true
        refreshTick++
    }

    val preflight = remember(contacts.size, refreshTick, smsAsked, locationAsked) {
        SosPreflight.evaluate(
            context = context,
            contactCount = contacts.size,
            activity = activity,
            smsAskedBefore = smsAsked,
            locationAskedBefore = locationAsked
        )
    }
    val locationPeek = remember(preflight.locationGranted, refreshTick) {
        app.locationProvider.peekUiStatus()
    }
    val phoneLocationOn = remember(refreshTick) {
        app.locationProvider.isSystemLocationEnabled()
    }

    LaunchedEffect(preflight.locationGranted, phoneLocationOn) {
        if (preflight.locationGranted && phoneLocationOn) {
            app.locationProvider.beginEmergencyFix()
        }
    }

    val categoryOptions = listOf(
        SosEmergencyCategory.GBV to R.string.emergency_cat_gbv,
        SosEmergencyCategory.FGM to R.string.emergency_cat_fgm,
        SosEmergencyCategory.FGM_THREAT to R.string.emergency_cat_fgm_threat,
        SosEmergencyCategory.OTHER_GBV_FGM to R.string.emergency_cat_other,
        SosEmergencyCategory.GENERAL to R.string.emergency_cat_general
    )

    LindaProtectedScaffold(
        topBar = {
            ProtectedTopBar(
                title = stringResource(R.string.emergency_title),
                onBack = onBack,
                onQuickHide = onQuickHide
            )
        }
    ) { padding ->
        Column(
            modifier = mod
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.emergency_intro),
                style = MaterialTheme.typography.bodyMedium
            )
            LindaSectionCard {
                Text(
                    stringResource(R.string.emergency_complement_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(stringResource(R.string.emergency_hotlines_priority), fontWeight = FontWeight.SemiBold)
            hotlines.forEach { line ->
                LindaSectionCard {
                    Text(line.name, fontWeight = FontWeight.Medium)
                    Text(line.note, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = mod.height(4.dp))
                    Button(
                        onClick = { context.dialNumber(line.number) },
                        colors = ButtonDefaults.buttonColors(containerColor = LindaDanger),
                        modifier = mod.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_call, line.number))
                    }
                }
            }

            LindaSectionCard {
                Text(
                    stringResource(
                        if (preflight.canSendSms) R.string.emergency_alert_ready
                        else R.string.emergency_preflight_title
                    ),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = mod.height(8.dp))
                Text(
                    stringResource(R.string.emergency_category_label),
                    fontWeight = FontWeight.Medium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categoryOptions.forEach { (value, labelRes) ->
                        FilterChip(
                            selected = category == value,
                            onClick = { category = value },
                            label = { Text(stringResource(labelRes)) }
                        )
                    }
                }
                Spacer(modifier = mod.height(8.dp))
                Text(
                    stringResource(
                        if (preflight.smsGranted) R.string.emergency_sms_allowed
                        else R.string.emergency_sms_not_allowed
                    ),
                    color = if (preflight.smsGranted) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                if (!preflight.smsGranted) {
                    Text(
                        stringResource(R.string.emergency_preflight_sms_missing),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    stringResource(
                        when {
                            !preflight.locationGranted -> R.string.emergency_location_unavailable
                            !phoneLocationOn -> R.string.emergency_location_phone_off
                            locationPeek == SosLocationPeek.AVAILABLE ->
                                R.string.emergency_location_available
                            else -> R.string.emergency_location_attempting
                        }
                    )
                )
                Text(
                    if (preflight.hasTrustedContacts) {
                        stringResource(
                            R.string.emergency_preflight_contacts_ok,
                            preflight.contactCount
                        )
                    } else {
                        stringResource(R.string.emergency_preflight_contacts_missing)
                    },
                    color = if (preflight.hasTrustedContacts) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                if (!preflight.notificationsGranted) {
                    Text(stringResource(R.string.emergency_preflight_notif_missing))
                }
                Text(
                    stringResource(R.string.sos_dual_sim_note),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = mod.height(6.dp))
                Text(
                    stringResource(
                        if (preflight.canSendSms) R.string.emergency_preflight_ready
                        else R.string.emergency_preflight_blocked
                    ),
                    fontWeight = FontWeight.Medium,
                    color = if (preflight.canSendSms) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                if (preflight.smsPermanentlyDenied || preflight.locationPermanentlyDenied) {
                    Text(
                        stringResource(R.string.perm_permanent_denied),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (!preflight.smsGranted || !preflight.locationGranted || !preflight.notificationsGranted) {
                OutlinedButton(
                    onClick = {
                        if (preflight.smsPermanentlyDenied) {
                            PermissionHelper.openAppSettings(context)
                        } else {
                            permissionLauncher.launch(PermissionHelper.sosPermissionArray())
                        }
                    },
                    modifier = mod.fillMaxWidth()
                ) {
                    Text(
                        stringResource(
                            if (preflight.smsPermanentlyDenied) R.string.perm_open_settings
                            else R.string.emergency_request_permissions
                        )
                    )
                }
            }
            if (preflight.locationGranted && !phoneLocationOn) {
                OutlinedButton(
                    onClick = { PermissionHelper.openLocationSettings(context) },
                    modifier = mod.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.emergency_turn_on_location))
                }
            }
            if (preflight.smsPermanentlyDenied) {
                OutlinedButton(
                    onClick = { PermissionHelper.openAppSettings(context) },
                    modifier = mod.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.perm_open_settings))
                }
            }
            if (!preflight.hasTrustedContacts) {
                OutlinedButton(
                    onClick = onOpenContacts,
                    modifier = mod.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.emergency_go_contacts))
                }
            }

            Button(
                onClick = {
                    if (preflight.canSendSms) {
                        onSendSos(category)
                    } else if (preflight.smsPermanentlyDenied) {
                        PermissionHelper.openAppSettings(context)
                    } else if (!preflight.smsGranted) {
                        permissionLauncher.launch(PermissionHelper.sosPermissionArray())
                    } else if (!preflight.hasTrustedContacts) {
                        onOpenContacts()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LindaDanger),
                modifier = mod.fillMaxWidth()
            ) {
                Text(stringResource(R.string.emergency_send_sos))
            }

            if (firstContact != null) {
                OutlinedButton(
                    onClick = { context.dialNumber(firstContact.phoneE164) },
                    modifier = mod.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_call, firstContact.name))
                }
            }

            LindaSectionCard {
                Text(stringResource(R.string.emergency_tips_title), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.emergency_tips_body),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
