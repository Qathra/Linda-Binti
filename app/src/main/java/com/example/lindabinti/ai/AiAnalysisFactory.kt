package com.example.lindabinti.ai

import android.content.Context
import com.example.lindabinti.data.offline.NetworkMonitor
import com.example.lindabinti.data.prefs.SecurePrefs
import com.example.lindabinti.data.remote.BackendClientFactory
import com.example.lindabinti.data.remote.BackendSession

@Deprecated(
    message = "Use AiOrchestrationFactory.createOrchestrator",
    replaceWith = ReplaceWith("AiOrchestrationFactory.createOrchestrator(context, prefs, session)")
)
object AiAnalysisFactory {

    fun createHybridRouter(
        context: Context,
        prefs: SecurePrefs,
        session: BackendSession
    ): HybridAiRouter {
        val api = BackendClientFactory.create()
        val onlineClient: OnlineAiClient = if (api != null) {
            HttpOnlineAiClient(api, session) { prefs.cloudAiConsent }
        } else {
            UnavailableOnlineAiClient()
        }
        val network = NetworkMonitor(context)
        return HybridAiRouter(
            offline = OfflineAiAnalysisEngine(),
            online = OnlineAiAnalysisEngine(onlineClient),
            policy = AiRoutingPolicy(
                isBackendConfigured = { BackendClientFactory.isConfigured() },
                isNetworkAvailable = { network.isInternetLikely() },
                hasCloudAiConsent = { prefs.cloudAiConsent },
                isUserSessionActive = { session.isLoggedIn() && session.isUserRole() },
                isOnlineAiConfigured = { onlineClient.isOnlineAiConfigured() }
            )
        )
    }
}
