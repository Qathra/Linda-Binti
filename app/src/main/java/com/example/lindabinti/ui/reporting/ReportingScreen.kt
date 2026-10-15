package com.example.lindabinti.ui.reporting

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.example.lindabinti.ui.common.LindaProtectedScaffold
import com.example.lindabinti.ui.common.LindaSectionCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.R
import com.example.lindabinti.data.content.GbvReportCategories
import com.example.lindabinti.data.content.GbvReportGroup
import com.example.lindabinti.data.content.ReportingFormRules
import com.example.lindabinti.data.sync.SyncStatus
import com.example.lindabinti.ui.common.ConnectivityStatusChip
import com.example.lindabinti.ui.common.lindaProtectedOverlayTextFieldColors
import com.example.lindabinti.ui.common.ProtectedTopBar
import com.example.lindabinti.ui.locale.localizedMessage
import com.example.lindabinti.util.PermissionHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportingScreen(
    onBack: () -> Unit,
    onQuickHide: () -> Unit,
    onAnalyzeReport: (Long) -> Unit = {}
) {
    val mod = androidx.compose.ui.Modifier
    val context = LocalContext.current
    val app = LindaBintiApp.instance
    val reports by app.reportRepository.observeReports()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val online by app.networkMonitor.isOnline.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var selectedId by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingAnalysisReportId by remember { mutableStateOf<Long?>(null) }
    var locationGranted by remember { mutableStateOf(PermissionHelper.locationGranted(context)) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        locationGranted = PermissionHelper.locationGranted(context)
    }

    fun categoryLabel(id: String): String = context.getString(labelResFor(id))

    fun statusLabel(status: String): String = when (status) {
        SyncStatus.PENDING -> context.getString(R.string.reporting_sync_pending)
        SyncStatus.SYNCED -> context.getString(R.string.reporting_sync_synced)
        SyncStatus.FAILED -> context.getString(R.string.reporting_sync_failed)
        else -> context.getString(R.string.reporting_device_only)
    }

    LindaProtectedScaffold(
        topBar = {
            ProtectedTopBar(
                title = stringResource(R.string.reporting_title),
                onBack = onBack,
                onQuickHide = onQuickHide
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = mod.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ConnectivityStatusChip()
                Text(
                    if (online) {
                        stringResource(R.string.reporting_connectivity_online_hint)
                    } else {
                        stringResource(R.string.reporting_connectivity_offline_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = mod.padding(top = 6.dp)
                )
            }
            item {
                LindaSectionCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text(stringResource(R.string.reporting_intro), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.reporting_no_pii), style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                Text(stringResource(R.string.reporting_step_category), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.reporting_category_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            GbvReportCategories.grouped.forEach { (group, categories) ->
                item {
                    Text(
                        stringResource(groupTitleRes(group)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = mod.padding(top = 4.dp)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedId == category.id,
                                onClick = { selectedId = category.id },
                                label = { Text(stringResource(labelResFor(category.id))) }
                            )
                        }
                    }
                }
            }

            if (selectedId != null) {
                item {
                    Text(stringResource(R.string.reporting_step_description), fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.reporting_body_label)) },
                        modifier = mod.fillMaxWidth(),
                        minLines = 3,
                        colors = lindaProtectedOverlayTextFieldColors()
                    )
                }
                item {
                    LindaSectionCard {
                        Text(
                            stringResource(R.string.reporting_area_opt_in_title),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.reporting_area_opt_in_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!locationGranted) {
                            Text(
                                stringResource(R.string.reporting_area_permission_missing),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        )
                                    )
                                }
                            ) {
                                Text(stringResource(R.string.reporting_area_allow_location))
                            }
                        }
                    }
                }
            }

            item {
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                pendingAnalysisReportId?.let { reportId ->
                    ReportSavedAiOfferCard(
                        onAnalyze = {
                            pendingAnalysisReportId = null
                            onAnalyzeReport(reportId)
                        },
                        onDismiss = { pendingAnalysisReportId = null }
                    )
                }
                Button(
                    onClick = {
                        val categoryId = selectedId
                        if (!ReportingFormRules.canSave(categoryId) || categoryId == null) {
                            message = context.getString(R.string.reporting_need_category)
                            return@Button
                        }
                        val label = categoryLabel(categoryId)
                        val body = ReportingFormRules.storedDescription(description)
                        scope.launch {
                            val area = runCatching { app.reportAreaResolver.resolveAreaOrNull() }.getOrNull()
                            val saved = app.reportRepository.addReport(
                                categoryLabel = label,
                                body = body,
                                categoryId = categoryId,
                                area = area
                            )
                            description = ""
                            selectedId = null
                            message = if (ReportingFormRules.isDeviceOnly(saved.syncStatus)) {
                                if (area != null) {
                                    context.getString(R.string.reporting_saved_with_area, area.label)
                                } else {
                                    context.getString(R.string.reporting_saved)
                                }
                            } else {
                                if (area != null) {
                                    context.getString(R.string.reporting_saved_queued_with_area, area.label)
                                } else {
                                    context.getString(R.string.reporting_saved_queued)
                                }
                            }
                            if (ReportingFormRules.shouldAttemptSyncAfterSave(saved.syncStatus)) {
                                runCatching { app.syncCoordinator.syncNow() }
                            }
                            pendingAnalysisReportId = saved.id
                        }
                    },
                    enabled = ReportingFormRules.canSave(selectedId),
                    modifier = mod.fillMaxWidth()
                ) { Text(stringResource(R.string.reporting_save)) }
            }

            item {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val cycle = app.syncCoordinator.syncNow()
                            message = cycle.reports?.localizedMessage(context) ?: cycle.message
                        }
                    },
                    modifier = mod.fillMaxWidth()
                ) { Text(stringResource(R.string.reporting_sync_now)) }
                Text(
                    stringResource(R.string.reporting_sync_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Text(stringResource(R.string.reporting_saved_list), fontWeight = FontWeight.SemiBold)
            }

            if (reports.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.reporting_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(reports, key = { it.id }) { report ->
                LindaSectionCard {
                    Text(report.category, fontWeight = FontWeight.SemiBold)
                    Text(
                        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ENGLISH).format(Date(report.createdAt)),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        statusLabel(report.syncStatus),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (ReportingFormRules.isDeviceOnly(report.syncStatus)) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                    if (report.body.isBlank()) {
                        Text(
                            stringResource(R.string.reporting_no_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(report.body, style = MaterialTheme.typography.bodySmall)
                    }
                    report.areaLabel?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            context.getString(R.string.reporting_area_saved_label, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { scope.launch { app.reportRepository.delete(report.id) } }) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            }
        }
    }
}

private fun groupTitleRes(group: GbvReportGroup): Int = when (group) {
    GbvReportGroup.GBV -> R.string.reporting_group_gbv
    GbvReportGroup.FGM -> R.string.reporting_group_fgm
    GbvReportGroup.RELATED_PROTECTION -> R.string.reporting_group_protection
    GbvReportGroup.OTHER -> R.string.reporting_group_other
}

private fun labelResFor(id: String): Int = when (id) {
    "physical" -> R.string.reporting_cat_physical
    "sexual" -> R.string.reporting_cat_sexual
    "emotional" -> R.string.reporting_cat_emotional
    "domestic" -> R.string.reporting_cat_domestic
    "harassment" -> R.string.reporting_cat_harassment
    "threats" -> R.string.reporting_cat_threats
    "fgm" -> R.string.reporting_cat_fgm
    "fgm_incident" -> R.string.reporting_cat_fgm_incident
    "fgm_pressure" -> R.string.reporting_cat_fgm_pressure
    "forced_marriage" -> R.string.reporting_cat_marriage
    else -> R.string.reporting_cat_other
}
