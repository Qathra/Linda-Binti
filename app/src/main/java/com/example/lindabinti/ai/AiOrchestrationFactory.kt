package com.example.lindabinti.ai

import android.content.Context
import com.example.lindabinti.assistant.HttpAssistantRemoteClient
import com.example.lindabinti.assistant.LocalAssistantEngine
import com.example.lindabinti.assistant.UnavailableAssistantRemoteClient
import com.example.lindabinti.data.offline.NetworkMonitor
import com.example.lindabinti.data.prefs.SecurePrefs
import com.example.lindabinti.data.remote.BackendClientFactory
import com.example.lindabinti.data.remote.BackendSession

/**
 * Creates the single production [AiOrchestrator] for all survivor AI on Android.
 */
object AiOrchestrationFactory {

    fun createOrchestrator(
        context: Context,
        prefs: SecurePrefs,
        session: BackendSession
    ): AiOrchestrator {
        val api = BackendClientFactory.create()
        val onlineClient: OnlineAiClient = if (api != null) {
            HttpOnlineAiClient(api, session) { prefs.cloudAiConsent }
        } else {
            UnavailableOnlineAiClient()
        }
        val remoteAssistant = if (api != null) {
            HttpAssistantRemoteClient(api, session) { prefs.cloudAiConsent }
        } else {
            UnavailableAssistantRemoteClient()
        }
        val httpAssistant = remoteAssistant as? HttpAssistantRemoteClient
        val network = NetworkMonitor(context)
        val policy = AiRoutingPolicy(
            isBackendConfigured = { BackendClientFactory.isConfigured() },
            isNetworkAvailable = { network.isInternetLikely() },
            hasCloudAiConsent = { prefs.cloudAiConsent },
            isUserSessionActive = { session.isLoggedIn() && session.isUserRole() },
            isOnlineAiConfigured = { onlineClient.isOnlineAiConfigured() }
        )
        return AiOrchestrator(
            offline = OfflineAiAnalysisEngine(),
            online = OnlineAiAnalysisEngine(onlineClient),
            localAssistant = LocalAssistantEngine(),
            remoteAssistant = remoteAssistant,
            policy = policy,
            isOnlineAssistantConfigured = {
                httpAssistant?.isOnlineAssistantConfigured() ?: false
            }
        )
    }
}
