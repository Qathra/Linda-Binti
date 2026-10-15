package com.example.lindabinti.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.example.lindabinti.ui.common.LindaProtectedScaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.lindabinti.BuildConfig
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.R
import com.example.lindabinti.data.content.SupportedLanguages
import com.example.lindabinti.data.repository.AccessPinResult
import com.example.lindabinti.data.repository.ContactRepository
import com.example.lindabinti.ui.common.ProtectedTopBar
import com.example.lindabinti.ui.common.lindaProtectedOverlayTextFieldColors
import com.example.lindabinti.ui.locale.localizedMessage
import com.example.lindabinti.ui.locale.rememberAppLanguage
import com.example.lindabinti.util.LocaleHelper
import com.example.lindabinti.util.PermissionHelper
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onQuickHide: () -> Unit,
    onOpenNgoDashboard: () -> Unit,
    onOpenUssdHelp: () -> Unit,
    onDataReset: () -> Unit
) {
    val mod = androidx.compose.ui.Modifier
    val app = LindaBintiApp.instance
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val language = rememberAppLanguage()
    var shareAnon by remember { mutableStateOf(app.authRepository.isShareAnonymousStats()) }
    var syncBodies by remember { mutableStateOf(app.securePrefs.syncReportBodies) }
    var cloudEmail by remember { mutableStateOf("") }
    var cloudPassword by remember { mutableStateOf("") }
    var cloudName by remember { mutableStateOf("") }
    var cloudSessionActive by remember { mutableStateOf(app.backendSession.isLoggedIn()) }
    var currentPin by remember { mutableStateOf("") }
    var newAccessPin by remember { mutableStateOf("") }
    var confirmAccessPin by remember { mutableStateOf("") }
    var newSosCode by remember { mutableStateOf("") }
    var confirmSosCode by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var smsOk by remember { mutableStateOf(PermissionHelper.smsGranted(context)) }
    var locOk by remember { mutableStateOf(PermissionHelper.locationGranted(context)) }
    var notifOk by remember { mutableStateOf(PermissionHelper.notificationsGranted(context)) }

    fun refreshPerms() {
        smsOk = PermissionHelper.smsGranted(context)
        locOk = PermissionHelper.locationGranted(context)
        notifOk = PermissionHelper.notificationsGranted(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshPerms() }

    LindaProtectedScaffold(
        topBar = {
            ProtectedTopBar(
                title = stringResource(R.string.settings_title),
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
            Text(stringResource(R.string.settings_language), fontWeight = FontWeight.SemiBold)
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = language == SupportedLanguages.EN,
                    onClick = {
                        app.authRepository.setLanguage(SupportedLanguages.EN)
                        LocaleHelper.applyLanguage(SupportedLanguages.EN)
                        status = LocaleHelper.wrap(context, SupportedLanguages.EN)
                            .getString(R.string.settings_language_set_en)
                        error = null
                    },
                    label = { Text(stringResource(R.string.lang_english)) }
                )
                FilterChip(
                    selected = language == SupportedLanguages.SW,
                    onClick = {
                        app.authRepository.setLanguage(SupportedLanguages.SW)
                        LocaleHelper.applyLanguage(SupportedLanguages.SW)
                        status = LocaleHelper.wrap(context, SupportedLanguages.SW)
                            .getString(R.string.settings_language_set_sw)
                        error = null
                    },
                    label = { Text(stringResource(R.string.lang_kiswahili)) }
                )
                FilterChip(
                    selected = language == SupportedLanguages.MAA,
                    onClick = {
                        app.authRepository.setLanguage(SupportedLanguages.MAA)
                        LocaleHelper.applyLanguage(SupportedLanguages.MAA)
                        status = LocaleHelper.wrap(context, SupportedLanguages.MAA)
                            .getString(R.string.settings_language_set_maa)
                        error = null
                    },
                    label = { Text(stringResource(R.string.lang_maa)) }
                )
                FilterChip(
                    selected = language == SupportedLanguages.SO,
                    onClick = {
                        app.authRepository.setLanguage(SupportedLanguages.SO)
                        LocaleHelper.applyLanguage(SupportedLanguages.SO)
                        status = LocaleHelper.wrap(context, SupportedLanguages.SO)
                            .getString(R.string.settings_language_set_so)
                        error = null
                    },
                    label = { Text(stringResource(R.string.lang_somali)) }
                )
            }
            if (SupportedLanguages.isFutureLanguage(language)) {
                Text(
                    stringResource(R.string.lang_future_note),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    stringResource(R.string.lang_pack_incomplete),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(stringResource(R.string.settings_change_access_pin), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.settings_access_pin_help),
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = currentPin,
                onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) currentPin = it },
                label = { Text(stringResource(R.string.settings_current_pin)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = mod.fillMaxWidth(),
                colors = lindaProtectedOverlayTextFieldColors()
            )
            OutlinedTextField(
                value = newAccessPin,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newAccessPin = it },
                label = { Text(stringResource(R.string.settings_new_access_pin)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = mod.fillMaxWidth(),
                colors = lindaProtectedOverlayTextFieldColors()
            )
            OutlinedTextField(
                value = confirmAccessPin,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmAccessPin = it },
                label = { Text(stringResource(R.string.settings_confirm_access_pin)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = mod.fillMaxWidth(),
                colors = lindaProtectedOverlayTextFieldColors()
            )
            Button(
                onClick = {
                    when (val pinResult = app.authRepository.attemptAccessPin(currentPin)) {
                        is AccessPinResult.Locked ->
                            error = context.getString(R.string.pin_locked, pinResult.remainingSeconds.toInt())
                        AccessPinResult.Wrong ->
                            error = context.getString(R.string.pin_wrong)
                        AccessPinResult.Success -> when {
                            newAccessPin.length !in 4..6 ->
                                error = context.getString(R.string.settings_access_len)
                            newAccessPin != confirmAccessPin ->
                                error = context.getString(R.string.settings_access_mismatch)
                            app.authRepository.verifySosCode(newAccessPin) ->
                                error = context.getString(R.string.settings_codes_must_differ)
                            !app.authRepository.saveAccessPin(newAccessPin) ->
                                error = context.getString(R.string.settings_codes_must_differ)
                            else -> {
                                currentPin = ""
                                newAccessPin = ""
                                confirmAccessPin = ""
                                error = null
                                status = context.getString(R.string.settings_access_updated)
                            }
                        }
                    }
                },
                modifier = mod.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_update_access_pin)) }

            Text(stringResource(R.string.settings_change_sos), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.settings_sos_help),
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = newSosCode,
                onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) newSosCode = it },
                label = { Text(stringResource(R.string.settings_new_sos)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = mod.fillMaxWidth(),
                colors = lindaProtectedOverlayTextFieldColors()
            )
            OutlinedTextField(
                value = confirmSosCode,
                onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) confirmSosCode = it },
                label = { Text(stringResource(R.string.settings_confirm_sos)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = mod.fillMaxWidth(),
                colors = lindaProtectedOverlayTextFieldColors()
            )
            Button(
                onClick = {
                    when (val pinResult = app.authRepository.attemptAccessPin(currentPin)) {
                        is AccessPinResult.Locked ->
                            error = context.getString(R.string.pin_locked, pinResult.remainingSeconds.toInt())
                        AccessPinResult.Wrong ->
                            error = context.getString(R.string.settings_need_current_for_sos)
                        AccessPinResult.Success -> when {
                            newSosCode.length < 4 ->
                                error = context.getString(R.string.settings_sos_len)
                            newSosCode != confirmSosCode ->
                                error = context.getString(R.string.settings_sos_mismatch)
                            app.authRepository.verifyAccessPin(newSosCode) ->
                                error = context.getString(R.string.settings_codes_must_differ)
                            !app.authRepository.saveSosCode(newSosCode) ->
                                error = context.getString(R.string.settings_codes_must_differ)
                            else -> {
                                newSosCode = ""
                                confirmSosCode = ""
                                currentPin = ""
                                error = null
                                status = context.getString(R.string.settings_sos_updated)
                            }
                        }
                    }
                },
                modifier = mod.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_update_sos)) }


            Text(stringResource(R.string.privacy_title), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.privacy_body),
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = onOpenUssdHelp,
                modifier = mod.fillMaxWidth()
            ) {
                Text(stringResource(R.string.ussd_help_title))
            }

            Text(stringResource(R.string.settings_permissions), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(
                    R.string.setup_perm_sms_label,
                    if (smsOk) stringResource(R.string.perm_granted) else stringResource(R.string.perm_denied)
                )
            )
            Text(
                stringResource(
                    R.string.setup_perm_location_label,
                    if (locOk) stringResource(R.string.perm_granted) else stringResource(R.string.perm_denied)
                )
            )
            Text(
                stringResource(
                    R.string.setup_perm_notifications_label,
                    if (notifOk) stringResource(R.string.perm_granted) else stringResource(R.string.perm_denied)
                )
            )
            Text(stringResource(R.string.perm_denied_help), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.sos_dual_sim_note), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                onClick = { permissionLauncher.launch(PermissionHelper.sosPermissionArray()) },
                modifier = mod.fillMaxWidth()
            ) { Text(stringResource(R.string.perm_retry)) }
            OutlinedButton(
                onClick = { PermissionHelper.openAppSettings(context) },
                modifier = mod.fillMaxWidth()
            ) { Text(stringResource(R.string.perm_open_settings)) }

            Text(stringResource(R.string.settings_kiswahili_note), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.settings_disguise), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.settings_disguise_body),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(stringResource(R.string.backend_title), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.backend_help), style = MaterialTheme.typography.bodySmall)
            if (BuildConfig.DEBUG) {
                Text(stringResource(R.string.backend_demo_account_hint), style = MaterialTheme.typography.bodySmall)
            }
            if (!app.backendAccountRepository.backendConfigured()) {
                Text(stringResource(R.string.backend_not_configured), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            } else if (BuildConfig.DEBUG) {
                Text(
                    stringResource(
                        R.string.backend_configured_debug,
                        app.backendAccountRepository.configuredBaseUrl()
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            app.backendAccountRepository.checkHealth()
                                .onSuccess {
                                    status = context.getString(R.string.backend_health_ok)
                                    error = null
                                }
                                .onFailure {
                                    error = context.getString(R.string.backend_health_fail, it.message ?: "")
                                    status = null
                                }
                        }
                    },
                    modifier = mod.fillMaxWidth()
                ) { Text(stringResource(R.string.backend_check_health)) }
            }
            if (cloudSessionActive) {
                Text(
                    stringResource(
                        R.string.backend_signed_in,
                        app.securePrefs.backendEmail ?: "",
                        app.securePrefs.backendRole ?: ""
                    )
                )
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            app.backendAccountRepository.logout()
                            cloudSessionActive = false
                            status = context.getString(R.string.backend_logout)
                        }
                    },
                    modifier = mod.fillMaxWidth()
                ) { Text(stringResource(R.string.backend_logout)) }
            } else {
                OutlinedTextField(
                    value = cloudEmail,
                    onValueChange = { cloudEmail = it },
                    label = { Text(stringResource(R.string.backend_email)) },
                    modifier = mod.fillMaxWidth(),
                colors = lindaProtectedOverlayTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    value = cloudPassword,
                    onValueChange = { cloudPassword = it },
                    label = { Text(stringResource(R.string.backend_password)) },
                    modifier = mod.fillMaxWidth(),
                colors = lindaProtectedOverlayTextFieldColors(),
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = cloudName,
                    onValueChange = { cloudName = it },
                    label = { Text(stringResource(R.string.backend_display_name)) },
                    modifier = mod.fillMaxWidth(),
                colors = lindaProtectedOverlayTextFieldColors()
                )
                Button(
                    onClick = {
                        val emailTrim = cloudEmail.trim()
                        when {
                            !emailTrim.contains("@") -> {
                                error = context.getString(R.string.backend_invalid_email)
                                status = null
                            }
                            cloudPassword.length < 8 -> {
                                error = context.getString(R.string.backend_password_short)
                                status = null
                            }
                            else -> scope.launch {
                                app.backendAccountRepository.register(emailTrim, cloudPassword, cloudName)
                                    .onSuccess {
                                        cloudSessionActive = true
                                        status = context.getString(R.string.backend_register_ok)
                                        error = null
                                        app.reportRepository.queueLocalReportsForSync()
                                        val cycle = app.syncCoordinator.syncNow()
                                        val syncMsg = cycle.reports?.localizedMessage(context) ?: cycle.message
                                        status = "${context.getString(R.string.backend_register_ok)} · $syncMsg"
                                    }
                                    .onFailure { error = it.message; status = null }
                            }
                        }
                    },
                    modifier = mod.fillMaxWidth()
                ) { Text(stringResource(R.string.backend_register)) }
                OutlinedButton(
                    onClick = {
                        val emailTrim = cloudEmail.trim()
                        when {
                            !emailTrim.contains("@") -> {
                                error = context.getString(R.string.backend_invalid_email)
                                status = null
                            }
                            cloudPassword.length < 8 -> {
                                error = context.getString(R.string.backend_password_short)
                                status = null
                            }
                            else -> scope.launch {
                                app.backendAccountRepository.login(emailTrim, cloudPassword)
                                    .onSuccess {
                                        cloudSessionActive = true
                                        status = context.getString(R.string.backend_login_ok)
                                        error = null
                                        app.reportRepository.queueLocalReportsForSync()
                                        val cycle = app.syncCoordinator.syncNow()
                                        val syncMsg = cycle.reports?.localizedMessage(context) ?: cycle.message
                                        status = "${context.getString(R.string.backend_login_ok)} · $syncMsg"
                                        if (app.securePrefs.shareAnonymousStats && app.backendSession.isUserRole()) {
                                            val buckets = app.statsRepository.prepareAnonymizedBuckets(true)
                                            app.analyticsUploadManager.uploadBuckets(buckets)
                                        }
                                    }
                                    .onFailure { error = it.message; status = null }
                            }
                        }
                    },
                    modifier = mod.fillMaxWidth()
                ) { Text(stringResource(R.string.backend_login)) }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = mod.fillMaxWidth()
            ) {
                Column(modifier = mod.weight(1f)) {
                    Text(stringResource(R.string.sync_bodies_title))
                    Text(stringResource(R.string.sync_bodies_help), style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = syncBodies,
                    onCheckedChange = {
                        syncBodies = it
                        app.securePrefs.syncReportBodies = it
                    }
                )
            }

            Text(stringResource(R.string.settings_anon_stats), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.settings_anon_help),
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = mod.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_prepare_anon))
                Switch(
                    checked = shareAnon,
                    onCheckedChange = { enabled ->
                        shareAnon = enabled
                        app.authRepository.setShareAnonymousStats(enabled)
                        error = null
                        if (enabled) {
                            scope.launch {
                                val buckets = app.statsRepository.prepareAnonymizedBuckets(true)
                                app.analyticsUploadManager.uploadBuckets(buckets)
                                    .onSuccess {
                                        status = context.getString(R.string.settings_anon_uploaded, it)
                                    }
                                    .onFailure {
                                        status = context.getString(R.string.settings_anon_on)
                                        // Keep prep enabled; upload may wait until USER login.
                                        if (it.message?.contains("USER session") == true) {
                                            error = it.message
                                        } else {
                                            error = it.message
                                        }
                                    }
                            }
                        } else {
                            status = context.getString(R.string.settings_anon_off)
                        }
                    }
                )
            }
            OutlinedButton(
                onClick = onOpenNgoDashboard,
                modifier = mod.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_open_ngo))
            }

            var escalationPhone by remember {
                mutableStateOf(app.securePrefs.escalationPhoneE164)
            }
            var escalationEnabled by remember {
                mutableStateOf(app.securePrefs.escalationSmsEnabled)
            }
            Text(stringResource(R.string.settings_escalation_title), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.settings_escalation_help),
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = escalationPhone,
                onValueChange = { escalationPhone = it },
                label = { Text(stringResource(R.string.settings_escalation_phone)) },
                singleLine = true,
                modifier = mod.fillMaxWidth()
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = mod.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_escalation_enable), modifier = mod.weight(1f))
                Switch(
                    checked = escalationEnabled,
                    onCheckedChange = { escalationEnabled = it }
                )
            }
            OutlinedButton(
                onClick = {
                    val normalized = ContactRepository.normalizePhone(escalationPhone)
                    if (escalationEnabled && (normalized == null || escalationPhone.isBlank())) {
                        error = context.getString(R.string.settings_escalation_invalid)
                        status = null
                        return@OutlinedButton
                    }
                    app.securePrefs.escalationPhoneE164 = escalationPhone.trim()
                    app.securePrefs.escalationSmsEnabled = escalationEnabled && normalized != null
                    escalationEnabled = app.securePrefs.escalationSmsEnabled
                    status = context.getString(R.string.settings_escalation_saved)
                    error = null
                },
                modifier = mod.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_save))
            }

            Text(stringResource(R.string.settings_about_title), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.settings_about_body, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall
            )
            Text(stringResource(R.string.settings_reset_title), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.settings_reset_help),
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = {
                    when (val pinResult = app.authRepository.attemptAccessPin(currentPin)) {
                        is AccessPinResult.Locked ->
                            error = context.getString(R.string.pin_locked, pinResult.remainingSeconds.toInt())
                        AccessPinResult.Wrong ->
                            error = context.getString(R.string.settings_need_current_for_reset)
                        AccessPinResult.Success -> {
                            scope.launch {
                                app.resetAllLocalData()
                                status = context.getString(R.string.settings_reset_done)
                                error = null
                                onDataReset()
                            }
                        }
                    }
                },
                modifier = mod.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.settings_reset_button))
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            status?.let { Text(it, color = androidx.compose.ui.graphics.Color(0xFF86EFAC)) }
        }
    }
}
