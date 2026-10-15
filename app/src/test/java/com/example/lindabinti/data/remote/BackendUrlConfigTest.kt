package com.example.lindabinti.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendUrlConfigTest {

    @Test
    fun normalizePhysicalDeviceLanUrl() {
        assertEquals(
            "http://192.168.100.44:8080/",
            BackendUrlConfig.normalize("http://192.168.100.44:8080/")
        )
        assertEquals(
            "http://192.168.100.44:8080/",
            BackendUrlConfig.normalize("http://192.168.100.44:8080")
        )
    }

    @Test
    fun blankMeansDisabledForRelease() {
        assertNull(BackendUrlConfig.normalize(""))
        assertNull(BackendUrlConfig.normalize("   "))
        assertFalse(BackendUrlConfig.isConfigured(""))
    }

    @Test
    fun configuredWhenLanUrlPresent() {
        assertTrue(BackendUrlConfig.isConfigured("http://192.168.100.44:8080/"))
    }

    @Test
    fun rejectsLoopbackAndEmulatorGatewayForPhysicalDevices() {
        BackendUrlConfig.assertPhysicalDeviceSafe("http://192.168.100.44:8080/")
        assertTrue(BackendUrlConfig.isLanIpHost("http://192.168.100.44:8080/"))
        assertFalse(BackendUrlConfig.isLanIpHost("http://10.0.2.2:8080/"))
        try {
            BackendUrlConfig.assertPhysicalDeviceSafe("http://10.0.2.2:8080/")
            throw AssertionError("expected reject of 10.0.2.2")
        } catch (_: IllegalArgumentException) {
        }
        try {
            BackendUrlConfig.assertPhysicalDeviceSafe("http://127.0.0.1:8080/")
            throw AssertionError("expected reject of 127.0.0.1")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun releaseRejectsCleartextHttp() {
        assertTrue(
            BackendUrlConfig.rejectCleartextInRelease("http://192.168.100.44:8080/", debug = false)
        )
        assertFalse(
            BackendUrlConfig.rejectCleartextInRelease("https://api.example.org/", debug = false)
        )
        assertFalse(
            BackendUrlConfig.rejectCleartextInRelease("http://192.168.100.44:8080/", debug = true)
        )
    }
}
