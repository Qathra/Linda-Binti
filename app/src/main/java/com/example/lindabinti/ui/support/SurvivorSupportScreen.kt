package com.example.lindabinti.ui.support

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.example.lindabinti.ui.common.LindaProtectedScaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.R
import com.example.lindabinti.data.content.SupportService
import com.example.lindabinti.data.content.SupportServiceArea
import com.example.lindabinti.data.content.SupportServiceKind
import com.example.lindabinti.data.content.SupportServicesCatalog
import com.example.lindabinti.data.content.SurvivorHelpAction
import com.example.lindabinti.data.content.SurvivorSupportCatalog
import com.example.lindabinti.ui.common.LindaSectionCard
import com.example.lindabinti.ui.common.ProtectedTopBar
import com.example.lindabinti.ui.common.dialNumber
import com.example.lindabinti.ui.locale.rememberAppLanguage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SurvivorSupportScreen(
    onBack: () -> Unit,
    onQuickHide: () -> Unit,
    onOpenEmergency: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenReporting: () -> Unit
) {
    val mod = androidx.compose.ui.Modifier
    val context = LocalContext.current
    val lang = rememberAppLanguage()
    val records by LindaBintiApp.instance.supportServicesRepository.records
        .collectAsStateWithLifecycle()
    val services = remember(lang, records) {
        SupportServicesCatalog.services(lang, records)
    }
    val truths = remember(lang) { SurvivorSupportCatalog.truths(lang) }
    val messages = remember(lang) { SurvivorSupportCatalog.messages(lang) }
    val steps = remember(lang) { SurvivorSupportCatalog.nextSteps(lang) }
    var selectedKind by remember { mutableStateOf<SupportServiceKind?>(null) }
    val filtered = remember(services, selectedKind) {
        services.filter { it.matches(selectedKind) }
    }

    fun handleShortcut(action: SurvivorHelpAction) {
        when (action) {
            SurvivorHelpAction.OPEN_EMERGENCY -> onOpenEmergency()
            SurvivorHelpAction.OPEN_CONTACTS -> onOpenContacts()
            SurvivorHelpAction.OPEN_REPORTING -> onOpenReporting()
            else -> selectedKind = SurvivorSupportCatalog.kindForAction(action)
        }
    }

    LindaProtectedScaffold(
        topBar = {
            ProtectedTopBar(
                title = stringResource(R.string.support_title),
                onBack = onBack,
                onQuickHide = onQuickHide
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = mod.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LindaSectionCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    IllustrationPlaceholder(
                        drawableRes = R.drawable.ic_support_placeholder_hero,
                        caption = stringResource(R.string.support_art_placeholder),
                        heightDp = 72
                    )
                    Text(
                        SurvivorSupportCatalog.coreMessage(lang),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(stringResource(R.string.support_intro), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.support_not_counsellor), style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                LindaSectionCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        stringResource(R.string.support_emergency_now),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        stringResource(R.string.support_emergency_now_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SupportServicesCatalog.emergencyNumbers().forEach { number ->
                            Button(onClick = { context.dialNumber(number) }) {
                                Text(stringResource(R.string.action_call, number))
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenEmergency,
                        modifier = mod.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.support_link_emergency))
                    }
                }
            }

            item {
                Text(stringResource(R.string.support_directory_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.support_directory_intro), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.support_offline_note), style = MaterialTheme.typography.bodySmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedKind == null,
                        onClick = { selectedKind = null },
                        label = { Text(stringResource(R.string.support_filter_all)) }
                    )
                    SupportServicesCatalog.directoryKinds.forEach { kind ->
                        FilterChip(
                            selected = selectedKind == kind,
                            onClick = { selectedKind = kind },
                            label = { Text(stringResource(kindLabelRes(kind))) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.support_filter_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(filtered, key = { it.id }) { service ->
                SupportServiceCard(service = service)
            }

            item {
                Text(stringResource(R.string.support_links_title), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.support_links_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SurvivorSupportCatalog.shortcuts.forEach { shortcut ->
                        OutlinedButton(onClick = { handleShortcut(shortcut.action) }) {
                            Text(stringResource(shortcutLabelRes(shortcut.action)))
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.support_truths_title), fontWeight = FontWeight.SemiBold)
                IllustrationPlaceholder(
                    drawableRes = R.drawable.ic_support_placeholder_worth,
                    caption = stringResource(R.string.support_art_placeholder),
                    heightDp = 72
                )
            }
            items(truths, key = { it.id }) { truth ->
                LindaSectionCard {
                    Text(truth.title, fontWeight = FontWeight.SemiBold)
                    Text(truth.body, style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                Text(stringResource(R.string.support_messages_title), fontWeight = FontWeight.SemiBold)
            }
            items(messages, key = { it.id }) { message ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = mod.fillMaxWidth()
                ) {
                    Text(
                        message.text,
                        modifier = mod.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            item {
                Text(stringResource(R.string.support_now_title), fontWeight = FontWeight.SemiBold)
                IllustrationPlaceholder(
                    drawableRes = R.drawable.ic_support_placeholder_path,
                    caption = stringResource(R.string.support_art_placeholder),
                    heightDp = 72
                )
            }
            items(steps, key = { it.id }) { step ->
                LindaSectionCard {
                    Text(step.title, fontWeight = FontWeight.SemiBold)
                    Text(step.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SupportServiceCard(service: SupportService) {
    val mod = androidx.compose.ui.Modifier
    val context = LocalContext.current
    LindaSectionCard {
        Text(service.name, fontWeight = FontWeight.SemiBold)
        Text(
            stringResource(kindLabelRes(service.kind)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            stringResource(
                if (service.area == SupportServiceArea.KAJIADO) {
                    R.string.support_area_kajiado
                } else {
                    R.string.support_area_kenya
                }
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.support_helps_label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        Text(service.helpsWith, style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(R.string.support_location_label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        Text(service.location, style = MaterialTheme.typography.bodyMedium)
        if (!service.locationVerified) {
            Text(
                stringResource(R.string.support_location_unverified),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            stringResource(R.string.support_referral_label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        Text(service.referralGuidance, style = MaterialTheme.typography.bodySmall)
        val numbers = service.callNumbers()
        if (numbers.isEmpty()) {
            Text(
                stringResource(R.string.support_phone_unverified),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            numbers.forEach { number ->
                Button(
                    onClick = { context.dialNumber(number) },
                    modifier = mod.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.action_call, number))
                }
            }
        }
    }
}

private fun shortcutLabelRes(action: SurvivorHelpAction): Int = when (action) {
    SurvivorHelpAction.OPEN_EMERGENCY -> R.string.support_link_emergency
    SurvivorHelpAction.OPEN_CONTACTS -> R.string.support_link_contacts
    SurvivorHelpAction.OPEN_REPORTING -> R.string.support_link_reporting
    SurvivorHelpAction.FILTER_GBV -> R.string.support_link_gbv
    SurvivorHelpAction.FILTER_MEDICAL -> R.string.support_link_medical
    SurvivorHelpAction.FILTER_COUNSELLING -> R.string.support_link_counselling
    SurvivorHelpAction.FILTER_POLICE -> R.string.support_link_police
    SurvivorHelpAction.FILTER_CHILD -> R.string.support_link_child
    SurvivorHelpAction.FILTER_FGM -> R.string.support_link_fgm
}

private fun kindLabelRes(kind: SupportServiceKind): Int = when (kind) {
    SupportServiceKind.EMERGENCY -> R.string.support_kind_emergency
    SupportServiceKind.GBV -> R.string.support_kind_gbv
    SupportServiceKind.MEDICAL -> R.string.support_kind_medical
    SupportServiceKind.COUNSELLING -> R.string.support_kind_counselling
    SupportServiceKind.POLICE -> R.string.support_kind_police
    SupportServiceKind.CHILD_PROTECTION -> R.string.support_kind_child
    SupportServiceKind.FGM -> R.string.support_kind_fgm
}

@Composable
private fun IllustrationPlaceholder(
    drawableRes: Int,
    caption: String,
    heightDp: Int
) {
    val mod = androidx.compose.ui.Modifier
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = mod.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surface,
            modifier = mod.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(drawableRes),
                contentDescription = caption,
                contentScale = ContentScale.FillWidth,
                modifier = mod
                    .fillMaxWidth()
                    .height(heightDp.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }
        Text(
            caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
