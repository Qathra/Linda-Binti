package com.example.lindabinti.ui.common

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.lindabinti.util.findActivity

/**
 * NavHost 2.8 AnimatedContent can compose a destination without
 * [LocalActivityResultRegistryOwner], which crashes permission / contact pickers.
 * Re-attach the Activity as the owner for those screens.
 */
@Composable
fun WithActivityResultOwner(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    if (activity == null) {
        content()
        return
    }
    CompositionLocalProvider(
        LocalActivityResultRegistryOwner provides activity,
        content = content
    )
}
