package com.example.lindabinti.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EducationCatalogTest {

    @Test
    fun eightGbvFgmTopicsInOrder() {
        assertEquals(EducationCatalog.topicIds, EducationCatalog.topics("en").map { it.id })
        assertEquals(EducationCatalog.topicIds, EducationCatalog.topics("sw").map { it.id })
        assertEquals(8, EducationCatalog.topics("en").size)
    }

    @Test
    fun everyTopicHasMeansSignsDoAndHelp() {
        EducationCatalog.topics("en").forEach { topic ->
            assertTrue(topic.id, topic.means.size >= 3)
            assertTrue(topic.id, topic.warningSigns.size >= 3)
            assertTrue(topic.id, topic.whatToDo.size >= 3)
            assertTrue(topic.id, topic.whereHelp.size >= 3)
            assertTrue(topic.id, topic.learnMore.isNotEmpty())
            topic.means.forEach { assertTrue("${topic.id} $it", it.length <= 90) }
        }
    }

    @Test
    fun kiswahiliUsesSameIdsAndShortCards() {
        val en = EducationCatalog.topics("en")
        val sw = EducationCatalog.topics("sw")
        assertEquals(en.map { it.id }, sw.map { it.id })
        sw.forEach { topic ->
            assertTrue(topic.title.isNotBlank())
            assertFalse(topic.summary.contains("\n\n"))
        }
    }

    @Test
    fun maaUsesMaasaiTopics() {
        assertEquals(EducationCatalog.topicIds, EducationCatalog.topics("maa").map { it.id })
        assertTrue(EducationCatalog.topics("maa").all { it.title.isNotBlank() })
        assertNotEquals(
            EducationCatalog.topics("en").first().title,
            EducationCatalog.topics("maa").first().title
        )
    }

    @Test
    fun quizzesHaveOneCorrectOption() {
        EducationCatalog.topics("en").forEach { topic ->
            assertEquals(1, topic.quiz.options.count { it.correct })
            assertTrue(topic.quiz.options.size >= 2)
        }
    }

    @Test
    fun reportingStaysOnThisPhoneFirst() {
        val reporting = EducationCatalog.topicById("en", "reporting_evidence")!!
        val blob = (reporting.whatToDo + reporting.learnMore).joinToString()
        assertTrue(blob.contains("this phone first", ignoreCase = true))
    }

    @Test
    fun gbvAndFgmBundleOfflineClips() {
        assertEquals("edu_gbv_what", EducationCatalog.topicById("en", "gbv")!!.videoRawName)
        assertEquals("edu_fgm_law", EducationCatalog.topicById("sw", "fgm")!!.videoRawName)
        assertTrue(EducationCatalog.topicById("en", "gbv")!!.slides.size >= 3)
        assertTrue(EducationCatalog.topicById("en", "fgm")!!.slides.size >= 3)
    }

    @Test
    fun staysOnGbvFgmScope() {
        val blob = EducationCatalog.topics("en").joinToString { it.title + it.summary }
        assertFalse(blob.contains("nutrition", ignoreCase = true))
        assertFalse(blob.contains("malaria", ignoreCase = true))
        assertTrue(blob.contains("GBV"))
        assertTrue(blob.contains("FGM"))
    }

    @Test
    fun localContentCategoriesMatchTopics() {
        assertEquals(8, LocalContent.educationCategories("en").size)
        assertEquals("gbv", LocalContent.educationCategories("en").first().id)
    }

    @Test
    fun audioKeysAreStableForFutureMaaFiles() {
        val en = EducationCatalog.topicById("en", "gbv")!!.audioKey
        val maa = EducationCatalog.topicById("maa", "gbv")!!.audioKey
        assertEquals("edu_audio_gbv_en", en)
        assertEquals("edu_audio_gbv_maa", maa)
    }
}
