package com.example.lindabinti.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SurvivorSupportCatalogTest {

    @Test
    fun coreMessageIsPresentInEnglishKiswahiliAndMaasai() {
        assertTrue(
            SurvivorSupportCatalog.coreMessage("en").contains("not less of a person", ignoreCase = true)
        )
        assertTrue(SurvivorSupportCatalog.coreMessage("sw").isNotBlank())
        assertTrue(SurvivorSupportCatalog.coreMessage("maa").isNotBlank())
        assertNotEquals(
            SurvivorSupportCatalog.coreMessage("en"),
            SurvivorSupportCatalog.coreMessage("maa")
        )
    }

    @Test
    fun truthsCoverRequiredGuidance() {
        val ids = SurvivorSupportCatalog.truths("en").map { it.id }
        assertEquals(SurvivorSupportCatalog.truthIds, ids)
        assertEquals(
            SurvivorSupportCatalog.truths("en").map { it.id },
            SurvivorSupportCatalog.truths("sw").map { it.id }
        )
        val blob = SurvivorSupportCatalog.truths("en").joinToString { "${it.title} ${it.body}" }
        assertTrue(blob.contains("not to blame", ignoreCase = true))
        assertTrue(blob.contains("worth", ignoreCase = true))
        assertTrue(blob.contains("safety", ignoreCase = true))
        assertTrue(blob.contains("not weakness", ignoreCase = true))
        assertTrue(blob.contains("someone you trust", ignoreCase = true))
        assertTrue(blob.contains("medical", ignoreCase = true))
        assertTrue(blob.contains("protection", ignoreCase = true))
        assertTrue(blob.contains("ready and safe", ignoreCase = true))
        assertTrue(blob.contains("take time", ignoreCase = true))
        assertTrue(blob.contains("counselling", ignoreCase = true))
    }

    @Test
    fun doesNotDiagnoseOrReplaceACounsellor() {
        val all = buildString {
            append(SurvivorSupportCatalog.coreMessage("en"))
            SurvivorSupportCatalog.truths("en").forEach { append(it.body) }
            SurvivorSupportCatalog.messages("en").forEach { append(it.text) }
            SurvivorSupportCatalog.nextSteps("en").forEach { append(it.body) }
        }.lowercase()
        assertFalse(all.contains("you have ptsd"))
        assertFalse(all.contains("this app is a counsellor"))
        assertFalse(all.contains("this app provides therapy"))
        assertTrue(all.contains("cannot take their place") || all.contains("does not provide therapy"))
    }

    @Test
    fun nextStepsAndMessagesAreOfflineBundled() {
        assertTrue(SurvivorSupportCatalog.nextSteps("en").size >= 5)
        assertTrue(SurvivorSupportCatalog.messages("en").size >= 4)
        assertEquals(
            SurvivorSupportCatalog.nextSteps("en").map { it.id },
            SurvivorSupportCatalog.nextSteps("sw").map { it.id }
        )
        assertEquals(
            SurvivorSupportCatalog.messages("en").map { it.id },
            SurvivorSupportCatalog.messages("sw").map { it.id }
        )
    }

    @Test
    fun shortcutsCoverRequestedHelpPaths() {
        val actions = SurvivorSupportCatalog.shortcuts.map { it.action }.toSet()
        assertTrue(actions.contains(SurvivorHelpAction.OPEN_EMERGENCY))
        assertTrue(actions.contains(SurvivorHelpAction.OPEN_CONTACTS))
        assertTrue(actions.contains(SurvivorHelpAction.FILTER_GBV))
        assertTrue(actions.contains(SurvivorHelpAction.FILTER_MEDICAL))
        assertTrue(actions.contains(SurvivorHelpAction.FILTER_COUNSELLING))
        assertTrue(actions.contains(SurvivorHelpAction.FILTER_POLICE))
        assertTrue(actions.contains(SurvivorHelpAction.FILTER_CHILD))
        assertTrue(actions.contains(SurvivorHelpAction.FILTER_FGM))
        assertTrue(actions.contains(SurvivorHelpAction.OPEN_REPORTING))
    }

    @Test
    fun directoryCoversGbvFgmKindsWithoutInventedPhones() {
        val services = LocalContent.supportServices("en")
        val kinds = services.flatMap { listOf(it.kind) + it.alsoKinds }.toSet()
        assertTrue(kinds.contains(SupportServiceKind.GBV))
        assertTrue(kinds.contains(SupportServiceKind.MEDICAL))
        assertTrue(kinds.contains(SupportServiceKind.COUNSELLING))
        assertTrue(kinds.contains(SupportServiceKind.POLICE))
        assertTrue(kinds.contains(SupportServiceKind.CHILD_PROTECTION))
        assertTrue(kinds.contains(SupportServiceKind.FGM))
        assertTrue(kinds.contains(SupportServiceKind.EMERGENCY))
        assertEquals(
            LocalContent.supportServices("en").map { it.id },
            LocalContent.supportServices("sw").map { it.id }
        )
        assertEquals(
            LocalContent.supportServices("en").map { it.id },
            LocalContent.supportServices("so").map { it.id }
        )
    }
}
