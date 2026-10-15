package com.example.lindabinti.ui.reporting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.example.lindabinti.ui.common.LindaProtectedScaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.R
import com.example.lindabinti.ai.AiAnalysisResult
import com.example.lindabinti.ai.AiSeverity
import com.example.lindabinti.ui.common.LindaSectionCard
import com.example.lindabinti.ui.common.ProtectedTopBar
import com.example.lindabinti.ui.locale.rememberAppLanguage
import com.example.lindabinti.ui.theme.LindaDanger
import com.example.lindabinti.ui.theme.LindaLavenderSoft
import com.example.lindabinti.ui.theme.LindaPurple
import com.example.lindabinti.ui.theme.LindaPurpleDark
import com.example.lindabinti.ui.theme.LindaSuccess
import com.example.lindabinti.ui.theme.LindaWarning

private sealed interface AnalysisUiState {
    data object Loading : AnalysisUiState
    data class Ready(val result: AiAnalysisResult, val categoryId: String) : AnalysisUiState
    data object MissingReport : AnalysisUiState
}

@Composable
fun ReportAiAnalysisScreen(
    reportId: Long,
    onBack: () -> Unit,
    onQuickHide: () -> Unit,
    onOpenEmergency: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSafetyGuidance: () -> Unit,
    onOpenEducation: (topicId: String?) -> Unit
) {
    val mod = Modifier
    val app = LindaBintiApp.instance
    val context = LocalContext.current
    val lang = rememberAppLanguage()
    var state by remember(reportId) { mutableStateOf<AnalysisUiState>(AnalysisUiState.Loading) }

    LaunchedEffect(reportId) {
        state = AnalysisUiState.Loading
        val report = app.reportRepository.getById(reportId)
        if (report == null) {
            state = AnalysisUiState.MissingReport
            return@LaunchedEffect
        }
        val result = ReportAnalysisAnalyzer.analyzeReport(
            app.aiOrchestrator,
            report,
            app.database.reportDao()
        )
        state = AnalysisUiState.Ready(result, report.categoryId)
    }

    LindaProtectedScaffold(
        topBar = {
            ProtectedTopBar(
                title = stringResource(R.string.report_analysis_title),
                onBack = onBack,
                onQuickHide = onQuickHide
            )
        },
    ) { padding ->
        Box(
            modifier = mod
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (val ui = state) {
                AnalysisUiState.Loading -> AnalysisLoadingCard(modifier = mod.align(Alignment.Center))
                AnalysisUiState.MissingReport -> Text(
                    stringResource(R.string.report_analysis_missing),
                    modifier = mod.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                is AnalysisUiState.Ready -> AnalysisResultContent(
                    result = ui.result,
                    categoryId = ui.categoryId,
                    language = lang,
                    onOpenEmergency = onOpenEmergency,
                    onOpenContacts = onOpenContacts,
                    onOpenSafetyGuidance = onOpenSafetyGuidance,
                    onOpenEducation = onOpenEducation,
                    modifier = mod.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun AnalysisLoadingCard(modifier: Modifier = Modifier) {
    LindaSectionCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Text(
                stringResource(R.string.report_analysis_loading),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                stringResource(R.string.report_analysis_loading_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AnalysisResultContent(
    result: AiAnalysisResult,
    categoryId: String,
    language: String,
    onOpenEmergency: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSafetyGuidance: () -> Unit,
    onOpenEducation: (topicId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val mod = modifier
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val confidencePct = (result.confidence.coerceIn(0.0, 1.0) * 100).toInt()
    val animatedConfidence by animateFloatAsState(
        targetValue = result.confidence.toFloat(),
        animationSpec = tween(durationMillis = 700),
        label = "confidence"
    )

    Column(
        modifier = mod.verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 3 }
        ) {
            LindaSectionCard(containerColor = MaterialTheme.colorScheme.surface) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(LindaPurpleDark, LindaPurple)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.report_analysis_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            ReportAnalysisUiMapper.sourceSubtitle(result.source),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                }

                AnalysisMetricRow(
                    label = stringResource(R.string.report_analysis_ai_source),
                    value = ReportAnalysisUiMapper.sourceLabel(result.source)
                )
                AnalysisMetricRow(
                    label = stringResource(R.string.report_analysis_incident),
                    value = ReportAnalysisUiMapper.categoryLabel(context, result.category, categoryId)
                )
                SeverityBadge(
                    label = stringResource(R.string.report_analysis_priority),
                    severity = result.severity
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.report_analysis_confidence),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.report_analysis_confidence_value, confidencePct),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { animatedConfidence },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = LindaLavenderSoft
                    )
                }
            }
        }

        if (result.indicators.isNotEmpty()) {
            LindaSectionCard {
                Text(
                    stringResource(R.string.report_analysis_indicators),
                    fontWeight = FontWeight.SemiBold
                )
                result.indicators.forEach { indicator ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(indicator, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        LindaSectionCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                stringResource(R.string.report_analysis_why_flagged),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                result.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        if (result.recommendations.isNotEmpty()) {
            LindaSectionCard {
                Text(
                    stringResource(R.string.report_analysis_next_steps),
                    fontWeight = FontWeight.SemiBold
                )
                result.recommendations.forEach { step ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {}
                        Text(step, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (result.educationRecommendations.isNotEmpty()) {
            LindaSectionCard {
                Text(
                    stringResource(R.string.report_analysis_education),
                    fontWeight = FontWeight.SemiBold
                )
                result.educationRecommendations.forEach { topicId ->
                    OutlinedButton(
                        onClick = { onOpenEducation(topicId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            ReportAnalysisUiMapper.educationTopicTitle(context, topicId, language)
                        )
                    }
                }
            }
        }

        Text(
            stringResource(R.string.report_analysis_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onOpenEmergency,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LindaDanger)
            ) {
                Text(stringResource(R.string.report_analysis_action_emergency))
            }
            OutlinedButton(onClick = onOpenContacts, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.report_analysis_action_contacts))
            }
            OutlinedButton(onClick = onOpenSafetyGuidance, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.report_analysis_action_safety))
            }
            OutlinedButton(
                onClick = { onOpenEducation(result.educationRecommendations.firstOrNull()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.report_analysis_action_education))
            }
        }
    }
}

@Composable
private fun AnalysisMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SeverityBadge(label: String, severity: AiSeverity) {
    val (bg, fg, text) = when (severity) {
        AiSeverity.CRITICAL -> Triple(LindaDanger.copy(alpha = 0.12f), LindaDanger, "CRITICAL")
        AiSeverity.HIGH -> Triple(LindaWarning.copy(alpha = 0.15f), LindaWarning, "HIGH")
        AiSeverity.ELEVATED -> Triple(LindaPurple.copy(alpha = 0.12f), LindaPurple, "ELEVATED")
        AiSeverity.LOWER -> Triple(LindaSuccess.copy(alpha = 0.12f), LindaSuccess, "LOWER")
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(color = bg, shape = RoundedCornerShape(20.dp)) {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = fg,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun ReportSavedAiOfferCard(
    onAnalyze: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 2 }
    ) {
        LindaSectionCard(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                stringResource(R.string.report_analysis_offer_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                stringResource(R.string.report_analysis_offer_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Button(onClick = onAnalyze, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.report_analysis_offer_action))
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.report_analysis_offer_dismiss))
            }
        }
    }
}
