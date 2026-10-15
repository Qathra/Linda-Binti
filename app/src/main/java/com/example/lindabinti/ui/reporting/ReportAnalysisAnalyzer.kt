package com.example.lindabinti.ui.reporting

import com.example.lindabinti.ai.AiAnalysisInput
import com.example.lindabinti.ai.AiAnalysisStorage
import com.example.lindabinti.ai.AiOrchestrator
import com.example.lindabinti.ai.AiAnalysisResult
import com.example.lindabinti.data.local.Report
import com.example.lindabinti.data.local.ReportDao

/**
 * Runs survivor report analysis through the central orchestrator and persists locally.
 */
object ReportAnalysisAnalyzer {

    suspend fun analyzeReport(
        orchestrator: AiOrchestrator,
        report: Report,
        reportDao: ReportDao
    ): AiAnalysisResult {
        val input = AiAnalysisInput(
            categoryId = report.categoryId,
            categoryLabel = report.category,
            description = report.body,
            includeDescription = report.body.isNotBlank()
        )
        return orchestrator.analyzeAndPersist(input) { result ->
            reportDao.updateAiAnalysis(
                id = report.id,
                json = AiAnalysisStorage.toJson(result),
                analyzedAt = result.timestamp,
                source = result.sourceLabel
            )
        }
    }
}
