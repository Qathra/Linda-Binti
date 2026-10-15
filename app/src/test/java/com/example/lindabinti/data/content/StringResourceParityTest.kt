package com.example.lindabinti.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StringResourceParityTest {

    @Test
    fun englishKiswahiliAndMaasaiShareTheSameKeys() {
        val en = stringNames(resFile("values", "strings.xml"))
        val sw = stringNames(resFile("values-sw", "strings.xml"))
        val mas = stringNames(resFile("values-mas", "strings.xml"))
        val missingInSw = en - sw
        val missingInMas = en - mas
        val extraInSw = sw - en
        val extraInMas = mas - en
        assertTrue("Missing Kiswahili keys: $missingInSw", missingInSw.isEmpty())
        assertTrue("Missing Maasai keys: $missingInMas", missingInMas.isEmpty())
        assertTrue("Extra Kiswahili keys: $extraInSw", extraInSw.isEmpty())
        assertTrue("Extra Maasai keys: $extraInMas", extraInMas.isEmpty())
        assertTrue(en.contains("lang_pack_incomplete"))
        assertTrue(en.contains("sync_finished"))
        assertFalse(resFile("values", "strings.xml").readText().contains("1102"))
        assertFalse(resFile("values-sw", "strings.xml").readText().contains("1102"))
    }

    @Test
    fun somaliPackIsPlaceholderOnly() {
        val so = stringNames(resFile("values-so", "strings.xml"))
        assertEquals(setOf("lang_pack_incomplete"), so)
    }

    private fun resFile(folder: String, name: String): File {
        val candidates = listOf(
            File("src/main/res/$folder/$name"),
            File("app/src/main/res/$folder/$name")
        )
        return candidates.first { it.exists() }
    }

    private fun stringNames(file: File): Set<String> {
        val regex = Regex("""<string\s+name="([^"]+)"""")
        return regex.findAll(file.readText()).map { it.groupValues[1] }.toSet()
    }
}
