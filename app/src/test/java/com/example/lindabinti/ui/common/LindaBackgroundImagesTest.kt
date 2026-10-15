package com.example.lindabinti.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LindaBackgroundImagesTest {

    @Test
    fun slidesContainAllSevenBackgroundImages() {
        assertEquals(7, LindaBackgroundImages.slides.size)
        assertTrue(LindaBackgroundImages.slides.all { it != 0 })
    }
}
