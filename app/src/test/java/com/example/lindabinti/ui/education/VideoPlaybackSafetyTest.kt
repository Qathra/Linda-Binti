package com.example.lindabinti.ui.education

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlaybackSafetyTest {

    @Test
    fun errorListenerConsumesOsAlertDialog() {
        assertTrue(VideoPlaybackSafety.consumeOsErrorDialog())
    }

    @Test
    fun toastOnlyWhenActivityWindowIsAlive() {
        assertTrue(
            VideoPlaybackSafety.canShowUserMessage(
                hasActivity = true,
                finishing = false,
                destroyed = false
            )
        )
        assertFalse(
            VideoPlaybackSafety.canShowUserMessage(
                hasActivity = true,
                finishing = true,
                destroyed = false
            )
        )
        assertFalse(
            VideoPlaybackSafety.canShowUserMessage(
                hasActivity = true,
                finishing = false,
                destroyed = true
            )
        )
        assertFalse(
            VideoPlaybackSafety.canShowUserMessage(
                hasActivity = false,
                finishing = false,
                destroyed = false
            )
        )
    }
}
