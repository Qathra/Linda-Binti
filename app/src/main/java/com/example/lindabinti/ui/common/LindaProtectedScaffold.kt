package com.example.lindabinti.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalContentColor

/**
 * Protected-area scaffold with a rotating Linda Binti background and readable overlay.
 * Keeps foreground content unchanged while making screens feel polished.
 */
@Composable
fun LindaProtectedScaffold(
    modifier: Modifier = Modifier,
    useRotatingBackground: Boolean = true,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    content: @Composable (PaddingValues) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (useRotatingBackground) {
            LindaRotatingBackground()
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = Color.Transparent,
            contentColor = if (useRotatingBackground) {
                Color.White
            } else {
                contentColorFor(MaterialTheme.colorScheme.background)
            },
            content = { padding ->
                if (useRotatingBackground) {
                    CompositionLocalProvider(LocalContentColor provides Color.White) {
                        content(padding)
                    }
                } else {
                    content(padding)
                }
            }
        )
    }
}
