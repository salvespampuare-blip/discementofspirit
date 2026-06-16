package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApi {
    private const val TAG = "GeminiApi"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeSpiritualLog(title: String, content: String, intensity: Int, stateEstimate: String): AnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
            Log.w(TAG, "No valid Gemini API key found, running local heuristic classifier.")
            return@withContext fallbackLocalAnalysis(title, content, stateEstimate)
        }

        val systemPrompt = """
            You are a wise spiritual director trained in the Christian tradition of the Discernment of Spirits according to St. Ignatius of Loyola's First Week Rules.
            Analyze the user's daily spiritual journal entry.
            Identify:
            1. The spiritual state: 'CONSOLATION' (peace, faith, hope, charity, joy, moving closer to God), 'DESOLATION' (darkness, sadness, restlessness, sloth, doubts, moving further from God, hiding secrets), or 'NEUTRAL'.
            2. The precise Ignatian Rule (Rules 1 to 14 of the First Week) that applies best to their experience. Reference it by name and number (e.g., "Rule 13: The Secret Tactic of the Enemy").
            3. A warm, compassionate spiritual guidance/actionable insight (max 3 sentences) grounded in the teachings of Timothy Gallagher, Pope Francis, and Saint Ignatius.
            4. Spiritual Security Alerts: If there are warning signs like prolonged desolation, severe doubts, or hiding struggles in secrecy (Rule 13), flags these as anomalies with a recommendation.

            You MUST respond ONLY with a clean JSON object containing the keys: 'state', 'rule', 'insight', 'alerts'. Do not encapsulate it in markdown codeblocks. Example schema:
            {
              "state": "DESOLATION",
              "rule": "Rule 13: Secrets",
              "insight": "The Enemy desires your struggles to remain hidden. Reveal this desolation to a trusted spiritual director to break its hold.",
              "alerts": "WARNING: Silent isolation detected. Reach out to your director immediately."
            }
        """.trimIndent()

        val prompt = """
            Journal Entry Title: $title
            State selected by user: $stateEstimate (intensity $intensity/10)
            Content: $content
        """.trimIndent()

        try {
            // Build request JSON
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                }
                put("contents", contentsArray)

                val systemInstruction = JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                    })
                }
                put("systemInstruction", systemInstruction)

                val generationConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.3)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val url = "$BASE_URL?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errMsg = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Gemini API call failed: ${response.code} $errMsg")
                    return@withContext fallbackLocalAnalysis(title, content, stateEstimate)
                }

                val responseBodyStr = response.body?.string() ?: throw Exception("Empty response body")
                Log.d(TAG, "Gemini responds: $responseBodyStr")

                val jsonResponse = JSONObject(responseBodyStr)
                val candidates = jsonResponse.getJSONArray("candidates")
                val textResponse = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                // Parse parsed JSON
                val resultJson = JSONObject(textResponse.trim())
                AnalysisResult(
                    state = resultJson.optString("state", "NEUTRAL"),
                    ruleApplied = resultJson.optString("rule", "Unknown Rule"),
                    insight = resultJson.optString("insight", "Draw near to God; seek peace and guidance."),
                    alerts = resultJson.optString("alerts", "").takeIf { it.isNotBlank() }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
            fallbackLocalAnalysis(title, content, stateEstimate)
        }
    }

    private fun fallbackLocalAnalysis(title: String, content: String, stateEstimate: String): AnalysisResult {
        // Simple offline local pattern recognizer
        val lower = "$title $content".lowercase()
        var state = stateEstimate
        var rule = "General Discernment Care"
        var insight = "Continue to be conscious of your interior movements. Reflection is key."
        var alerts: String? = null

        val isConsolationWords = listOf("peace", "joy", "consoled", "love", "fervor", "grateful", "tears of love", "happy", "pray", "close", "warm")
        val isDesolationWords = listOf("sad", "dark", "heavy", "dry", "sloth", "neglect", "tepid", "doubt", "cold", "restless", "frustrated", "secret", "hide", "hiding")

        val cCount = isConsolationWords.count { lower.contains(it) }
        val dCount = isDesolationWords.count { lower.contains(it) }

        if (cCount > dCount) {
            state = "CONSOLATION"
            if (lower.contains("tears")) {
                rule = "Rule 3: Spiritual Consolation"
                insight = "Tears of love and remorse are direct movements of consoles. Praise God and let your soul expand."
            } else {
                rule = "Rule 10: Strength for Desolation"
                insight = "While in consolation, gather strength and reflect on how you will carry yourself when trial comes."
            }
        } else if (dCount > cCount || lower.contains("sad") || lower.contains("dark")) {
            state = "DESOLATION"
            if (lower.contains("secret") || lower.contains("hide")) {
                rule = "Rule 13: Secrets Exposed"
                insight = "The enemy thrives in secrecy, like a false lover. Share your interior thoughts with your guide to break this spell."
                alerts = "ALERT: Keep secrets anomaly. Rule 13 warning triggers."
            } else if (lower.contains("neglect") || lower.contains("lazy") || lower.contains("pray less")) {
                rule = "Rule 9: Cause of Desolation"
                insight = "Negligence in spiritual exercises removes consolation. Renew your prayer times with focus, Rule 6 advises doing more prayer."
                alerts = "ALERT: Negligence pattern detected. Rule 9 desolation triggers."
            } else {
                rule = "Rule 5: Constant Resolutions"
                insight = "When desolated, the bad spirit counsels. Never make new resolutions or changes now. Stay firm!"
            }
        } else {
            // Apply based on user estimation
            if (state == "CONSOLATION") {
                rule = "Rule 3: Consolation"
                insight = "A wholesome sense of closeness to your Creator. Work to remain humbled and active in deeds."
            } else if (state == "DESOLATION") {
                rule = "Rule 4: Desolation"
                insight = "You are going through a test of grace. Have patience, desolation never lasts. Consolation will soon return!"
            }
        }

        return AnalysisResult(state, rule, insight, alerts)
    }
}

data class AnalysisResult(
    val state: String,
    val ruleApplied: String,
    val insight: String,
    val alerts: String?
)
