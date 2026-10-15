package com.example.lindabinti.ui.risk

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.R
import com.example.lindabinti.data.content.LocalContent
import com.example.lindabinti.domain.RiskScorer
import com.example.lindabinti.ui.common.LindaSectionCard
import com.example.lindabinti.ui.common.ProtectedTopBar
import com.example.lindabinti.ui.locale.rememberAppLanguage
import com.example.lindabinti.ui.theme.LindaDanger
import com.example.lindabinti.ui.theme.LindaWarning
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RiskAssessmentScreen(
    onBack: () -> Unit,
    onQuickHide: () -> Unit,
    onOpenEmergency: () -> Unit,
    onOpenSupport: () -> Unit = {},
    onOpenEducation: () -> Unit = {},
    onOpenContacts: () -> Unit = {}
) {
    val mod = androidx.compose.ui.Modifier
    val app = LindaBintiApp.instance
    val recent by app.riskRepository.observeRecent()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val answers = remember { mutableStateMapOf<String, Boolean>() }
    var resultBand by remember { mutableStateOf<String?>(null) }
    var resultScore by remember { mutableStateOf<Int?>(null) }
    var answerHint by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lang = rememberAppLanguage()
    val questions = remember(lang) { LocalContent.riskQuestions(lang) }
    val allAnswered = questions.all { answers.containsKey(it.id) }

    LindaProtectedScaffold(
        topBar = {
            ProtectedTopBar(
                title = stringResource(R.string.risk_title),
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
            questions.forEach { q ->
                LindaSectionCard {
                    Text(q.text, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = answers[q.id] == true,
                            onClick = {
                                answers[q.id] = true
                                answerHint = false
                            },
                            label = { Text(stringResource(R.string.action_yes)) }
                        )
                        FilterChip(
                            selected = answers[q.id] == false,
                            onClick = {
                                answers[q.id] = false
                                answerHint = false
                            },
                            label = { Text(stringResource(R.string.action_no)) }
                        )
                    }
                }
            }

            if (answerHint) {
                Text(
                    stringResource(R.string.risk_answer_all),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = {
                    if (!allAnswered) {
                        answerHint = true
                        return@Button
                    }
                    val result = RiskScorer.evaluate(answers.toMap(), questions) ?: return@Button
                    resultScore = result.score
                    resultBand = result.band
                    val map = linkedMapOf<String, Boolean>()
                    questions.forEach { q -> map[q.id] = answers[q.id] == true }
                    scope.launch {
                        app.riskRepository.save(
                            answersJson = map.entries.joinToString(";") { "${it.key}=${it.value}" },
                            score = result.score,
                            band = result.band
                        )
                    }
                },
                modifier = mod.fillMaxWidth()
            ) { Text(stringResource(R.string.risk_see_result)) }

            resultBand?.let { band ->
                val bandLabel = when (band) {
                    "critical" -> stringResource(R.string.risk_band_critical)
                    "high" -> stringResource(R.string.risk_band_high)
                    "elevated" -> stringResource(R.string.risk_band_elevated)
                    else -> stringResource(R.string.risk_band_lower)
                }
                val resultColor = when (band) {
                    "critical" -> LindaDanger
                    "high" -> LindaWarning
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
                LindaSectionCard(containerColor = resultColor) {
                    val onResult = if (band == "critical" || band == "high") Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    Text(
                        stringResource(R.string.risk_result_title, bandLabel),
                        fontWeight = FontWeight.SemiBold,
                        color = onResult
                    )
                    Text(stringResource(R.string.risk_score, resultScore ?: 0), color = onResult)
                    Text(
                        when (band) {
                            "critical" -> stringResource(R.string.risk_advice_critical)
                            "high" -> stringResource(R.string.risk_advice_high)
                            "elevated" -> stringResource(R.string.risk_advice_elevated)
                            else -> stringResource(R.string.risk_advice_lower)
                        },
                        color = onResult
                    )
                    Text(stringResource(R.string.risk_recommendations_title), fontWeight = FontWeight.SemiBold, color = onResult)
                    when (band) {
                        "critical", "high" -> {
                            Button(onClick = onOpenEmergency, modifier = mod.fillMaxWidth()) { Text(stringResource(R.string.risk_open_emergency)) }
                            OutlinedButton(onClick = onOpenSupport, modifier = mod.fillMaxWidth()) { Text(stringResource(R.string.risk_open_support)) }
                            OutlinedButton(onClick = onOpenContacts, modifier = mod.fillMaxWidth()) { Text(stringResource(R.string.risk_open_contacts)) }
                        }
                        "elevated" -> {
                            OutlinedButton(onClick = onOpenContacts, modifier = mod.fillMaxWidth()) { Text(stringResource(R.string.risk_open_contacts)) }
                            OutlinedButton(onClick = onOpenSupport, modifier = mod.fillMaxWidth()) { Text(stringResource(R.string.risk_open_support)) }
                            OutlinedButton(onClick = onOpenEducation, modifier = mod.fillMaxWidth()) { Text(stringResource(R.string.risk_open_education)) }
                            OutlinedButton(onClick = onOpenEmergency, modifier = mod.fillMaxWidth()) { Text(stringResource(R.string.risk_open_emergency)) }
                        }
                        else -> {
                            OutlinedButton(onClick = onOpenEducation, modifier = mod.fillMaxWidth()) { Text(stringResource(R.string.risk_open_education)) }
                            OutlinedButton(onClick = onOpenSupport, modifier = mod.fillMaxWidth()) { Text(stringResource(R.string.risk_open_support)) }
                        }
                    }
                }
            }

            if (recent.isNotEmpty()) {
                Text(stringResource(R.string.risk_history_title), fontWeight = FontWeight.SemiBold)
                recent.forEach { item ->
                    val histLabel = when (item.band) {
                        "critical" -> stringResource(R.string.risk_band_critical)
                        "high" -> stringResource(R.string.risk_band_high)
                        "elevated" -> stringResource(R.string.risk_band_elevated)
                        else -> stringResource(R.string.risk_band_lower)
                    }
                    LindaSectionCard {
                        Text(
                            stringResource(
                                R.string.risk_history_item,
                                histLabel,
                                item.score,
                                SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ENGLISH).format(Date(item.createdAt))
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
