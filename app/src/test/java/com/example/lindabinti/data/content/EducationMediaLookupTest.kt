package com.example.lindabinti.data.content

import org.junit.Assert.assertEquals
import org.junit.Test

class EducationMediaLookupTest {

    @Test
    fun audioFilenamesFollowLangConvention() {
        assertEquals("edu_audio_gbv_en", EducationMediaLookup.audioRawName("gbv", "en"))
        assertEquals("edu_audio_gbv_sw", EducationMediaLookup.audioRawName("gbv", "sw"))
        assertEquals("edu_audio_fgm_maa", EducationMediaLookup.audioRawName("fgm", "maasai"))
    }

    @Test
    fun videoFilenameIsStablePerTopic() {
        assertEquals("edu_video_warning_signs", EducationMediaLookup.videoRawName("warning_signs"))
    }
}
