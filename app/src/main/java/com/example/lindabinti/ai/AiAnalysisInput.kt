package com.example.lindabinti.ai

data class AiAnalysisInput(
    val categoryId: String,
    val categoryLabel: String,
    val description: String,
    val includeDescription: Boolean = true
)
