package com.lindabinti.backend.ai

/**
 * Tries the online provider, then rule-based priority so a report is never blocked.
 */
class FallbackAiRiskAnalyzer(
    private val remote: AiRiskAnalyzer?,
    private val rules: AiRiskAnalyzer = RulesAiRiskAnalyzer()
) : AiRiskAnalyzer {

    override fun analyze(input: AiRiskInput): AiRiskResult {
        if (remote == null) {
            return rules.analyze(input).copy(status = AiStatus.UNAVAILABLE)
        }
        return try {
            val result = remote.analyze(input)
            if (result.status != AiStatus.COMPLETE || !AiRiskLevel.isValid(result.riskLevel)) {
                fallback(input, AiStatus.INVALID)
            } else {
                result
            }
        } catch (e: InvalidAiResultException) {
            val status = if (e.message?.contains("Missing", ignoreCase = true) == true) {
                AiStatus.MISSING
            } else {
                AiStatus.INVALID
            }
            fallback(input, status)
        } catch (_: AiTransportException.Timeout) {
            fallback(input, AiStatus.TIMEOUT)
        } catch (_: AiTransportException.Network) {
            fallback(input, AiStatus.UNAVAILABLE)
        } catch (_: Exception) {
            fallback(input, AiStatus.FAILED)
        }
    }

    private fun fallback(input: AiRiskInput, status: String): AiRiskResult {
        val base = rules.analyze(input)
        val note = when (status) {
            AiStatus.TIMEOUT -> "Online AI timed out. Rule-based priority is shown. Staff may retry."
            AiStatus.INVALID -> "Online AI returned an invalid result. Rule-based priority is shown. Staff may retry."
            AiStatus.MISSING -> "Online AI returned no result. Rule-based priority is shown. Staff may retry."
            AiStatus.FAILED -> "Online AI failed. Rule-based priority is shown. Staff may retry."
            else -> "Online AI is unavailable. Rule-based priority is shown. Staff may retry."
        }
        return base.copy(
            source = "rules",
            status = status,
            explanation = "${base.explanation} $note".trim()
        )
    }
}

object AiAnalyzerFactory {
    fun fromConfig(config: AiConfig, http: AiHttpClient = JdkAiHttpClient()): AiRiskAnalyzer {
        val remote = if (config.onlineEnabled) HttpAiRiskAnalyzer(config, http) else null
        return FallbackAiRiskAnalyzer(remote)
    }
}
