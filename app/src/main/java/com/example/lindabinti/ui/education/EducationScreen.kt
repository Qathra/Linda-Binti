package com.example.lindabinti.ui.education

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.example.lindabinti.ui.common.LindaProtectedScaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.R
import com.example.lindabinti.data.content.EducationCatalog
import com.example.lindabinti.data.content.EducationMediaLookup
import com.example.lindabinti.data.content.EducationQuizOption
import com.example.lindabinti.data.content.EducationSlideView
import com.example.lindabinti.data.content.EducationTopicView
import com.example.lindabinti.data.content.FgmAwarenessVideos
import com.example.lindabinti.data.content.GbvAwarenessVideos
import com.example.lindabinti.ui.common.LindaSectionCard
import com.example.lindabinti.ui.common.ProtectedTopBar
import com.example.lindabinti.ui.locale.rememberAppLanguage
import com.example.lindabinti.ui.theme.LindaLavenderSoft
import com.example.lindabinti.ui.theme.LindaSuccess
import kotlinx.coroutines.launch

@Composable
fun EducationScreen(
    onBack: () -> Unit,
    onQuickHide: () -> Unit,
    onOpenEmergency: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenReporting: () -> Unit,
    initialTopicId: String? = null
) {
    val mod = androidx.compose.ui.Modifier
    val app = LindaBintiApp.instance
    val progress by app.educationRepository.observeProgress()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val completed = progress.map { it.moduleId }.toSet()
    val scope = rememberCoroutineScope()
    val lang = rememberAppLanguage()
    val topics = remember(lang) { EducationCatalog.topics(lang) }
    var dest by remember { mutableStateOf<EducationDest>(EducationDest.Hub) }

    LaunchedEffect(initialTopicId) {
        if (!initialTopicId.isNullOrBlank()) {
            dest = EducationDest.Topic(initialTopicId)
        }
    }

    val title = when (val d = dest) {
        EducationDest.Hub -> stringResource(R.string.education_title)
        is EducationDest.Topic -> topics.find { it.id == d.id }?.title
            ?: stringResource(R.string.education_title)
    }

    LindaProtectedScaffold(
        topBar = {
            ProtectedTopBar(
                title = title,
                onBack = {
                    val next = EducationNavigation.back(dest)
                    if (next == null) onBack() else dest = next
                },
                onQuickHide = onQuickHide
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = dest,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "eduDest",
            modifier = mod.fillMaxSize().padding(padding)
        ) { state ->
            when (state) {
                EducationDest.Hub -> EducationHub(
                    topics = topics,
                    completed = completed,
                    onOpenTopic = { dest = EducationNavigation.openTopic(dest, it.id) }
                )
                is EducationDest.Topic -> {
                    val topic = topics.find { it.id == state.id }
                    if (topic == null) {
                        EducationHub(
                            topics = topics,
                            completed = completed,
                            onOpenTopic = { dest = EducationNavigation.openTopic(dest, it.id) }
                        )
                    } else {
                        EducationTopicPage(
                            topic = topic,
                            completed = topic.id in completed,
                            onMarkDone = {
                                scope.launch { app.educationRepository.markComplete(topic.id) }
                            },
                            onOpenEmergency = onOpenEmergency,
                            onOpenSupport = onOpenSupport,
                            onOpenReporting = onOpenReporting
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EducationHub(
    topics: List<EducationTopicView>,
    completed: Set<String>,
    onOpenTopic: (EducationTopicView) -> Unit
) {
    val mod = androidx.compose.ui.Modifier

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = mod.fillMaxSize()
    ) {
        items(topics, key = { it.id }) { topic ->
            TopicTile(
                topic = topic,
                done = topic.id in completed,
                onClick = { onOpenTopic(topic) }
            )
        }
    }
}

@Composable
private fun TopicTile(
    topic: EducationTopicView,
    done: Boolean,
    onClick: () -> Unit
) {
    val mod = androidx.compose.ui.Modifier
    val context = LocalContext.current
    val iconId = drawableId(context, topic.iconResName, R.drawable.ic_edu_gbv)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = if (done) LindaLavenderSoft else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = mod.fillMaxWidth()
    ) {
        Column(
            modifier = mod.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = mod
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconId),
                    contentDescription = topic.title,
                    modifier = mod.size(34.dp)
                )
            }
            Text(topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(topic.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (done) {
                Text(
                    stringResource(R.string.education_completed),
                    color = LindaSuccess,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun EducationTopicPage(
    topic: EducationTopicView,
    completed: Boolean,
    onMarkDone: () -> Unit,
    onOpenEmergency: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenReporting: () -> Unit
) {
    val mod = androidx.compose.ui.Modifier
    val context = LocalContext.current
    val app = LindaBintiApp.instance
    val iconId = drawableId(context, topic.iconResName, R.drawable.ic_edu_gbv)
    val artId = drawableId(context, topic.illustrationResName, iconId)
    var showMore by remember(topic.id) { mutableStateOf(false) }
    var picked by remember(topic.id) { mutableStateOf<String?>(null) }

    Column(
        modifier = mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(artId),
            contentDescription = stringResource(R.string.education_art_placeholder),
            modifier = mod.fillMaxWidth().height(88.dp),
            contentScale = ContentScale.Fit
        )
        Text(topic.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(topic.summary, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

        AudioReadyRow(audioKey = topic.audioKey)

        FactCard(stringResource(R.string.education_section_means), R.drawable.ic_edu_gbv, topic.means)
        FactCard(stringResource(R.string.education_section_signs), R.drawable.ic_edu_warning, topic.warningSigns)
        FactCard(stringResource(R.string.education_section_do), R.drawable.ic_edu_emergency, topic.whatToDo)
        FactCard(stringResource(R.string.education_section_help), R.drawable.ic_edu_survivor, topic.whereHelp)

        OfflineClip(topic = topic)

        OutlinedButton(onClick = { showMore = !showMore }, modifier = mod.fillMaxWidth()) {
            Text(
                if (showMore) stringResource(R.string.education_learn_less)
                else stringResource(R.string.education_learn_more)
            )
        }
        AnimatedVisibility(visible = showMore) {
            LindaSectionCard {
                topic.learnMore.forEach { line ->
                    Text("• $line", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        if (topic.id == "fgm") {
            val online by app.networkMonitor.isOnline.collectAsStateWithLifecycle()
            AwarenessVideosSection(
                title = stringResource(R.string.education_fgm_videos_title),
                videos = FgmAwarenessVideos.all,
                online = online
            )
        }

        if (topic.id == "gbv") {
            val online by app.networkMonitor.isOnline.collectAsStateWithLifecycle()
            AwarenessVideosSection(
                title = stringResource(R.string.education_gbv_videos_title),
                videos = GbvAwarenessVideos.all,
                online = online
            )
        }

        Text(stringResource(R.string.education_quiz_title), fontWeight = FontWeight.SemiBold)
        LindaSectionCard {
            Text(topic.quiz.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            topic.quiz.options.forEach { option ->
                QuizChoice(
                    option = option,
                    selectedId = picked,
                    onPick = { picked = option.id }
                )
            }
            val selected = topic.quiz.options.find { it.id == picked }
            if (selected != null) {
                Text(
                    if (selected.correct) {
                        stringResource(R.string.education_quiz_correct)
                    } else {
                        stringResource(R.string.education_quiz_again)
                    },
                    color = if (selected.correct) LindaSuccess else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
                if (selected.correct) {
                    Text(topic.quiz.explain, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Text(stringResource(R.string.education_open_help), fontWeight = FontWeight.SemiBold)
        Button(onClick = onOpenEmergency, modifier = mod.fillMaxWidth()) {
            Text(stringResource(R.string.education_open_emergency))
        }
        OutlinedButton(onClick = onOpenSupport, modifier = mod.fillMaxWidth()) {
            Text(stringResource(R.string.education_open_support))
        }
        OutlinedButton(onClick = onOpenReporting, modifier = mod.fillMaxWidth()) {
            Text(stringResource(R.string.education_open_reporting))
        }

        Text(
            stringResource(R.string.education_disclaimer_short),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onMarkDone, modifier = mod.fillMaxWidth()) {
            Text(
                if (completed) stringResource(R.string.education_done)
                else stringResource(R.string.education_mark_read)
            )
        }
    }
}

@Composable
private fun FactCard(title: String, iconRes: Int, lines: List<String>) {
    val mod = androidx.compose.ui.Modifier
    LindaSectionCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(painter = painterResource(iconRes), contentDescription = title, modifier = mod.size(28.dp))
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        }
        lines.forEach { line ->
            Text("• $line", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun QuizChoice(
    option: EducationQuizOption,
    selectedId: String?,
    onPick: () -> Unit
) {
    FilterChip(
        selected = selectedId == option.id,
        onClick = onPick,
        label = { Text(option.text, style = MaterialTheme.typography.bodyLarge) },
        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
    )
}

@Composable
private fun AudioReadyRow(audioKey: String) {
    val context = LocalContext.current
    val hasAudio = remember(audioKey) { EducationMediaLookup.hasRaw(context, audioKey) }
    if (!hasAudio) return
    LindaSectionCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
        Text(stringResource(R.string.education_listen_title), fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.education_audio_ready), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun OfflineClip(topic: EducationTopicView) {
    val context = LocalContext.current
    val videoName = topic.videoRawName ?: EducationMediaLookup.videoRawName(topic.id)
    val videoId = remember(videoName) { EducationMediaLookup.rawId(context, videoName) }
    if (videoId != 0) {
        BundledVideo(videoId = videoId)
    } else if (topic.slides.isNotEmpty()) {
        SlideClip(slides = topic.slides)
    }
}

@Composable
private fun BundledVideo(videoId: Int) {
    val mod = androidx.compose.ui.Modifier
    var player by remember { mutableStateOf<VideoView?>(null) }
    DisposableEffect(videoId) {
        onDispose {
            player?.releaseSafely()
            player = null
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.education_video_caption), fontWeight = FontWeight.SemiBold)
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    installSafePlayback(
                        uri = Uri.parse("android.resource://${ctx.packageName}/$videoId"),
                        autoStart = false
                    )
                    player = this
                }
            },
            modifier = mod.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))
        )
        OutlinedButton(
            onClick = { player?.start() },
            modifier = mod.fillMaxWidth()
        ) { Text(stringResource(R.string.education_play_video)) }
    }
}

@Composable
private fun SlideClip(slides: List<EducationSlideView>) {
    var playing by remember { mutableStateOf(false) }
    var index by remember { mutableStateOf(0) }
    val mod = androidx.compose.ui.Modifier

    LaunchedEffect(playing, index) {
        if (!playing) return@LaunchedEffect
        kotlinx.coroutines.delay(2800)
        if (index < slides.lastIndex) index += 1 else {
            playing = false
            index = 0
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.education_video_caption), fontWeight = FontWeight.SemiBold)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = mod.fillMaxWidth().height(180.dp)
        ) {
            Column(
                modifier = mod.padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    slides[index].title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(slides[index].body, style = MaterialTheme.typography.bodyLarge)
                Text("${index + 1} / ${slides.size}", style = MaterialTheme.typography.labelSmall)
            }
        }
        OutlinedButton(
            onClick = { playing = !playing },
            modifier = mod.fillMaxWidth()
        ) {
            Text(
                stringResource(
                    if (playing) R.string.education_pause_video else R.string.education_play_video
                )
            )
        }
    }
}

private fun drawableId(context: android.content.Context, name: String, fallback: Int): Int =
    context.resources.getIdentifier(name, "drawable", context.packageName).takeIf { it != 0 } ?: fallback
