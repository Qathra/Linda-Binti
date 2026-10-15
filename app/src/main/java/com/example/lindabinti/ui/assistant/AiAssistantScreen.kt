package com.example.lindabinti.ui.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.example.lindabinti.ui.common.LindaProtectedScaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.R
import com.example.lindabinti.ai.AiOrchestrator
import com.example.lindabinti.ai.AssistantOnlineBlocker
import com.example.lindabinti.assistant.AssistantChatTurn
import com.example.lindabinti.assistant.AssistantMessage
import com.example.lindabinti.assistant.AssistantSource
import com.example.lindabinti.data.content.AssistantKnowledge
import com.example.lindabinti.ui.common.LindaSectionCard
import com.example.lindabinti.ui.common.ProtectedTopBar
import com.example.lindabinti.ui.common.lindaProtectedOverlayTextFieldColors
import com.example.lindabinti.ui.locale.rememberAppLanguage
import com.example.lindabinti.ui.theme.LindaPurple
import com.example.lindabinti.ui.theme.LindaPurpleDark
import kotlinx.coroutines.launch

private data class ChatLine(
    val fromUser: Boolean,
    val message: AssistantMessage
)

@Composable
fun AiAssistantScreen(
    onBack: () -> Unit,
    onQuickHide: () -> Unit,
    onOpenAction: (String) -> Unit
) {
    val mod = Modifier
    val app = LindaBintiApp.instance
    val lang = rememberAppLanguage()
    val scope = rememberCoroutineScope()
    val welcome = stringResource(R.string.ai_welcome)
    val lines = remember { mutableStateListOf<ChatLine>() }
    val prompts = remember(lang) { AssistantKnowledge.quickPrompts(lang) }
    LaunchedEffect(welcome) {
        if (lines.isEmpty()) {
            lines += ChatLine(false, AssistantMessage(welcome))
        } else if (lines.size == 1 && !lines[0].fromUser) {
            lines[0] = ChatLine(false, AssistantMessage(welcome))
        }
    }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var modeLabel by remember { mutableStateOf(AssistantSource.Local.modeLabel) }
    var setupBlocker by remember { mutableStateOf(AssistantOnlineBlocker.NONE) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        app.backendAccountRepository.prepareAssistantSession(app.securePrefs)
        modeLabel = app.aiOrchestrator.assistantStatusLabel()
        setupBlocker = app.aiOrchestrator.assistantSessionBlocker()
    }

    LaunchedEffect(lines.size, busy) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }

    fun buildHistory(): List<AssistantChatTurn> = lines.mapNotNull { line ->
        val role = if (line.fromUser) "user" else "assistant"
        val text = line.message.text.trim()
        if (text.isBlank()) null else AssistantChatTurn(role, text)
    }.dropLast(1)

    fun ask(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || busy) return
        lines += ChatLine(true, AssistantMessage(trimmed))
        input = ""
        busy = true
        scope.launch {
            try {
                val result = app.aiOrchestrator.assistantReply(trimmed, buildHistory())
                modeLabel = app.aiOrchestrator.assistantStatusLabel()
                setupBlocker = when {
                    result.blocker == AssistantOnlineBlocker.ONLINE_REQUEST_FAILED -> result.blocker
                    else -> app.aiOrchestrator.assistantSessionBlocker()
                }
                lines += ChatLine(false, result.message)
            } finally {
                busy = false
            }
        }
    }

    fun clearChat() {
        lines.clear()
        lines += ChatLine(false, AssistantMessage(welcome))
        AiOrchestrator.resetOnlineAssistantPause()
        setupBlocker = AssistantOnlineBlocker.NONE
        scope.launch {
            modeLabel = app.aiOrchestrator.assistantStatusLabel()
        }
    }

    LindaProtectedScaffold(
        useRotatingBackground = false,
        topBar = {
            ProtectedTopBar(
                title = stringResource(R.string.ai_title),
                onBack = onBack,
                onQuickHide = onQuickHide
            )
        }
    ) { padding ->
        Box(
            modifier = mod
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A1030), Color(0xFF0F172A), Color(0xFF1E1B4B))
                    )
                )
        ) {
        Column(
            modifier = mod
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = mod.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = mod.weight(1f)) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                if (modeLabel == AssistantSource.Remote.modeLabel) {
                                    stringResource(R.string.ai_mode_online)
                                } else {
                                    stringResource(R.string.ai_mode_offline)
                                },
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = if (modeLabel == AssistantSource.Remote.modeLabel) {
                                LindaPurple
                            } else {
                                Color(0xFF334155)
                            },
                            disabledLabelColor = Color.White
                        )
                    )
                    if (modeLabel == AssistantSource.Remote.modeLabel) {
                        Text(
                            stringResource(R.string.ai_online_session_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                            modifier = mod.padding(start = 4.dp, top = 2.dp)
                        )
                    } else {
                        Text(
                            stringResource(R.string.ai_offline_session_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                            modifier = mod.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }
                OutlinedButton(onClick = { clearChat() }) {
                    Text(stringResource(R.string.ai_clear), color = Color.White)
                }
            }

            if (setupBlocker != AssistantOnlineBlocker.NONE) {
                Text(
                    stringResource(assistantBlockerHintRes(setupBlocker)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFDE68A),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                stringResource(R.string.ai_disclaimer_auto),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )

            LazyColumn(
                state = listState,
                modifier = mod
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lines) { line ->
                    ChatBubble(line = line, onOpenAction = onOpenAction)
                }
                if (busy) {
                    item { AssistantTypingIndicator() }
                }
            }

            Column(
                modifier = mod
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Text(
                    stringResource(R.string.ai_suggested_prompts),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(prompts) { prompt ->
                        AssistChip(
                            onClick = { ask(prompt) },
                            label = {
                                Text(
                                    prompt,
                                    maxLines = 2,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            enabled = !busy,
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF4C1D95),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                Row(
                    modifier = mod.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.ai_ask_hint),
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        },
                        modifier = mod
                            .weight(1f)
                            .defaultMinSize(minHeight = 72.dp)
                            .heightIn(min = 72.dp, max = 140.dp),
                        minLines = 2,
                        maxLines = 5,
                        enabled = !busy,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        ),
                        colors = lindaProtectedOverlayTextFieldColors()
                    )
                    Button(
                        onClick = { ask(input) },
                        modifier = mod
                            .defaultMinSize(minHeight = 72.dp)
                            .heightIn(min = 72.dp),
                        enabled = input.isNotBlank() && !busy
                    ) {
                        Text(stringResource(R.string.ai_ask))
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ChatBubble(
    line: ChatLine,
    onOpenAction: (String) -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 4 }
    ) {
        if (line.fromUser) {
            LindaSectionCard(containerColor = Color(0xFF334155).copy(alpha = 0.95f)) {
                Text(
                    stringResource(R.string.ai_you),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                Text(
                    line.message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    lineHeight = 22.sp
                )
            }
        } else {
            LindaSectionCard(containerColor = Color(0xFF1E293B).copy(alpha = 0.96f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .background(Brush.horizontalGradient(listOf(LindaPurpleDark, LindaPurple)))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            stringResource(R.string.ai_bot),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            line.message.source.modeLabel,
                            color = if (line.message.source == AssistantSource.Remote) {
                                Color(0xFFBBF7D0)
                            } else {
                                Color.White.copy(alpha = 0.85f)
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Text(
                    line.message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    lineHeight = 22.sp
                )
                line.message.riskLevel?.takeIf { it != "NONE" }?.let { risk ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.ai_risk_notice, risk),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                line.message.relatedAction?.let { action ->
                    OutlinedButton(onClick = { onOpenAction(action) }) {
                        Text(
                            when (action) {
                                "emergency" -> stringResource(R.string.module_emergency)
                                "education" -> stringResource(R.string.module_education)
                                "support" -> stringResource(R.string.module_support)
                                "reporting" -> stringResource(R.string.module_reporting)
                                "contacts" -> stringResource(R.string.module_contacts)
                                "settings" -> stringResource(R.string.module_settings)
                                else -> stringResource(R.string.action_open)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantTypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "typingAlpha"
    )
    LindaSectionCard(containerColor = Color(0xFF312E81).copy(alpha = 0.92f)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White.copy(alpha = alpha)
            )
            Text(
                stringResource(R.string.ai_typing),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Surface(
                        modifier = Modifier.size(6.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    ) {}
                }
            }
        }
    }
}

private fun assistantBlockerHintRes(blocker: AssistantOnlineBlocker): Int = when (blocker) {
    AssistantOnlineBlocker.NOT_SIGNED_IN -> R.string.ai_blocker_sign_in
    AssistantOnlineBlocker.CLOUD_AI_OFF -> R.string.ai_blocker_cloud_ai
    AssistantOnlineBlocker.NO_NETWORK -> R.string.ai_blocker_network
    AssistantOnlineBlocker.SERVER_AI_UNAVAILABLE -> R.string.ai_blocker_server
    AssistantOnlineBlocker.BACKEND_NOT_CONFIGURED -> R.string.ai_blocker_backend
    AssistantOnlineBlocker.ONLINE_REQUEST_FAILED -> R.string.ai_blocker_online_failed
    AssistantOnlineBlocker.NONE -> R.string.ai_online_setup_hint
}
