package com.example.lindabinti.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay

private const val SLIDE_VISIBLE_MS = 2_500L
private const val FADE_DURATION_MS = 900

@Composable
fun LindaRotatingBackground(
    modifier: Modifier = Modifier
) {
    val slides = remember { LindaBackgroundImages.slides }
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(slides) {
        if (slides.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(SLIDE_VISIBLE_MS)
            currentIndex = (currentIndex + 1) % slides.size
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (slides.isNotEmpty()) {
            Crossfade(
                targetState = currentIndex,
                animationSpec = tween(
                    durationMillis = FADE_DURATION_MS,
                    easing = FastOutSlowInEasing
                ),
                modifier = Modifier.fillMaxSize(),
                label = "lindaBackgroundCrossfade"
            ) { index ->
                Image(
                    painter = painterResource(slides[index]),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC121018),
                            Color(0xE6000000)
                        )
                    )
                )
        )
    }
}
