package com.example.lindabinti.data.education

import android.content.Context
import com.example.lindabinti.data.remote.EducationContentDto
import com.example.lindabinti.data.remote.EducationContentListDto
import com.example.lindabinti.data.remote.LindaBintiApi
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Offline-first education media: cached API catalog + optional downloaded MP4 files.
 */
class EducationContentRepository(
    context: Context,
    private val api: LindaBintiApi?
) {
    private val appContext = context.applicationContext
    private val gson = Gson()
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val videoDir = File(appContext.filesDir, "education_videos").also { it.mkdirs() }

    private val _items = MutableStateFlow(readCache())
    val items: StateFlow<List<EducationContentDto>> = _items.asStateFlow()

    fun videos(language: String? = null, category: String? = null): List<EducationContentDto> =
        _items.value.filter { item ->
            item.contentType.equals("video", ignoreCase = true) &&
                (language.isNullOrBlank() || item.language.equals(language, ignoreCase = true)) &&
                (category.isNullOrBlank() || item.category.equals(category, ignoreCase = true))
        }

    fun textItems(language: String? = null): List<EducationContentDto> =
        _items.value.filter {
            it.contentType.equals("text", ignoreCase = true) &&
                (language.isNullOrBlank() || it.language.equals(language, ignoreCase = true))
        }

    fun isDownloaded(id: String): Boolean = localFile(id).isFile && localFile(id).length() > 0L

    fun localPlaybackUri(id: String): String? =
        localFile(id).takeIf { it.isFile && it.length() > 0L }?.absolutePath

    suspend fun refreshFromBackend(language: String? = null): Boolean = withContext(Dispatchers.IO) {
        val dto = runCatching { api?.educationContent(language = language) }.getOrNull()
            ?: return@withContext false
        if (dto.items.isEmpty() && language != null) {
            // Fallback: all languages if filtered list empty
            val all = runCatching { api?.educationContent() }.getOrNull() ?: return@withContext false
            writeCache(all)
            _items.value = all.items.filter { it.active }
            return@withContext true
        }
        writeCache(dto)
        _items.value = dto.items.filter { it.active }
        true
    }

    suspend fun downloadForOffline(item: EducationContentDto): Boolean = withContext(Dispatchers.IO) {
        if (!item.contentType.equals("video", ignoreCase = true)) return@withContext false
        val url = item.urlOrPath.trim()
        if (url.isBlank()) return@withContext false
        val dest = localFile(item.id)
        val tmp = File(dest.absolutePath + ".part")
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                instanceFollowRedirects = true
            }
            conn.inputStream.use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            if (dest.exists()) dest.delete()
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
            true
        } catch (_: Exception) {
            tmp.delete()
            false
        }
    }

    fun deleteDownload(id: String): Boolean = localFile(id).delete()

    private fun localFile(id: String): File {
        val safe = id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(videoDir, "$safe.mp4")
    }

    private fun writeCache(dto: EducationContentListDto) {
        prefs.edit().putString(KEY, gson.toJson(dto)).apply()
    }

    private fun readCache(): List<EducationContentDto> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            gson.fromJson(json, EducationContentListDto::class.java).items.filter { it.active }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val PREFS = "linda_education_content"
        private const val KEY = "catalog_json"
    }
}
