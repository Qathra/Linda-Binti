package com.example.lindabinti.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.HttpException
import java.io.IOException

object BackendHttpErrors {
    private val gson = Gson()

    fun toUserMessage(error: Throwable): String {
        val detail = when (error) {
            is HttpException -> {
                val body = runCatching { error.response()?.errorBody()?.string() }.getOrNull()
                val fromJson = body?.let { parseErrorField(it) }
                mapBackendError(fromJson) ?: fromJson ?: "HTTP ${error.code()}"
            }
            is IOException -> {
                val base = BackendClientFactory.resolvedBaseUrl().orEmpty()
                val attempted = BackendClientFactory.lastAttemptedUrl().orEmpty()
                val where = when {
                    attempted.isNotBlank() -> " | url=$attempted"
                    base.isNotBlank() -> " | base=$base"
                    else -> ""
                }
                "Network error: ${error.message ?: "unreachable"}$where"
            }
            else -> error.message ?: "Request failed"
        }
        return detail
    }

    fun isConnectFailure(error: Throwable): Boolean {
        if (error !is IOException) return false
        val msg = error.message.orEmpty().lowercase()
        return msg.contains("failed to connect") ||
            msg.contains("connection timed out") ||
            msg.contains("connect timed out") ||
            msg.contains("etimedout")
    }

    private fun parseErrorField(raw: String): String? {
        return runCatching {
            gson.fromJson(raw, JsonObject::class.java)?.get("error")?.asString
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun mapBackendError(code: String?): String? = when (code) {
        "email_taken" -> "This email is already registered. Tap Sign in instead."
        "password_too_short" -> "Password must be at least 8 characters."
        "invalid_email" -> "Enter a valid email address (must include @)."
        "register_failed" -> "Registration failed. Use a valid email and password (min 8 characters)."
        "login_failed", "unauthorized" -> "Sign in failed. Check email and password."
        else -> null
    }
}
