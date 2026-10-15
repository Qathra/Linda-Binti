package com.lindabinti.backend

import com.lindabinti.backend.ussd.UssdAction
import com.lindabinti.backend.ussd.UssdMenuService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UssdMenuServiceTest {

    @Test
    fun emptyTextShowsSimpleSafaricomStyleMenu() {
        val r = UssdMenuService.handle("s1", "")
        assertFalse(r.endSession)
        assertTrue(r.message.contains("1 Report GBV"))
        assertTrue(r.message.contains("2 Report FGM"))
        assertTrue(r.message.contains("3 Emergency"))
        assertFalse(r.message.contains("+254"))
    }

    @Test
    fun emergencyNumbersDoNotCollectIdentity() {
        val r = UssdMenuService.handle("s1", "3")
        assertTrue(r.endSession)
        assertTrue(r.message.contains("999"))
        assertTrue(r.message.contains("1195"))
        assertEquals(UssdAction.NONE, r.action)
    }

    @Test
    fun gbvThenAreaThenNameQueuesReport() {
        val step1 = UssdMenuService.handle("s", "1")
        assertFalse(step1.endSession)
        assertTrue(step1.message.contains("Kajiado Central"))

        val step2 = UssdMenuService.handle("s", "1*1")
        assertFalse(step2.endSession)
        assertTrue(step2.message.contains("name", ignoreCase = true))

        val done = UssdMenuService.handle("s", "1*1*Jane Wanjiku")
        assertTrue(done.endSession)
        assertEquals(UssdAction.REPORT_GBV, done.action)
        assertEquals("kajiado_central", done.areaCode)
        assertEquals("Jane Wanjiku", done.reporterName)
        assertTrue(done.message.contains("Jane Wanjiku"))
        assertTrue(done.message.contains("Kajiado Central"))
    }

    @Test
    fun fgmUsesEastWhenUserPicksTwo() {
        val done = UssdMenuService.handle("s", "2*2*Amina")
        assertEquals(UssdAction.REPORT_FGM, done.action)
        assertEquals("kajiado_east", done.areaCode)
        assertEquals("Amina", done.reporterName)
    }

    @Test
    fun fgmTypedAreaThenName() {
        val done = UssdMenuService.handle("s", "2*Kajiado Central*Mary Nkini")
        assertEquals(UssdAction.REPORT_FGM, done.action)
        assertEquals("kajiado_central", done.areaCode)
        assertEquals("Mary Nkini", done.reporterName)
    }

    @Test
    fun unknownChoiceReturnsMenu() {
        val r = UssdMenuService.handle("s1", "9")
        assertFalse(r.endSession)
        assertTrue(r.message.contains("Unknown"))
    }
}
