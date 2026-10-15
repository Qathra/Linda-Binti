package com.example.lindabinti.data.content

import android.content.Context

/**
 * Offline media lookup. Screens do not hard-code files.
 *
 * Audio files (optional, later): res/raw/edu_audio_{topicId}_{lang}.mp3
 * Video files (optional, later): res/raw/edu_video_{topicId}.mp4
 *
 * If a file is missing, the UI shows the illustrated slideshow instead.
 * Adding Maa audio later is a new raw file — no UI rewrite.
 */
object EducationMediaLookup {

    fun audioRawName(topicId: String, language: String): String {
        val lang = SupportedLanguages.normalize(language)
        return "edu_audio_${topicId}_$lang"
    }

    fun videoRawName(topicId: String): String = "edu_video_$topicId"

    fun rawId(context: Context, rawName: String): Int {
        if (rawName.isBlank()) return 0
        return context.resources.getIdentifier(rawName, "raw", context.packageName)
    }

    fun hasRaw(context: Context, rawName: String): Boolean = rawId(context, rawName) != 0
}
