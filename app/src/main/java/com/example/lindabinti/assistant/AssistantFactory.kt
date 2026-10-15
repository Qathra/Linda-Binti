package com.example.lindabinti.assistant

import android.content.Context
import com.example.lindabinti.data.offline.NetworkMonitor
import com.example.lindabinti.data.prefs.SecurePrefs
import com.example.lindabinti.data.remote.BackendClientFactory
import com.example.lindabinti.data.remote.BackendSession

@Deprecated(
    message = "Assistant routing is internal to AiOrchestrator",
    replaceWith = ReplaceWith("AiOrchestrationFactory.createOrchestrator(context, prefs, session)")
)
object AssistantFactory {

    fun createHybridEngine(
        context: Context,
        prefs: SecurePrefs,
        session: BackendSession
    ): HybridAssistantEngine {
        val api = BackendClientFactory.create()
        val remote: AssistantRemoteClient = if (api != null) {
            HttpAssistantRemoteClient(api, session) { prefs.cloudAiConsent }
        } else {
            UnavailableAssistantRemoteClient()
        }
        val httpRemote = remote as? HttpAssistantRemoteClient
        val network = NetworkMonitor(context)
        return HybridAssistantEngine(
            local = LocalAssistantEngine(),
            remote = remote,
            policy = AssistantRoutingPolicy(
                isBackendConfigured = { BackendClientFactory.isConfigured() },
                isNetworkAvailable = { network.isInternetLikely() },
                hasCloudAiConsent = { prefs.cloudAiConsent },
                isUserSessionActive = { session.isLoggedIn() && session.isUserRole() },
                isOnlineAssistantConfigured = {
                    httpRemote?.isOnlineAssistantConfigured() ?: false
                }
            )
        )
    }
}
