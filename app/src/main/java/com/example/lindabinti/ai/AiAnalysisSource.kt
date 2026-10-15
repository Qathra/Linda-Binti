package com.example.lindabinti.ai

enum class AiAnalysisSource {
    OFFLINE,
    ONLINE,
    /** Offline baseline validated and merged with a successful online response. */
    HYBRID;

    val displayLabel: String
        get() = when (this) {
            OFFLINE -> "OFFLINE AI"
            ONLINE -> "ONLINE AI"
            HYBRID -> "HYBRID AI"
        }
}
