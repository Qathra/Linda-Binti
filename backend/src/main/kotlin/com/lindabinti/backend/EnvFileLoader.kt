package com.lindabinti.backend

import java.io.File

/**
 * Loads KEY=VALUE pairs from a local `.env` file into the process environment
 * when the key is not already set. Never logs secret values.
 *
 * Looks for (first match wins):
 * - LINDA_ENV_FILE path
 * - ./backend/.env (repo root)
 * - ./.env (cwd)
 */
object EnvFileLoader {
    fun loadIfPresent() {
        val candidates = buildList {
            System.getenv("LINDA_ENV_FILE")?.trim()?.takeIf { it.isNotEmpty() }?.let { add(File(it)) }
            add(File("backend/.env"))
            add(File(".env"))
        }
        val file = candidates.firstOrNull { it.isFile && it.canRead() } ?: return
        var loaded = 0
        file.readLines().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val eq = line.indexOf('=')
            if (eq <= 0) return@forEach
            val key = line.substring(0, eq).trim()
            if (key.isEmpty() || System.getenv(key) != null) return@forEach
            var value = line.substring(eq + 1).trim()
            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))
            ) {
                value = value.substring(1, value.length - 1)
            }
            // System properties are reliable on Windows JVMs; AiConfig reads both env + props.
            System.setProperty(key, value)
            loaded++
        }
        println("EnvFileLoader: loaded $loaded unset key(s) from ${file.absolutePath}")
    }

    /** Prefer real process env, then System property from `.env`. */
    fun get(key: String): String? =
        System.getenv(key)?.takeIf { it.isNotBlank() }
            ?: System.getProperty(key)?.takeIf { it.isNotBlank() }
}
