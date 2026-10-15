package com.example.lindabinti.data.content

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizedCopyTest {

    @Test
    fun englishAndKiswahiliResolve() {
        val copy = Copy(en = "Help", sw = "Msaada")
        assertEquals("Help", copy.t("en"))
        assertEquals("Msaada", copy.t("sw"))
        assertEquals("Msaada", copy.t("sw-KE"))
    }

    @Test
    fun maaAndSomaliFallBackUntilFilled() {
        val copy = Copy(en = "Help", sw = "Msaada")
        assertEquals("Help", copy.t("maa"))
        assertEquals("Help", copy.t("maasai"))
        assertEquals("Help", copy.t("so"))
    }

    @Test
    fun maaCanBeAddedWithoutNewUiCode() {
        val copy = Copy(en = "Help", sw = "Msaada", maa = "Nkidong'o")
        assertEquals("Nkidong'o", copy.t("maa"))
        assertEquals("Help", copy.t("en"))
        assertEquals("Msaada", copy.t("sw"))
    }
}
