package com.example.lindabinti.ui.education

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EducationNavigationTest {

    @Test
    fun hubOpensTopicThenBackReturnsToHub() {
        val opened = EducationNavigation.openTopic(EducationDest.Hub, "fgm")
        assertEquals(EducationDest.Topic("fgm"), opened)
        assertEquals(EducationDest.Hub, EducationNavigation.back(opened))
        assertNull(EducationNavigation.back(EducationDest.Hub))
    }

    @Test
    fun blankTopicIdDoesNotNavigate() {
        val same = EducationNavigation.openTopic(EducationDest.Hub, "")
        assertEquals(EducationDest.Hub, same)
    }

    @Test
    fun topicIdsMatchCatalog() {
        val ids = com.example.lindabinti.data.content.EducationCatalog.topicIds
        assertTrue(ids.containsAll(listOf("gbv", "fgm", "warning_signs", "prevention")))
        ids.forEach { id ->
            val dest = EducationNavigation.openTopic(EducationDest.Hub, id)
            assertEquals(id, (dest as EducationDest.Topic).id)
        }
    }
}
