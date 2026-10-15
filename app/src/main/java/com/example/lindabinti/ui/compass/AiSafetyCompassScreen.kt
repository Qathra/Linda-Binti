package com.example.lindabinti.ui.compass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.R
import com.example.lindabinti.compass.SafetyCompassAnalyzer
import com.example.lindabinti.compass.SafetyCompassSnapshot
import com.example.lindabinti.compass.SafetyCompassStatus
import com.example.lindabinti.data.content.LocalContent
import com.example.lindabinti.ui.common.LindaSectionCard
import com.example.lindabinti.ui.common.ProtectedTopBar
import com.example.lindabinti.ui.locale.rememberAppLanguage
import com.example.lindabinti.ui.theme.LindaDanger
import com.example.lindabinti.ui.theme.LindaLavender
import com.example.lindabinti.ui.theme.LindaLavenderSoft
import com.example.lindabinti.ui.theme.LindaMuted
import com.example.lindabinti.ui.theme.LindaPurple
import com.example.lindabinti.ui.theme.LindaPurpleDark
import com.example.lindabinti.ui.theme.LindaPurpleMid
import com.example.lindabinti.ui.theme.LindaSuccess
import com.example.lindabinti.ui.theme.LindaWarning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AiSafetyCompassScreen(
    onBack: () -> Unit,
    onQuickHide: () -> Unit,
    onOpenEmergency: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenEducation: (topicId: String?) -> Unit,
    onOpenRiskAssessment: () -> Unit,
    onOpenAssistant: () -> Unit
) {
    val mod = Modifier
    val app = LindaBintiApp.instance
    val lang = rememberAppLanguage()

    val latestRisk by app.riskRepository.observeLatest().collectAsStateWithLifecycle(initialValue = null)
    val reports by app.reportRepository.observeReports().collectAsStateWithLifecycle(initialValue = emptyList())
    val education by app.educationRepository.observeProgress().collectAsStateWithLifecycle(initialValue = emptyList())
    val contacts by app.contactRepository.observeContacts().collectAsStateWithLifecycle(initialValue = emptyList())

    var snapshot by remember { mutableStateOf<SafetyCompassSnapshot?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(latestRisk, reports, education, contacts, lang) {
        loading = true
        snapshot = withContext(Dispatchers.Default) {
            SafetyCompassAnalyzer.buildSnapshot(
                orchestrator = app.aiOrchestrator,
                language = lang,
                latestRisk = latestRisk,
                riskQuestions = LocalContent.riskQuestions(lang),
                reports = reports,
                completedEducationIds = education.map { it.moduleId }.toSet(),
                trustedContactCount = contacts.size
            )
        }
        loading = false
    }

    LindaProtectedScaffold(
        topBar = {
            ProtectedTopBar(
                title = stringResource(R.string.compass_title),
                onBack = onBack,
                onQuickHide = onQuickHide
            )
        }
    ) { padding ->
        when {
            loading -> Box(
                modifier = mod.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = LindaPurple)
            }
            snapshot == null -> Box(
                modifier = mod.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.compass_unavailable))
            }
            else -> CompassContent(
                modifier = mod.padding(padding),
                snapshot = snapshot!!,
                language = lang,
                onOpenEmergency = onOpenEmergency,
                onOpenContacts = onOpenContacts,
                onOpenSupport = onOpenSupport,
                onOpenEducation = onOpenEducation,
                onOpenRiskAssessment = onOpenRiskAssessment,
                onOpenAssistant = onOpenAssistant
            )
        }
    }
}

