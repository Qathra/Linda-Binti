package com.example.lindabinti.ui.education

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.R
import com.example.lindabinti.data.remote.EducationContentDto
import com.example.lindabinti.ui.common.LindaSectionCard
import kotlinx.coroutines.launch

private val VIDEO_CATEGORIES = listOf(
    "" to R.string.education_video_cat_all,
    "what_is_gbv" to R.string.education_video_cat_gbv,
    "what_is_fgm" to R.string.education_video_cat_fgm,
    "how_to_get_help" to R.string.education_video_cat_help,
    "safety_planning" to R.string.education_video_cat_safety
)

private fun isDirectVideoUrl(url: String): Boolean {
    val u = url.lowercase()
    return u.endsWith(".mp4") || u.endsWith(".m3u8") || u.contains(".mp4?")
}

private fun isExternalWatchUrl(url: String): Boolean {
    val u = url.lowercase()
    return u.contains("youtube.com") || u.contains("youtu.be") || u.contains("vimeo.com")
}

@Composable
fun EducationVideoLibrarySection(modifier: Modifier = Modifier) {
    val app = LindaBintiApp.instance
    val context = LocalContext.current
    val online by app.networkMonitor.isOnline.collectAsStateWithLifecycle()
    val catalog by app.educationContentRepository.items.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var category by remember { mutableStateOf("") }
    var playing by remember { mutableStateOf<EducationContentDto?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var downloadTick by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        if (online) {
            runCatching { app.educationContentRepository.refreshFromBackend() }
        }
    }

    val videos = remember(catalog, category, downloadTick) {
        app.educationContentRepository.videos(category = category.ifBlank { null })
    }
    val textTips = remember(catalog) { app.educationContentRepository.textItems() }

    fun playOrOpen(item: EducationContentDto) {
        val url = item.urlOrPath.trim()
        when {
            isExternalWatchUrl(url) -> {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }.onFailure {
                    Toast.makeText(context, R.string.education_video_open_fail, Toast.LENGTH_SHORT).show()
                }
            }
            else -> playing = item
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.education_videos_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(VIDEO_CATEGORIES) { (id, labelRes) ->
                FilterChip(
                    selected = category == id,
                    onClick = { category = id },
                    label = { Text(stringResource(labelRes)) }
                )
            }
        }

        if (videos.isEmpty() && textTips.isEmpty()) {
            LindaSectionCard {
                Text(
                    if (online) {
                        stringResource(R.string.education_videos_empty_online)
                    } else {
                        stringResource(R.string.education_videos_empty_offline)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        videos.forEach { item ->
            val downloaded = remember(item.id, downloadTick) {
                app.educationContentRepository.isDownloaded(item.id)
            }
            val external = isExternalWatchUrl(item.urlOrPath)
            val direct = isDirectVideoUrl(item.urlOrPath)
            LindaSectionCard {
                Text(item.title, fontWeight = FontWeight.SemiBold)
                Text(item.description, style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(
                        R.string.education_video_meta,
                        item.language.uppercase(),
                        item.category,
                        item.durationSeconds
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    when {
                        external -> stringResource(R.string.education_video_status_community)
                        downloaded -> stringResource(R.string.education_video_status_downloaded)
                        online -> stringResource(R.string.education_video_status_online)
                        else -> stringResource(R.string.education_video_status_unavailable)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (downloaded || online || external) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { playOrOpen(item) },
                        enabled = downloaded || (online && item.urlOrPath.isNotBlank()) ||
                            (external && item.urlOrPath.isNotBlank())
                    ) {
                        Text(
                            if (external) stringResource(R.string.education_open_video)
                            else stringResource(R.string.education_play_video)
                        )
                    }
                    if (!external && direct && !downloaded) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    status = null
                                    val ok = app.educationContentRepository.downloadForOffline(item)
                                    downloadTick++
                                    status = if (ok) {
                                        app.getString(R.string.education_video_download_ok)
                                    } else {
                                        app.getString(R.string.education_video_download_fail)
                                    }
                                }
                            },
                            enabled = online && item.urlOrPath.isNotBlank()
                        ) {
                            Text(stringResource(R.string.education_video_download))
                        }
                    } else if (!external && downloaded) {
                        OutlinedButton(
                            onClick = {
                                app.educationContentRepository.deleteDownload(item.id)
                                downloadTick++
                                if (playing?.id == item.id) playing = null
                            }
                        ) {
                            Text(stringResource(R.string.education_video_remove_download))
                        }
                    }
                }
            }
        }

        textTips.forEach { tip ->
            LindaSectionCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Text(tip.title, fontWeight = FontWeight.SemiBold)
                Text(tip.description, style = MaterialTheme.typography.bodyMedium)
            }
        }

        status?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }

        playing?.let { item ->
            StreamOrLocalPlayer(
                item = item,
                localPath = app.educationContentRepository.localPlaybackUri(item.id),
                onClose = { playing = null }
            )
        }
    }
}

@Composable
private fun StreamOrLocalPlayer(
    item: EducationContentDto,
    localPath: String?,
    onClose: () -> Unit
) {
    var player by remember { mutableStateOf<VideoView?>(null) }
    DisposableEffect(item.id, localPath) {
        onDispose {
            player?.releaseSafely()
            player = null
        }
    }
    LindaSectionCard {
        Text(item.title, fontWeight = FontWeight.Bold)
        key(item.id, localPath) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val uri = if (!localPath.isNullOrBlank()) {
                            Uri.fromFile(java.io.File(localPath))
                        } else {
                            Uri.parse(item.urlOrPath)
                        }
                        installSafePlayback(
                            uri = uri,
                            autoStart = true,
                            onFailed = onClose
                        )
                        player = this
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.education_video_close_player))
        }
    }
}
