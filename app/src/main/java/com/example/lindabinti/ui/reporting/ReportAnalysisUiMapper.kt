package com.example.lindabinti.ui.reporting

import android.content.Context
import com.example.lindabinti.R
import com.example.lindabinti.ai.AiAnalysisCategory
import com.example.lindabinti.ai.AiAnalysisSource
import com.example.lindabinti.ai.AiSeverity
import com.example.lindabinti.data.content.EducationCatalog

object ReportAnalysisUiMapper {

    fun sourceLabel(source: AiAnalysisSource): String = when (source) {
        AiAnalysisSource.ONLINE -> "ONLINE AI"
        AiAnalysisSource.OFFLINE -> "OFFLINE AI"
        AiAnalysisSource.HYBRID -> "HYBRID AI"
    }

    fun sourceSubtitle(source: AiAnalysisSource): String = sourceLabel(source)

    fun severityLabel(severity: AiSeverity): String = when (severity) {
        AiSeverity.CRITICAL -> "CRITICAL"
        AiSeverity.HIGH -> "HIGH"
        AiSeverity.ELEVATED -> "ELEVATED"
        AiSeverity.LOWER -> "LOWER"
    }

    fun categoryLabel(context: Context, category: AiAnalysisCategory, categoryId: String): String =
        when (category) {
            AiAnalysisCategory.GBV -> categoryIdLabel(context, categoryId)
            AiAnalysisCategory.FGM -> context.getString(R.string.reporting_cat_fgm_incident)
            AiAnalysisCategory.FGM_THREAT -> context.getString(R.string.reporting_cat_fgm)
            AiAnalysisCategory.RELATED -> context.getString(R.string.reporting_cat_marriage)
            AiAnalysisCategory.UNKNOWN -> context.getString(R.string.reporting_cat_other)
        }

    fun educationTopicTitle(context: Context, topicId: String, language: String): String =
        EducationCatalog.topicById(language, topicId)?.title
            ?: topicId.replace('_', ' ').replaceFirstChar { it.uppercase() }

    private fun categoryIdLabel(context: Context, categoryId: String): String = when (categoryId) {
        "physical" -> context.getString(R.string.reporting_cat_physical)
        "sexual" -> context.getString(R.string.reporting_cat_sexual)
        "emotional" -> context.getString(R.string.reporting_cat_emotional)
        "domestic" -> context.getString(R.string.reporting_cat_domestic)
        "harassment" -> context.getString(R.string.reporting_cat_harassment)
        "threats" -> context.getString(R.string.reporting_cat_threats)
        else -> context.getString(R.string.reporting_cat_other)
    }
}
