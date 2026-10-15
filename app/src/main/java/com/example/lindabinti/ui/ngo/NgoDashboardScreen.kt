package com.example.lindabinti.ui.ngo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.R
import com.example.lindabinti.data.remote.AnalyticsSummaryDto
import com.example.lindabinti.data.remote.ReportFlagDto
import com.example.lindabinti.data.stats.NgoDashboardSnapshot
import com.example.lindabinti.ui.common.LindaSectionCard
import com.example.lindabinti.ui.common.ProtectedTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun NgoDashboardScreen(
    onBack: () -> Unit,
    onQuickHide: () -> Unit
) {
    val mod = androidx.compose.ui.Modifier
    val app = LindaBintiApp.instance
    val scope = rememberCoroutineScope()
    var showDemo by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<NgoDashboardSnapshot?>(null) }
    var remote by remember { mutableStateOf<AnalyticsSummaryDto?>(null) }
    var flags by remember { mutableStateOf<List<ReportFlagDto>?>(null) }
    var remoteError by remember { mutableStateOf<String?>(null) }
    var retryingId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showDemo) {
        snapshot = if (showDemo) {
            app.statsRepository.buildDemoSnapshot()
        } else {
            app.statsRepository.buildSnapshot(includeDemoIfEmpty = false)
        }
        app.statsRepository.prepareAnonymizedBuckets(app.authRepository.isShareAnonymousStats())
    }

    LindaProtectedScaffold(
        topBar = {
            ProtectedTopBar(
                title = stringResource(R.string.ngo_title),
                onBack = onBack,
                onQuickHide = onQuickHide
            )
        },
    ) { padding ->
        Column(
            modifier = mod
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.ngo_intro), style = MaterialTheme.typography.bodyMedium)

            Text(stringResource(R.string.ngo_remote_title), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.ngo_remote_note), style = MaterialTheme.typography.bodySmall)
            if (!app.backendSession.isNgoRole()) {
                Text(stringResource(R.string.ngo_remote_locked), style = MaterialTheme.typography.bodySmall)
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            remoteError = null
                            app.backendAccountRepository.fetchNgoSummary()
                                .onSuccess { remote = it }
                                .onFailure { remoteError = it.message }
                            app.backendAccountRepository.fetchNgoReportFlags()
                                .onSuccess { flags = it }
                                .onFailure { err ->
                                    if (remoteError == null) remoteError = err.message
                                }
                        }
                    },
                    modifier = mod.fillMaxWidth()
                ) { Text(stringResource(R.string.ngo_load_remote)) }
                remoteError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                remote?.let { summary ->
                    LindaSectionCard {
                        Text(summary.note, style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.ngo_threshold, summary.minThreshold))
                        Text(stringResource(R.string.ngo_sos_server, summary.sosEvents))
                        Text(stringResource(R.string.ngo_education_server, summary.educationCompletions))
                        Text(stringResource(R.string.ngo_reports_by_category), fontWeight = FontWeight.SemiBold)
                        if (summary.reportsByCategory.isEmpty()) {
                            Text(stringResource(R.string.ngo_empty), style = MaterialTheme.typography.bodySmall)
                        } else {
                            summary.reportsByCategory.forEach { b ->
                                val mark = if (b.suppressed) " (suppressed)" else ""
                                Text("• ${b.key}: ${b.count}$mark")
                            }
                        }
                        if (summary.riskByBand.isNotEmpty()) {
                            Text(stringResource(R.string.ngo_risk_high), fontWeight = FontWeight.SemiBold)
                            summary.riskByBand.forEach { b ->
                                Text("• ${b.key}: ${b.count}")
                            }
                        }
                    }
                }
                flags?.let { list ->
                    Text(stringResource(R.string.ngo_flags_title), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.ngo_flags_note), style = MaterialTheme.typography.bodySmall)
                    if (list.isEmpty()) {
                        Text(stringResource(R.string.ngo_flags_empty), style = MaterialTheme.typography.bodySmall)
                    } else {
                        list.forEach { flag ->
                            LindaSectionCard {
                                Text(
                                    stringResource(
                                        R.string.ngo_flag_line,
                                        flag.urgency.uppercase(Locale.ENGLISH),
                                        flag.categoryLabel.ifBlank { flag.categoryId },
                                        flag.weekLabel
                                    ),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    stringResource(R.string.ngo_flag_risk, flag.urgency.uppercase(Locale.ENGLISH)),
                                    fontWeight = FontWeight.Medium
                                )
                                if (flag.gbvFgmRelevance.isNotBlank()) {
                                    Text(
                                        stringResource(R.string.ngo_flag_relevance, flag.gbvFgmRelevance),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                val category = flag.incidentCategory.ifBlank { flag.categoryLabel }
                                if (category.isNotBlank()) {
                                    Text(
                                        stringResource(R.string.ngo_flag_category, category),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (flag.urgencyIndicators.isNotBlank()) {
                                    Text(
                                        stringResource(R.string.ngo_flag_indicators, flag.urgencyIndicators),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (flag.safetyConcerns.isNotBlank()) {
                                    Text(
                                        stringResource(R.string.ngo_flag_safety, flag.safetyConcerns),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (flag.recommendedPriority.isNotBlank()) {
                                    Text(
                                        stringResource(
                                            R.string.ngo_flag_priority,
                                            flag.recommendedPriority.uppercase(Locale.ENGLISH)
                                        ),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    stringResource(
                                        R.string.ngo_flag_ai_status,
                                        aiStatusLabel(flag.aiStatus)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (flag.aiStatus.equals("complete", ignoreCase = true)) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.tertiary
                                    }
                                )
                                Text(
                                    stringResource(R.string.ngo_flag_reason, flag.explanation),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (flag.referenceId.isNotBlank() || flag.caseStatus.isNotBlank()) {
                                    Text(
                                        stringResource(
                                            R.string.ngo_flag_case,
                                            flag.referenceId.ifBlank { flag.serverReportId.take(8) },
                                            flag.caseStatus.ifBlank { "NEW" }
                                        ),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(flag.disclaimer, style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            retryingId = flag.serverReportId
                                            remoteError = null
                                            app.backendAccountRepository.retryReportFlagAnalysis(flag.serverReportId)
                                                .onSuccess { updated ->
                                                    flags = flags.orEmpty().map { current ->
                                                        if (current.serverReportId == updated.serverReportId) {
                                                            updated
                                                        } else {
                                                            current
                                                        }
                                                    }
                                                }
                                                .onFailure { remoteError = it.message }
                                            retryingId = null
                                        }
                                    },
                                    enabled = retryingId == null,
                                    modifier = mod.fillMaxWidth()
                                ) {
                                    Text(
                                        stringResource(
                                            if (retryingId == flag.serverReportId) {
                                                R.string.ngo_flag_retrying
                                            } else {
                                                R.string.ngo_flag_retry
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(stringResource(R.string.ngo_local_preview), fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showDemo,
                    onClick = { showDemo = false },
                    label = { Text(stringResource(R.string.ngo_show_device)) }
                )
                FilterChip(
                    selected = showDemo,
                    onClick = { showDemo = true },
                    label = { Text(stringResource(R.string.ngo_show_demo)) }
                )
            }

            val snap = snapshot
            if (snap == null) {
                Text(stringResource(R.string.ngo_loading))
            } else if (!snap.fromDevice && !snap.isDemoSample) {
                Text(stringResource(R.string.ngo_empty), style = MaterialTheme.typography.titleMedium)
            } else {
                Text(
                    stringResource(if (snap.isDemoSample) R.string.ngo_demo_banner else R.string.ngo_device_banner),
                    color = if (snap.isDemoSample) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(snap.note, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                Text(
                    stringResource(
                        R.string.ngo_generated,
                        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ENGLISH).format(Date(snap.generatedAtMillis))
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = mod.fillMaxWidth()) {
                    StatCard(stringResource(R.string.ngo_sos_events), snap.sosTotal.toString(), mod.weight(1f))
                    StatCard(stringResource(R.string.ngo_reports), snap.reportsTotal.toString(), mod.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = mod.fillMaxWidth()) {
                    StatCard(stringResource(R.string.ngo_risk_critical), snap.riskCritical.toString(), mod.weight(1f))
                    StatCard(stringResource(R.string.ngo_risk_high), snap.riskHigh.toString(), mod.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = mod.fillMaxWidth()) {
                    StatCard(stringResource(R.string.ngo_risk_elevated), snap.riskElevated.toString(), mod.weight(1f))
                    StatCard(stringResource(R.string.ngo_risk_lower), snap.riskLower.toString(), mod.weight(1f))
                }
                Text(stringResource(R.string.ngo_reports_by_category), fontWeight = FontWeight.SemiBold)
                if (snap.reportByCategory.isEmpty()) {
                    Text(stringResource(R.string.ngo_empty), style = MaterialTheme.typography.bodySmall)
                } else {
                    snap.reportByCategory.forEach { (cat, count) -> Text("• $cat: $count") }
                }
                Text(stringResource(R.string.ngo_sos_by_week), fontWeight = FontWeight.SemiBold)
                snap.weeklySos.toSortedMap().forEach { (week, count) -> Text("• $week: $count") }
                Text(stringResource(R.string.ngo_upload_note), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun aiStatusLabel(status: String): String {
    return when (status.lowercase(Locale.ENGLISH)) {
        "complete" -> stringResource(R.string.ngo_flag_ai_complete)
        "pending" -> stringResource(R.string.ngo_flag_ai_pending)
        "timeout" -> stringResource(R.string.ngo_flag_ai_timeout)
        "invalid" -> stringResource(R.string.ngo_flag_ai_invalid)
        "missing" -> stringResource(R.string.ngo_flag_ai_missing)
        "failed" -> stringResource(R.string.ngo_flag_ai_failed)
        else -> stringResource(R.string.ngo_flag_ai_unavailable)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    LindaSectionCard(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
