package com.lindabinti.backend.ai

/**
 * Audit-friendly logging for AI operations. Never logs report bodies, names, phones, or secrets.
 */
object AiAnalysisLogger {
    fun info(event: String, detail: String) {
        System.err.println("AI[$event] $detail")
    }

    fun failure(code: String, status: String, detail: String) {
        System.err.println("AI[failure] code=$code status=$status detail=$detail")
    }
}
