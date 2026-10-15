package com.example.lindabinti.ui.locale

import android.content.res.Configuration
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.util.LocaleHelper
import com.example.lindabinti.util.findActivity

/** Selected app language. Catalog screens should remember content against this value. */
@Composable
fun rememberAppLanguage(): String {
    val lang by LindaBintiApp.instance.authRepository.language.collectAsStateWithLifecycle()
    return lang
}

/**
 * Makes [androidx.compose.ui.res.stringResource] follow the in-app language
 * even when the host Activity is not AppCompatActivity.
 */
@Composable
fun ProvideAppLanguage(content: @Composable () -> Unit) {
    val language = rememberAppLanguage()
    val base = LocalContext.current
    val localized = remember(language, base) { LocaleHelper.wrap(base, language) }
    val configuration = remember(localized) { Configuration(localized.resources.configuration) }
    val activity = remember(base) { base.findActivity() }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides configuration
    ) {
        if (activity != null) {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides activity,
                content = content
            )
        } else {
            content()
        }
    }
}
