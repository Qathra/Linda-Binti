package com.example.lindabinti.data.repository

import android.content.Context
import com.example.lindabinti.data.content.SupportService
import com.example.lindabinti.data.content.SupportServiceRecord
import com.example.lindabinti.data.content.SupportServicesCatalog
import com.example.lindabinti.data.content.SupportServicesMerger
import com.example.lindabinti.data.remote.LindaBintiApi
import com.example.lindabinti.data.remote.SupportServicesCatalogDto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SupportServicesCache(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun read(): SupportServicesCatalogDto? {
        val json = prefs.getString(KEY, null) ?: return null
        return runCatching { gson.fromJson(json, SupportServicesCatalogDto::class.java) }.getOrNull()
    }

    fun write(dto: SupportServicesCatalogDto) {
        prefs.edit().putString(KEY, gson.toJson(dto)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        private const val PREFS = "linda_support_catalog"
        private const val KEY = "catalog_json"
    }
}

/**
 * Offline-first directory. Bundled verified services always load.
 * A later backend pack may overlay text or add services; unverified phones are stripped.
 */
class SupportServicesRepository(
    private val api: LindaBintiApi?,
    private val cache: SupportServicesCache?
) {
    private val _records = MutableStateFlow(initialRecords())
    val records: StateFlow<List<SupportServiceRecord>> = _records.asStateFlow()

    fun services(language: String): List<SupportService> =
        SupportServicesCatalog.services(language, _records.value)

    suspend fun refreshFromBackend() = withContext(Dispatchers.IO) {
        val dto = api?.supportServices() ?: return@withContext
        if (dto.services.isEmpty()) return@withContext
        cache?.write(dto)
        _records.value = SupportServicesMerger.merge(SupportServicesCatalog.records, dto)
    }

    fun resetToBundled() {
        cache?.clear()
        _records.value = SupportServicesCatalog.records
    }

    private fun initialRecords(): List<SupportServiceRecord> =
        SupportServicesMerger.merge(SupportServicesCatalog.records, cache?.read())
}