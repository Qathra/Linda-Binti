package com.example.lindabinti.sos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SosAlertMessageTest {

    private val triggeredAt = 1_700_000_000_000L
    private val fresh = SosLocation(
        latitude = -1.2921,
        longitude = 36.8219,
        accuracyMeters = 25f,
        status = "ok",
        capturedAtMillis = triggeredAt
    )
    private val cached = SosLocation(
        latitude = -1.2921,
        longitude = 36.8219,
        accuracyMeters = 80f,
        status = "cached",
        capturedAtMillis = triggeredAt - 30 * 60 * 1000L
    )
    private val unavailable = SosLocation(
        latitude = 0.0,
        longitude = 0.0,
        accuracyMeters = null,
        status = "unavailable"
    )

    @Test
    fun gbvMessageIncludesCategoryAndFreshLocation() {
        val msg = SosAlertMessage.build(fresh, triggeredAt, SosEmergencyCategory.GBV)
        assertTrue(msg.startsWith("LINDA BINTI EMERGENCY ALERT"))
        assertTrue(msg.contains("Possible GBV emergency reported."))
        assertTrue(msg.contains("This person has activated an emergency alert and needs immediate assistance."))
        assertTrue(msg.contains("Need help now.").not())
        assertFreshLocation(msg)
        assertSafeSms(msg)
    }

    @Test
    fun fgmMessageIncludesProtectionWording() {
        val msg = SosAlertMessage.build(fresh, triggeredAt, SosEmergencyCategory.FGM)
        assertTrue(msg.contains("Possible FGM-related emergency reported."))
        assertTrue(msg.contains("may need immediate protection").not())
        assertFalse(msg.contains("Possible GBV emergency reported."))
        assertFreshLocation(msg)
        assertSafeSms(msg)
    }

    @Test
    fun fgmThreatMessageIsConcise() {
        val msg = SosAlertMessage.build(fresh, triggeredAt, SosEmergencyCategory.FGM_THREAT)
        assertTrue(msg.contains("Possible FGM threat/emergency reported."))
        assertFalse(msg.contains("Immediate safety assistance may be required."))
        assertFreshLocation(msg)
        assertSafeSms(msg)
    }

    @Test
    fun otherGbvFgmMessage() {
        val msg = SosAlertMessage.build(unavailable, triggeredAt, SosEmergencyCategory.OTHER_GBV_FGM)
        assertTrue(msg.contains("Possible GBV/FGM-related emergency reported."))
        assertTrue(msg.contains("Location pending."))
        assertFalse(msg.contains("maps.google.com"))
        assertSafeSms(msg)
    }

    @Test
    fun generalEmergencyIsDefaultAndUsedForSecretSos() {
        val msg = SmsSender.buildAlertMessage(unavailable, triggeredAt)
        assertTrue(msg.startsWith("LINDA BINTI EMERGENCY ALERT"))
        assertTrue(msg.contains("This person has activated an emergency alert and needs immediate assistance."))
        assertFalse(msg.contains("Need help now."))
        assertFalse(msg.contains("Possible GBV"))
        assertFalse(msg.contains("Possible FGM"))
        assertEquals(SosEmergencyCategory.GENERAL, SosEmergencyCategory.fromWire(null))
        assertSafeSms(msg)
    }

    @Test
    fun locationIncludedWhenAvailable() {
        val msg = SosAlertMessage.build(fresh, triggeredAt, SosEmergencyCategory.GENERAL)
        assertTrue(msg.startsWith("LINDA BINTI EMERGENCY ALERT"))
        assertTrue(msg.contains("https://maps.google.com/?q=-1.29210,36.82190"))
        assertTrue(msg.contains("This person has activated an emergency alert and needs immediate assistance."))
        assertTrue(msg.contains("GPS: -1.29210,36.82190"))
        assertTrue(msg.contains("Time: ${SosAlertMessage.formatTime(triggeredAt)}"))
        assertFalse(msg.contains("Last known location"))
        assertFalse(msg.contains("Location unavailable"))
        assertTrue(msg.indexOf("https://maps.google.com") < msg.indexOf("Time:"))
        assertSafeSms(msg)
    }

    @Test
    fun validCoordinatesAreIncludedEvenIfStatusIsUnexpected() {
        val loc = SosLocation(-1.2921, 36.8219, 40f, "unknown", triggeredAt)
        assertTrue(loc.hasFix())
        val msg = SosAlertMessage.build(loc, triggeredAt, SosEmergencyCategory.GENERAL)
        assertTrue(msg.contains("https://maps.google.com/?q=-1.29210,36.82190"))
        assertTrue(msg.contains("GPS: -1.29210,36.82190"))
        assertFalse(msg.contains("Location pending"))
        assertSafeSms(msg)
    }

    @Test
    fun locationUpdateSmsIsMapsFirst() {
        val msg = SosAlertMessage.buildLocationUpdate(fresh, triggeredAt)
        assertTrue(msg.startsWith("LINDA BINTI LOCATION UPDATE"))
        assertTrue(msg.contains("https://maps.google.com/?q=-1.29210,36.82190"))
        assertTrue(msg.contains("Lat: -1.29210"))
        assertTrue(msg.contains("Lng: 36.82190"))
        assertTrue(msg.indexOf("https://maps.google.com") < msg.indexOf("Lat:"))
        assertSafeSms(msg)
    }

    @Test
    fun locationUnavailableFallbackStillBuildsSms() {
        val msg = SosAlertMessage.build(unavailable, triggeredAt, SosEmergencyCategory.GENERAL)
        assertTrue(msg.contains("Location pending."))
        assertFalse(msg.contains("GPS:"))
        assertFalse(msg.contains("maps.google.com"))
        assertSafeSms(msg)
    }

    @Test
    fun lastKnownLocationIsLabelledWithTimestamp() {
        val msg = SosAlertMessage.build(cached, triggeredAt, SosEmergencyCategory.GBV)
        assertTrue(msg.contains("Last known location:"))
        assertTrue(msg.contains("GPS: -1.29210,36.82190"))
        assertTrue(msg.contains("Recorded: ${SosAlertMessage.formatTime(cached.capturedAtMillis!!)}"))
        assertTrue(msg.contains("https://maps.google.com/?q=-1.29210,36.82190"))
        assertTrue(msg.contains("This person has activated an emergency alert and needs immediate assistance."))
        assertSafeSms(msg)
    }

    @Test
    fun generalAlertFitsOneGsmPart() {
        val msg = SosAlertMessage.build(fresh, triggeredAt, SosEmergencyCategory.GENERAL)
        assertTrue(msg.contains("This person has activated an emergency alert and needs immediate assistance."))
        assertTrue(msg.contains("Time:"))
        assertTrue(msg.length <= 320)
    }

    @Test
    fun mapsLinkUsesUsDecimalPoint() {
        val loc = SosLocation(-1.5, 36.8, 10f, "ok", triggeredAt)
        val msg = SosAlertMessage.build(loc, triggeredAt)
        assertTrue(msg.contains("https://maps.google.com/?q=-1.50000,36.80000"))
        assertFalse(msg.contains("?q=-1,50000"))
    }

    @Test
    fun neverIncludesAccessPinSosCodeOrPrivateReport() {
        SosEmergencyCategory.entries.forEach { category ->
            listOf(fresh, cached, unavailable).forEach { loc ->
                assertSafeSms(SosAlertMessage.build(loc, triggeredAt, category))
            }
        }
    }

    private fun assertFreshLocation(msg: String) {
        assertTrue(msg.contains("Time: ${SosAlertMessage.formatTime(triggeredAt)}"))
        assertTrue(msg.contains("https://maps.google.com/?q=-1.29210,36.82190"))
        assertTrue(msg.contains("GPS: -1.29210,36.82190"))
        assertTrue(msg.indexOf("https://maps.google.com") < msg.indexOf("Time:"))
    }

    private fun assertSafeSms(msg: String) {
        assertFalse(SosAlertMessage.containsSecrets(msg))
        assertFalse(msg.contains("Access PIN", ignoreCase = true))
        assertFalse(msg.contains("Access Code", ignoreCase = true))
        assertFalse(msg.contains("SOS code", ignoreCase = true))
        assertFalse(msg.contains("1102"))
        assertFalse(msg.contains("1111"))
        assertFalse(msg.contains("report description", ignoreCase = true))
        assertFalse(msg.contains("He beat"))
        assertFalse(msg.contains("cutting"))
    }
}