@Composable
private fun CompassContent(
    modifier: Modifier,
    snapshot: SafetyCompassSnapshot,
    language: String,
    onOpenEmergency: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenEducation: (topicId: String?) -> Unit,
    onOpenRiskAssessment: () -> Unit,
    onOpenAssistant: () -> Unit
) {
    val scroll = rememberScrollState()
    val needleRotation by animateFloatAsState(
        targetValue = statusNeedleAngle(snapshot.status),
        animationSpec = tween(800),
        label = "compassNeedle"
    )
    val statusLabel = statusDisplay(snapshot.status)
    val statusColor = statusColor(snapshot.status)
    val compassA11y = stringResource(R.string.compass_a11y_status, statusLabel)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = LindaLavender
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.compass_reassurance_alone),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LindaPurpleDark,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.compass_reassurance_matters),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LindaMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                CompassDial(
                    needleRotation = needleRotation,
                    statusColor = statusColor,
                    contentDescription = compassA11y
                )
                Text(
                    text = stringResource(R.string.compass_safety_status),
                    style = MaterialTheme.typography.labelLarge,
                    color = LindaMuted,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Text(
                    text = snapshot.statusConfidenceNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = LindaMuted,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.compass_source_label, snapshot.aiSourceLabel),
                    style = MaterialTheme.typography.labelSmall,
                    color = LindaPurpleMid
                )
            }
        }

        Text(
            text = stringResource(R.string.compass_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = LindaMuted
        )

        if (!snapshot.hasAnyData) {
            LindaSectionCard {
                SectionTitle(stringResource(R.string.compass_get_started))
                Text(stringResource(R.string.compass_get_started_body))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenRiskAssessment, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.compass_open_risk))
                }
            }
        }

        LindaSectionCard(modifier = Modifier.semantics { heading() }) {
            SectionTitle(stringResource(R.string.compass_noticed_title))
            BulletSection(stringResource(R.string.compass_indicators_heading), snapshot.riskIndicators)
            BulletSection(stringResource(R.string.compass_concerns_heading), snapshot.gbvFgmConcerns)
            BulletSection(stringResource(R.string.compass_factors_heading), snapshot.safetyFactors)
        }

        LindaSectionCard(modifier = Modifier.semantics { heading() }) {
            SectionTitle(stringResource(R.string.compass_next_title))
            snapshot.nextSteps.forEach { step ->
                CompassBullet(step)
            }
        }

        LindaSectionCard(modifier = Modifier.semantics { heading() }) {
            SectionTitle(stringResource(R.string.compass_learn_title))
            if (snapshot.educationItems.isEmpty()) {
                Text(stringResource(R.string.compass_learn_empty))
            } else {
                snapshot.educationItems.forEach { item ->
                    OutlinedButton(
                        onClick = { onOpenEducation(item.topicId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(item.title)
                    }
                }
            }
            OutlinedButton(
                onClick = { onOpenEducation(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.compass_all_education))
            }
        }

        LindaSectionCard(modifier = Modifier.semantics { heading() }) {
            SectionTitle(stringResource(R.string.compass_support_title))
            if (snapshot.showTrustedContacts) {
                Text(
                    stringResource(R.string.compass_trusted_count, snapshot.trustedContactCount),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = onOpenContacts,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.compass_open_contacts))
                }
            } else {
                Text(stringResource(R.string.compass_no_contacts_hint))
                OutlinedButton(
                    onClick = onOpenContacts,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.compass_add_contacts))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onOpenEmergency,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LindaPurple)
            ) {
                Text(stringResource(R.string.compass_open_emergency))
            }
            OutlinedButton(
                onClick = onOpenSupport,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.compass_open_support))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.compass_hotlines_heading),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
            LocalContent.hotlines(language).forEach { hotline ->
                Text(
                    "${hotline.name}: ${hotline.number}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LindaMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        LindaSectionCard {
            SectionTitle(stringResource(R.string.compass_chat_title))
            Text(
                stringResource(R.string.compass_chat_body),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onOpenAssistant,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LindaPurpleDark)
            ) {
                Text(stringResource(R.string.compass_open_assistant))
            }
        }
    }
}

@Composable
private fun CompassDial(
    needleRotation: Float,
    statusColor: Color,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(160.dp)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f * 0.92f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LindaLavenderSoft, Color.White),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                color = LindaPurple.copy(alpha = 0.25f),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = LindaPurpleMid.copy(alpha = 0.15f),
                radius = radius * 0.55f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            val needleLength = radius * 0.72f
            val angleRad = Math.toRadians(needleRotation.toDouble())
            val tip = Offset(
                center.x + (needleLength * kotlin.math.sin(angleRad)).toFloat(),
                center.y - (needleLength * kotlin.math.cos(angleRad)).toFloat()
            )
            drawLine(
                color = statusColor,
                start = center,
                end = tip,
                strokeWidth = 4.dp.toPx()
            )
            drawCircle(color = LindaPurpleDark, radius = 6.dp.toPx(), center = center)
        }
        Text(
            text = "N",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = LindaPurpleMid,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = LindaPurpleDark,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun BulletSection(heading: String, items: List<String>) {
    if (items.isEmpty()) return
    Text(heading, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
    items.forEach { CompassBullet(it) }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun CompassBullet(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(LindaPurpleMid)
        )
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun statusDisplay(status: SafetyCompassStatus): String = when (status) {
    SafetyCompassStatus.LOWER -> stringResource(R.string.compass_status_lower)
    SafetyCompassStatus.ELEVATED -> stringResource(R.string.compass_status_elevated)
    SafetyCompassStatus.HIGH -> stringResource(R.string.compass_status_high)
    SafetyCompassStatus.CRITICAL -> stringResource(R.string.compass_status_critical)
}

private fun statusColor(status: SafetyCompassStatus): Color = when (status) {
    SafetyCompassStatus.CRITICAL -> LindaDanger
    SafetyCompassStatus.HIGH -> LindaDanger.copy(alpha = 0.85f)
    SafetyCompassStatus.ELEVATED -> LindaWarning
    SafetyCompassStatus.LOWER -> LindaSuccess
}

private fun statusNeedleAngle(status: SafetyCompassStatus): Float = when (status) {
    SafetyCompassStatus.LOWER -> 0f
    SafetyCompassStatus.ELEVATED -> 35f
    SafetyCompassStatus.HIGH -> 70f
    SafetyCompassStatus.CRITICAL -> 95f
}
