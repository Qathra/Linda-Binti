package com.example.lindabinti

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.lindabinti.ui.navigation.LindaBintiNavHost
import com.example.lindabinti.util.LocaleHelper

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val tag = runCatching {
            LindaBintiApp.instance.authRepository.getLanguage()
        }.getOrDefault("en")
        super.attachBaseContext(LocaleHelper.wrap(newBase, tag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = application as LindaBintiApp
        LocaleHelper.applyLanguage(app.authRepository.getLanguage())
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep task description generic for recents
        setTaskDescription(
            android.app.ActivityManager.TaskDescription("Calculator")
        )
        setContent {
            LindaBintiNavHost()
        }
    }
}
