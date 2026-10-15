package com.example.lindabinti.data.remote

import com.example.lindabinti.BuildConfig
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Inet4Address
import java.net.InetAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Single shared Retrofit/OkHttp client for ALL backend calls (health, auth, reports, analytics).
 * Debug builds must use [BuildConfig.BACKEND_BASE_URL] (LAN IP). Release stays blank/disabled.
 */
object BackendClientFactory {

    private val lastRequestUrl = AtomicReference<String?>(null)
    private var okHttpClient: OkHttpClient? = null
    private var retrofitApi: LindaBintiApi? = null
    private var boundBaseUrl: String? = null

    fun isConfigured(): Boolean =
        BackendUrlConfig.isConfigured(BuildConfig.BACKEND_BASE_URL)

    fun resolvedBaseUrl(): String? =
        boundBaseUrl ?: BackendUrlConfig.normalize(BuildConfig.BACKEND_BASE_URL)

    fun lastAttemptedUrl(): String? = lastRequestUrl.get()

    @Synchronized
    fun create(baseUrl: String = BuildConfig.BACKEND_BASE_URL): LindaBintiApi? {
        val normalized = BackendUrlConfig.normalize(baseUrl) ?: return null
        if (BackendUrlConfig.rejectCleartextInRelease(normalized, BuildConfig.DEBUG)) {
            return null
        }

        if (retrofitApi != null && boundBaseUrl == normalized) {
            return retrofitApi
        }

        val urlCapture = Interceptor { chain ->
            val req = chain.request()
            lastRequestUrl.set(req.url.toString())
            chain.proceed(req)
        }

        val logging = HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            // Never log bodies (report text). Release must not write request URLs to logcat.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            // Avoid system/Wi‑Fi HTTP proxies that break LAN POSTs while browser/app GET still works.
            .proxy(Proxy.NO_PROXY)
            // Prefer IPv4 for literal LAN backends (avoids IPv6 unreachable races).
            .dns(Ipv4PreferredDns)
            .protocols(listOf(Protocol.HTTP_1_1))
            .retryOnConnectionFailure(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(urlCapture)
            .addInterceptor(logging)
            .build()

        val api = Retrofit.Builder()
            .baseUrl(normalized.toHttpUrl())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LindaBintiApi::class.java)

        okHttpClient = client
        retrofitApi = api
        boundBaseUrl = normalized
        return api
    }

    /** Drop pooled sockets before retrying sync after a connect failure. */
    @Synchronized
    fun evictAllConnections() {
        okHttpClient?.connectionPool?.evictAll()
    }
}

/** Prefer IPv4 addresses when DNS returns both families. */
object Ipv4PreferredDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val all = Dns.SYSTEM.lookup(hostname)
        val v4 = all.filterIsInstance<Inet4Address>()
        return if (v4.isNotEmpty()) v4 + all.filterNot { it is Inet4Address } else all
    }
}
