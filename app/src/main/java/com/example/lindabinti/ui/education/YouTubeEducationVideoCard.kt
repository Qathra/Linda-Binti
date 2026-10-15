package com.example.lindabinti.ui.education

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.lindabinti.R
import com.example.lindabinti.data.content.EducationYoutubeVideo
import com.example.lindabinti.ui.common.LindaSectionCard

@Composable
fun AwarenessVideosSection(
    title: String,
    videos: List<EducationYoutubeVideo>,
    online: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (!online) {
            Text(
                stringResource(R.string.education_awareness_videos_offline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        videos.forEach { video ->
            YouTubeEducationVideoCard(video = video, online = online)
        }
    }
}

@Composable
fun YouTubeEducationVideoCard(
    video: EducationYoutubeVideo,
    online: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var playing by remember(video.id) { mutableStateOf(false) }
    var embedFailed by remember(video.id) { mutableStateOf(false) }
    val playLabel = stringResource(R.string.education_play_video)
    val youtubeLabel = stringResource(R.string.education_watch_on_youtube)

    DisposableEffect(playing) {
        val window = context.findActivity()?.window
        if (playing && window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (playing) {
                window?.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            }
        }
    }

    fun openOnYoutube() {
        openYoutubeVideo(context, video.youtubeId)
    }

    LindaSectionCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .semantics { contentDescription = video.title }
            ) {
                when {
                    playing && online && !embedFailed -> {
                        YoutubeEmbedWebView(
                            youtubeId = video.youtubeId,
                            onError = {
                                embedFailed = true
                                playing = false
                                openOnYoutube()
                            }
                        )
                    }
                    else -> {
                        AsyncImage(
                            model = video.thumbnailUrl,
                            contentDescription = video.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (!online || embedFailed) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (embedFailed) {
                                        stringResource(R.string.education_fgm_video_embed_fail)
                                    } else {
                                        stringResource(R.string.education_awareness_video_needs_internet)
                                    },
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Text(video.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (video.description.isNotBlank()) {
                Text(
                    video.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (online) {
                            embedFailed = false
                            playing = true
                        } else {
                            openOnYoutube()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "$playLabel: ${video.title}" }
                ) {
                    Text(playLabel)
                }
                OutlinedButton(
                    onClick = { openOnYoutube() },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "$youtubeLabel: ${video.title}" }
                ) {
                    Text(youtubeLabel)
                }
            }

            if (embedFailed) {
                Text(
                    stringResource(R.string.education_fgm_video_embed_fail_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun openYoutubeVideo(context: Context, youtubeId: String) {
    val watchUri = Uri.parse("https://www.youtube.com/watch?v=$youtubeId")
    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$youtubeId")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val webIntent = Intent(Intent.ACTION_VIEW, watchUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(appIntent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(webIntent)
    } catch (_: Exception) {
        runCatching { context.startActivity(webIntent) }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YoutubeEmbedWebView(
    youtubeId: String,
    onError: () -> Unit
) {
    val embedUrl = remember(youtubeId) {
        "https://www.youtube.com/embed/$youtubeId?playsinline=1&rel=0&modestbranding=1"
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadsImagesAutomatically = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.userAgentString =
                    settings.userAgentString.replace("; wv", "") + " LindaBintiEducation"
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) onError()
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString().orEmpty()
                        if (url.contains("youtube.com/watch") || url.contains("youtu.be/")) {
                            openYoutubeVideo(ctx, youtubeId)
                            return true
                        }
                        return false
                    }
                }
                loadUrl(embedUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
