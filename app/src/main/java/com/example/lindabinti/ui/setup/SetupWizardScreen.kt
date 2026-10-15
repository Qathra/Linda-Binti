package com.example.lindabinti.ui.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.R
import com.example.lindabinti.data.content.SupportedLanguages
import com.example.lindabinti.data.repository.AuthRepository
import com.example.lindabinti.data.repository.ContactRepository
import com.example.lindabinti.ui.common.lindaInputTextStyle
import com.example.lindabinti.ui.common.lindaOutlinedTextFieldColors
import com.example.lindabinti.ui.locale.localizedContactMessage
import com.example.lindabinti.util.LocaleHelper
import com.example.lindabinti.util.PermissionHelper
import kotlinx.coroutines.launch

private data class DraftContact(
    val name: String,
    val phone: String,
    val relationship: String
)

@Composable
fun SetupWizardScreen(
    onSetupComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val mod = androidx.compose.ui.Modifier
    val app = LindaBintiApp.instance
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 0 language → 1 name → 2 access → 3 sos → 4 contacts → 5 permissions
    var step by remember { mutableIntStateOf(0) }
    var language by remember { mutableStateOf(app.authRepository.getLanguage()) }
    var userName by remember { mutableStateOf("") }
    var accessPin by remember { mutableStateOf("") }
    var accessPinConfirm by remember { mutableStateOf("") }
    var sosCode by remember { mutableStateOf("") }
    var sosCodeConfirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val drafts = remember { mutableStateListOf<DraftContact>() }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactRel by remember { mutableStateOf("") }

    var smsGranted by remember { mutableStateOf(PermissionHelper.smsGranted(context)) }
    var locationGranted by remember { mutableStateOf(PermissionHelper.locationGranted(context)) }
    var notificationsGranted by remember {
        mutableStateOf(PermissionHelper.notificationsGranted(context))
    }

    fun refreshPermissionState() {
        smsGranted = PermissionHelper.smsGranted(context)
        locationGranted = PermissionHelper.locationGranted(context)
        notificationsGranted = PermissionHelper.notificationsGranted(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissionState()
    }

    fun requestPermissions() {
        permissionLauncher.launch(PermissionHelper.sosPermissionArray())
    }

    Surface(
        modifier = mod.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
    Column(
        modifier = mod
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.setup_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.setup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )

        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        when (step) {
            0 -> {
                Text(stringResource(R.string.setup_language), fontWeight = FontWeight.SemiBold)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = language == SupportedLanguages.EN,
                        onClick = {
                            language = SupportedLanguages.EN
                            app.authRepository.setLanguage(SupportedLanguages.EN)
                            LocaleHelper.applyLanguage(SupportedLanguages.EN)
                        },
                        label = { Text(stringResource(R.string.lang_english)) }
                    )
                    FilterChip(
                        selected = language == SupportedLanguages.SW,
                        onClick = {
                            language = SupportedLanguages.SW
                            app.authRepository.setLanguage(SupportedLanguages.SW)
                            LocaleHelper.applyLanguage(SupportedLanguages.SW)
                        },
                        label = { Text(stringResource(R.string.lang_kiswahili)) }
                    )
                    FilterChip(
                        selected = language == SupportedLanguages.MAA,
                        onClick = {
                            language = SupportedLanguages.MAA
                            app.authRepository.setLanguage(SupportedLanguages.MAA)
                            LocaleHelper.applyLanguage(SupportedLanguages.MAA)
                        },
                        label = { Text(stringResource(R.string.lang_maa)) }
                    )
                    FilterChip(
                        selected = language == SupportedLanguages.SO,
                        onClick = {
                            language = SupportedLanguages.SO
                            app.authRepository.setLanguage(SupportedLanguages.SO)
                            LocaleHelper.applyLanguage(SupportedLanguages.SO)
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
                Spacer(modifier = mod.height(8.dp))
                Button(onClick = {
                    app.authRepository.setLanguage(language)
                    LocaleHelper.applyLanguage(language)
                    step = 1
                    error = null
                }, modifier = mod.fillMaxWidth()) {
                    Text(stringResource(R.string.action_continue))
                }
            }

            1 -> {
                Text(stringResource(R.string.setup_name_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.setup_name_help))
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it.take(60) },
                    label = { Text(stringResource(R.string.setup_name_label)) },
                    modifier = mod.fillMaxWidth(),
                    textStyle = lindaInputTextStyle(),
                    colors = lindaOutlinedTextFieldColors()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { step = 0 }) {
                        Text(stringResource(R.string.action_back))
                    }
                    Button(onClick = {
                        if (userName.isBlank()) {
                            error = context.getString(R.string.setup_name_required)
                        } else {
                            error = null
                            step = 2
                        }
                    }) { Text(stringResource(R.string.action_continue)) }
                }
            }

            2 -> {
                Text(stringResource(R.string.setup_access_pin_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.setup_access_pin_help))
                OutlinedTextField(
                    value = accessPin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) accessPin = it },
                    label = { Text(stringResource(R.string.setup_access_pin_title)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = mod.fillMaxWidth(),
                    textStyle = lindaInputTextStyle(),
                    colors = lindaOutlinedTextFieldColors()
                )
                OutlinedTextField(
                    value = accessPinConfirm,
                    onValueChange = {
                        if (it.length <= 6 && it.all(Char::isDigit)) accessPinConfirm = it
                    },
                    label = { Text(stringResource(R.string.setup_access_confirm)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = mod.fillMaxWidth(),
                    textStyle = lindaInputTextStyle(),
                    colors = lindaOutlinedTextFieldColors()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { step = 1 }) {
                        Text(stringResource(R.string.action_back))
                    }
                    Button(onClick = {
                        when {
                            accessPin.length !in 4..6 ->
                                error = context.getString(R.string.setup_access_len)
                            accessPin != accessPinConfirm ->
                                error = context.getString(R.string.setup_access_mismatch)
                            AuthRepository.codesAreIdentical(accessPin, sosCode) ->
                                error = context.getString(R.string.setup_codes_must_differ)
                            else -> {
                                error = null
                                step = 3
                            }
                        }
                    }) { Text(stringResource(R.string.action_continue)) }
                }
            }

            3 -> {
                Text(stringResource(R.string.setup_sos_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.setup_sos_help))
                OutlinedTextField(
                    value = sosCode,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) sosCode = it },
                    label = { Text(stringResource(R.string.setup_sos_title)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = mod.fillMaxWidth(),
                    textStyle = lindaInputTextStyle(),
                    colors = lindaOutlinedTextFieldColors()
                )
                OutlinedTextField(
                    value = sosCodeConfirm,
                    onValueChange = {
                        if (it.length <= 8 && it.all(Char::isDigit)) sosCodeConfirm = it
                    },
                    label = { Text(stringResource(R.string.setup_sos_confirm)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = mod.fillMaxWidth(),
                    textStyle = lindaInputTextStyle(),
                    colors = lindaOutlinedTextFieldColors()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { step = 2 }) {
                        Text(stringResource(R.string.action_back))
                    }
                    Button(onClick = {
                        when {
                            sosCode.length < 4 -> error = context.getString(R.string.setup_sos_len)
                            sosCode != sosCodeConfirm ->
                                error = context.getString(R.string.setup_sos_mismatch)
                            AuthRepository.codesAreIdentical(accessPin, sosCode) ->
                                error = context.getString(R.string.setup_codes_must_differ)
                            else -> {
                                error = null
                                step = 4
                            }
                        }
                    }) { Text(stringResource(R.string.action_continue)) }
                }
            }

            4 -> {
                Text(stringResource(R.string.setup_contacts_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.setup_contacts_help))

                drafts.forEachIndexed { index, c ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = mod.fillMaxWidth()
                    ) {
                        Row(
                            modifier = mod.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = mod.weight(1f)) {
                                Text(c.name, fontWeight = FontWeight.Medium)
                                Text(c.phone)
                                if (c.relationship.isNotBlank()) Text(c.relationship)
                            }
                            TextButton(onClick = { drafts.removeAt(index) }) {
                                Text(stringResource(R.string.action_delete))
                            }
                        }
                    }
                }

                if (drafts.size < ContactRepository.MAX_CONTACTS) {
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text(stringResource(R.string.contacts_name)) },
                        modifier = mod.fillMaxWidth(),
                        textStyle = lindaInputTextStyle(),
                        colors = lindaOutlinedTextFieldColors()
                    )
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text(stringResource(R.string.contacts_phone)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = mod.fillMaxWidth(),
                        textStyle = lindaInputTextStyle(),
                        colors = lindaOutlinedTextFieldColors()
                    )
                    OutlinedTextField(
                        value = contactRel,
                        onValueChange = { contactRel = it },
                        label = { Text(stringResource(R.string.contacts_relationship)) },
                        modifier = mod.fillMaxWidth(),
                        textStyle = lindaInputTextStyle(),
                        colors = lindaOutlinedTextFieldColors()
                    )
                    OutlinedButton(
                        onClick = {
                            val normalized = ContactRepository.normalizePhone(contactPhone)
                            when {
                                drafts.size >= ContactRepository.MAX_CONTACTS ->
                                    error = context.getString(R.string.setup_contacts_max)
                                normalized == null ->
                                    error = context.getString(R.string.setup_contacts_invalid_phone)
                                drafts.any { it.phone == normalized } ->
                                    error = context.getString(R.string.setup_contacts_duplicate)
                                else -> {
                                    drafts.add(
                                        DraftContact(
                                            name = contactName.trim().ifBlank { normalized },
                                            phone = normalized,
                                            relationship = contactRel.trim()
                                        )
                                    )
                                    contactName = ""
                                    contactPhone = ""
                                    contactRel = ""
                                    error = null
                                }
                            }
                        },
                        modifier = mod.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.contacts_add))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { step = 3 }) {
                        Text(stringResource(R.string.action_back))
                    }
                    Button(onClick = {
                        if (drafts.size < ContactRepository.MIN_CONTACTS) {
                            error = context.getString(R.string.setup_contacts_min)
                        } else {
                            error = null
                            step = 5
                            requestPermissions()
                        }
                    }) { Text(stringResource(R.string.action_continue)) }
                }
            }

            5 -> {
                Text(stringResource(R.string.setup_permissions_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.setup_permissions_help))
                Text(
                    stringResource(
                        R.string.setup_perm_sms_label,
                        if (smsGranted) stringResource(R.string.perm_granted) else stringResource(R.string.perm_denied)
                    )
                )
                Text(
                    stringResource(
                        R.string.setup_perm_location_label,
                        if (locationGranted) stringResource(R.string.perm_granted) else stringResource(R.string.perm_denied)
                    )
                )
                Text(
                    stringResource(
                        R.string.setup_perm_notifications_label,
                        if (notificationsGranted) stringResource(R.string.perm_granted) else stringResource(R.string.perm_denied)
                    )
                )
                Text(
                    stringResource(R.string.perm_denied_help),
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(onClick = { requestPermissions() }, modifier = mod.fillMaxWidth()) {
                    Text(stringResource(R.string.perm_retry))
                }
                OutlinedButton(
                    onClick = { PermissionHelper.openAppSettings(context) },
                    modifier = mod.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.perm_open_settings))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { step = 4 }) {
                        Text(stringResource(R.string.action_back))
                    }
                    Button(
                        onClick = {
                            refreshPermissionState()
                            if (!smsGranted) {
                                error = context.getString(R.string.perm_sms_required)
                                return@Button
                            }
                            if (AuthRepository.codesAreIdentical(accessPin, sosCode)) {
                                error = context.getString(R.string.setup_codes_must_differ)
                                return@Button
                            }
                            scope.launch {
                                try {
                                    for (d in drafts) {
                                        val result = app.contactRepository.addContact(
                                            d.name, d.phone, d.relationship
                                        )
                                        if (result.isFailure) {
                                            error = result.exceptionOrNull()?.localizedContactMessage(context)
                                            return@launch
                                        }
                                    }
                                    app.authRepository.completeSetup(
                                        userName = userName.trim(),
                                        accessPin = accessPin,
                                        sosCode = sosCode,
                                        language = language
                                    )
                                    onSetupComplete()
                                } catch (e: Exception) {
                                    error = e.message ?: context.getString(R.string.setup_failed)
                                }
                            }
                        },
                        modifier = mod.weight(1f)
                    ) {
                        Text(stringResource(R.string.setup_finish))
                    }
                }
            }
        }

        if (step == 0) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.setup_cancel_return))
            }
        }
    }
    }
}