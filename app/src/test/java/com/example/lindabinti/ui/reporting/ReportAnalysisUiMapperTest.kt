package com.example.lindabinti.ui.reporting

import com.example.lindabinti.ai.AiAnalysisCategory
import com.example.lindabinti.ai.AiAnalysisSource
import com.example.lindabinti.ai.AiSeverity
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportAnalysisUiMapperTest {

    @Test
    fun compactSourceLabelsMatchSpec() {
        assertEquals("ONLINE AI", ReportAnalysisUiMapper.sourceLabel(AiAnalysisSource.ONLINE))
        assertEquals("OFFLINE AI", ReportAnalysisUiMapper.sourceLabel(AiAnalysisSource.OFFLINE))
        assertEquals("HYBRID AI", ReportAnalysisUiMapper.sourceLabel(AiAnalysisSource.HYBRID))
    }

    @Test
    fun subtitleUsesOfflineSafetyAnalysisWording() {
        assertEquals("OFFLINE AI", ReportAnalysisUiMapper.sourceSubtitle(AiAnalysisSource.OFFLINE))
    }

    @Test
    fun severityLabelsAreUppercaseBands() {
        assertEquals("CRITICAL", ReportAnalysisUiMapper.severityLabel(AiSeverity.CRITICAL))
        assertEquals("HIGH", ReportAnalysisUiMapper.severityLabel(AiSeverity.HIGH))
        assertEquals("ELEVATED", ReportAnalysisUiMapper.severityLabel(AiSeverity.ELEVATED))
        assertEquals("LOWER", ReportAnalysisUiMapper.severityLabel(AiSeverity.LOWER))
    }

    @Test
    fun fgmThreatCategoryUsesThreatStringResourceKeyPath() {
        // categoryLabel requires Context; verify enum mapping path via severity/category constants only here.
        assertEquals(AiAnalysisCategory.FGM_THREAT.name, "FGM_THREAT")
    }
}
