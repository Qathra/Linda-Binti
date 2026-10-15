package com.lindabinti.backend.ai

import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Optional live call against a real OpenAI-compatible provider.
 * Run only when credentials are present:
 *   set LINDA_AI_LIVE_TEST=1
 *   set LINDA_AI_ENABLED=1
 *   set LINDA_AI_API_KEY=...
 *   set LINDA_AI_BASE_URL=https://api.openai.com
 */
class LiveOnlineAiProviderTest {

    @Test
    fun liveSurvivorAnalyzeAgainstRealProvider() {
        assumeTrue(
            "Set LINDA_AI_LIVE_TEST=1 to run live provider tests",
            System.getenv("LINDA_AI_LIVE_TEST") == "1"
        )
        val config = AiConfig.fromEnv()
        assumeTrue("Online AI not configured in environment", config.onlineEnabled)

        val input = PiiMinimizer.toInput(
            categoryId = "fgm_pressure",
            categoryLabel = "FGM pressure",
            body = "Family is planning cutting ceremony soon",
            includeBody = true
        )
        val provider = OpenAiCompatibleProvider(config, JdkAiHttpClient())
        val result = provider.analyzeReport(input)

        org.junit.Assert.assertEquals(AiStatus.COMPLETE, result.aiStatus)
        org.junit.Assert.assertTrue(result.explanation.isNotBlank())
        org.junit.Assert.assertTrue(result.confidence in 0.0..1.0)
        org.junit.Assert.assertFalse(result.explanation.contains(config.apiKey))
    }
}
