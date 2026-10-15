package com.example.lindabinti

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.lindabinti.ai.AiOrchestrationFactory
import com.example.lindabinti.ai.AiOrchestrator
import com.example.lindabinti.data.local.AppDatabase
import com.example.lindabinti.data.prefs.SecurePrefs
import com.example.lindabinti.data.remote.BackendAccountRepository
import com.example.lindabinti.data.remote.BackendClientFactory
import com.example.lindabinti.data.remote.BackendSessionStore
import com.example.lindabinti.data.remote.LindaBintiApi
import com.example.lindabinti.data.repository.AuthRepository
import com.example.lindabinti.data.repository.ContactRepository
import com.example.lindabinti.data.repository.EducationRepository
import com.example.lindabinti.data.repository.ReportRepository
import com.example.lindabinti.data.repository.RiskRepository
import com.example.lindabinti.data.repository.SosRepository
import com.example.lindabinti.data.repository.SupportServicesRepository
import com.example.lindabinti.data.repository.SupportServicesCache
import com.example.lindabinti.data.stats.StatsRepository
import com.example.lindabinti.data.education.EducationContentRepository
import com.example.lindabinti.data.offline.NetworkMonitor
import com.example.lindabinti.data.sync.AnalyticsUploadManager
import com.example.lindabinti.data.sync.ReportSyncManager
import com.example.lindabinti.data.sync.SyncCoordinator
import com.example.lindabinti.geo.ReportAreaResolver
import com.example.lindabinti.sos.LocationProvider
import com.example.lindabinti.sos.SmsSender
import com.example.lindabinti.sos.SosDispatcher
import com.example.lindabinti.sos.SosEmergencyCategory
import com.example.lindabinti.sos.SosLocationRules
import com.example.lindabinti.sos.SosStatusUpdater
import com.example.lindabinti.sos.SosStatusUpdaterImpl
import com.example.lindabinti.util.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LindaBintiApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var securePrefs: SecurePrefs
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var contactRepository: ContactRepository
        private set
    lateinit var sosRepository: SosRepository
        private set
    lateinit var reportRepository: ReportRepository
        private set
    lateinit var riskRepository: RiskRepository
        private set
    lateinit var educationRepository: EducationRepository
        private set
    lateinit var statsRepository: StatsRepository
        private set
    lateinit var aiOrchestrator: AiOrchestrator
        private set
    lateinit var smsSender: SmsSender
        private set
    lateinit var locationProvider: LocationProvider
        private set
    lateinit var reportAreaResolver: ReportAreaResolver
        private set
    lateinit var sosStatusUpdater: SosStatusUpdater
        private set
    lateinit var backendSession: BackendSessionStore
        private set
    lateinit var backendAccountRepository: BackendAccountRepository
        private set
    lateinit var reportSyncManager: ReportSyncManager
        private set
    lateinit var analyticsUploadManager: AnalyticsUploadManager
        private set
    lateinit var supportServicesRepository: SupportServicesRepository
        private set
    lateinit var educationContentRepository: EducationContentRepository
        private set
    lateinit var networkMonitor: NetworkMonitor
        private set
    lateinit var syncCoordinator: SyncCoordinator
        private set
    lateinit var sosDispatcher: SosDispatcher
        private set

    private var api: LindaBintiApi? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        securePrefs = SecurePrefs(this)
        LocaleHelper.applyLanguage(securePrefs.language)

        database = AppDatabase.getInstance(this)
        authRepository = AuthRepository(securePrefs)
        backendSession = BackendSessionStore(securePrefs)
        api = BackendClientFactory.create()
        backendAccountRepository = BackendAccountRepository(api, backendSession)

        contactRepository = ContactRepository(database.trustedContactDao())
        sosRepository = SosRepository(database.sosDao())
        reportRepository = ReportRepository(database.reportDao(), securePrefs, backendSession)
        riskRepository = RiskRepository(database.riskDao())
        educationRepository = EducationRepository(database.educationDao())
        statsRepository = StatsRepository(
            sosDao = database.sosDao(),
            reportDao = database.reportDao(),
            riskDao = database.riskDao()
        )
        reportSyncManager = ReportSyncManager(
            reportDao = database.reportDao(),
            api = api,
            session = backendSession,
            hasSyncConsent = { securePrefs.syncReportsConsent }
        )
        analyticsUploadManager = AnalyticsUploadManager(api, backendSession, securePrefs)
        supportServicesRepository = SupportServicesRepository(api, SupportServicesCache(this))
        educationContentRepository = EducationContentRepository(this, api)
        networkMonitor = NetworkMonitor(this)
        syncCoordinator = SyncCoordinator(
            network = networkMonitor,
            reportSyncManager = reportSyncManager,
            supportServicesRepository = supportServicesRepository,
            educationContentRepository = educationContentRepository,
            analyticsUploadManager = analyticsUploadManager,
            statsRepository = statsRepository,
            prefs = securePrefs,
            session = backendSession,
            scope = appScope,
            ensureSession = { backendAccountRepository.ensureDeviceSession(securePrefs) }
        )
        aiOrchestrator = AiOrchestrationFactory.createOrchestrator(
            context = this,
            prefs = securePrefs,
            session = backendSession
        )
        smsSender = SmsSender(this)
        locationProvider = LocationProvider(this)
        reportAreaResolver = ReportAreaResolver(locationProvider)
        sosStatusUpdater = SosStatusUpdaterImpl(sosRepository)
        sosDispatcher = SosDispatcher(
            appContext = this,
            contactRepository = contactRepository,
            sosRepository = sosRepository,
            locationProvider = locationProvider,
            smsSender = smsSender,
            securePrefs = securePrefs
        )

        createSosNotificationChannel()

        // Pull content + push queued reports when online / on reconnect.
        // Do not overwrite the user's sync or cloud-AI consent flags.
        syncCoordinator.start()
        appScope.launch {
            runCatching { backendAccountRepository.ensureDeviceSession(securePrefs) }
            runCatching { locationProvider.warmCache(SosLocationRules.SOS_TIMEOUT_MS) }
            runCatching { reportRepository.queueLocalReportsForSync() }
            runCatching { syncCoordinator.syncNow() }
        }
    }

    fun enqueueSos(category: SosEmergencyCategory) {
        locationProvider.beginSosHold()
        appScope.launch {
            try {
                // SMS first — never wait on NGO sync / network.
                runCatching { sosDispatcher.dispatch(category) }
                    .onFailure { android.util.Log.e("LindaSos", "SOS dispatch failed", it) }
            } finally {
                locationProvider.endSosHold()
            }
            notifyNgoAfterSos()
        }
    }

    /** After emergency SMS — never gates or delays [SosDispatcher]. */
    fun notifyNgoAfterSos() {
        appScope.launch {
            runCatching {
                backendAccountRepository.ensureDeviceSession(securePrefs)
                val area = resolveSosAreaForNgo()
                reportRepository.queueSosAlertForNgo(area)
                reportSyncManager.syncPending(forceRetry = true, allowSosWithoutConsent = true)
                syncCoordinator.syncNow()
            }
        }
    }

    private suspend fun resolveSosAreaForNgo(): com.example.lindabinti.geo.ReportArea? {
        return reportAreaResolver.resolveAreaOrNull()
    }

    suspend fun resetAllLocalData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        authRepository.wipeSecurePrefs()
        securePrefs = SecurePrefs(this@LindaBintiApp)
        authRepository = AuthRepository(securePrefs)
        backendSession = BackendSessionStore(securePrefs)
        reportRepository = ReportRepository(database.reportDao(), securePrefs, backendSession)
        reportSyncManager = ReportSyncManager(database.reportDao(), api, backendSession) {
            securePrefs.syncReportsConsent
        }
        analyticsUploadManager = AnalyticsUploadManager(api, backendSession, securePrefs)
        backendAccountRepository = BackendAccountRepository(api, backendSession)
        supportServicesRepository.resetToBundled()
        syncCoordinator = SyncCoordinator(
            network = networkMonitor,
            reportSyncManager = reportSyncManager,
            supportServicesRepository = supportServicesRepository,
            educationContentRepository = educationContentRepository,
            analyticsUploadManager = analyticsUploadManager,
            statsRepository = statsRepository,
            prefs = securePrefs,
            session = backendSession,
            scope = appScope,
            ensureSession = { backendAccountRepository.ensureDeviceSession(securePrefs) }
        )
        LocaleHelper.applyLanguage("en")
    }

    private fun createSosNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SOS_CHANNEL_ID,
                "Calculator",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background work"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val SOS_CHANNEL_ID = "calc_work"
        lateinit var instance: LindaBintiApp
            private set
    }
}
