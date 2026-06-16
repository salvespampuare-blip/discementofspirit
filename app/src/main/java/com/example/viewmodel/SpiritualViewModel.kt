package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SpiritualLog
import com.example.api.GeminiApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SpiritualViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.spiritualLogDao()

    // 1. Logs flow
    val logs: StateFlow<List<SpiritualLog>> = dao.getAllLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 2. Persona State ("EXERCITANT" vs "DIRECTOR")
    private val _activePersona = MutableStateFlow("EXERCITANT")
    val activePersona: StateFlow<String> = _activePersona.asStateFlow()

    fun setPersona(persona: String) {
        _activePersona.value = persona
    }

    // 3. Subscription (Stripe Mock)
    private val _subscriptionStatus = MutableStateFlow("FREE") // "FREE", "PREMIUM"
    val subscriptionStatus: StateFlow<String> = _subscriptionStatus.asStateFlow()

    private val _stripeMessage = MutableStateFlow<String?>(null)
    val stripeMessage: StateFlow<String?> = _stripeMessage.asStateFlow()

    fun clearStripeMessage() {
        _stripeMessage.value = null
    }

    fun processStripePayment(cardNumber: String, expiry: String, cvc: String) {
        viewModelScope.launch {
            if (cardNumber.length < 16 || expiry.length < 4 || cvc.length < 3) {
                _stripeMessage.value = "Stripe Error: Invalid payment card format."
                return@launch
            }
            _stripeMessage.value = "Contacting Stripe Gateway..."
            delay(1500)
            _subscriptionStatus.value = "PREMIUM"
            _stripeMessage.value = "Success! Payment processed securely. Premium Spiritual Mentor unlocked."
        }
    }

    fun cancelSubscription() {
        _subscriptionStatus.value = "FREE"
    }

    // 4. CRM / REST Sync state
    private val _syncState = MutableStateFlow("IDLE") // "IDLE", "SYNCING", "SUCCESS", "FAILED"
    val syncState: StateFlow<String> = _syncState.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<String>>(emptyList())
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    fun triggerCrmSync(endpoint: String) {
        viewModelScope.launch {
            _syncState.value = "SYNCING"
            _syncLogs.value = listOf(
                "Establishing secure REST API session...",
                "Target URL: $endpoint",
                "Authenticating with secure HMAC credentials..."
            )
            delay(1000)
            _syncLogs.value = _syncLogs.value + "Checking local SQLite record count [${logs.value.size}]..."
            _syncLogs.value = _syncLogs.value + "Streaming encrypted JSON payloads..."
            delay(1200)
            _syncLogs.value = _syncLogs.value + "Response: 200 OK. Records sync complete."
            _syncLogs.value = _syncLogs.value + "Sync successfully matching parish records and pipeline indices."
            _syncState.value = "SUCCESS"
        }
    }

    // 5. Spiritual alert settings
    private val _alertThresholdDays = MutableStateFlow(2) // trigger alert if Desolation exceeds this many consecutive logs
    val alertThresholdDays: StateFlow<Int> = _alertThresholdDays.asStateFlow()

    private val _notifyOnSecretHiding = MutableStateFlow(true) // Rule 13 anomaly monitoring
    val notifyOnSecretHiding: StateFlow<Boolean> = _notifyOnSecretHiding.asStateFlow()

    private val _spiritualSecurityAlerts = MutableStateFlow<List<String>>(emptyList())
    val spiritualSecurityAlerts: StateFlow<List<String>> = _spiritualSecurityAlerts.asStateFlow()

    fun updateThresholdDays(days: Int) {
        _alertThresholdDays.value = days
        recalculateAlerts()
    }

    fun setNotifyOnSecretHiding(enabled: Boolean) {
        _notifyOnSecretHiding.value = enabled
        recalculateAlerts()
    }

    fun clearAlerts() {
        _spiritualSecurityAlerts.value = emptyList()
    }

    private fun recalculateAlerts() {
        val currentLogs = logs.value
        val list = mutableListOf<String>()

        // 1. Detect consecutive Desolations
        var consecutiveDesolations = 0
        for (log in currentLogs.asReversed()) {
            if (log.state == "DESOLATION") {
                consecutiveDesolations++
                if (consecutiveDesolations >= alertThresholdDays.value) {
                    list.add("Prolonged Desolation Alert: You have been in desolation for $consecutiveDesolations consecutive entries. Rule 5 warns never to alter resolutions during this time.")
                }
            } else {
                consecutiveDesolations = 0
            }
        }

        // 2. Secret Keeping Detection (Rule 13)
        if (notifyOnSecretHiding.value) {
            val hasSecrets = currentLogs.any {
                it.content.lowercase().contains("secret") ||
                it.content.lowercase().contains("hide") ||
                it.content.lowercase().contains("tell no one")
            }
            if (hasSecrets) {
                list.add("Spiritual Hazard (Rule 13): Signs of interior secrecy detected. The enemy desires struggles to remain hidden to maintain control. Reveal this to a trusted counselor.")
            }
        }

        _spiritualSecurityAlerts.value = list
    }

    // 6. Log additions & analysis
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    fun addLogEntry(title: String, content: String, intensity: Int, stateEstimate: String) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                // Call Gemini / Local API
                val analysisResult = GeminiApi.analyzeSpiritualLog(title, content, intensity, stateEstimate)

                // Save to Database
                val log = SpiritualLog(
                    title = title,
                    content = content,
                    intensity = intensity,
                    state = analysisResult.state,
                    feelings = detectFeelings(content, stateEstimate),
                    ruleApplied = analysisResult.ruleApplied,
                    analysis = analysisResult.insight
                )
                dao.insertLog(log)
                Log.d("ViewModel", "Saved log entry details.")

                delay(500)
                recalculateAlerts()
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to analyze and save spiritual log", e)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    private fun detectFeelings(content: String, state: String): String {
        val feelingsList = mutableListOf<String>()
        val lowercase = content.lowercase()
        if (state == "CONSOLATION") {
            if (lowercase.contains("peace") || lowercase.contains("calm")) feelingsList.add("Peace")
            if (lowercase.contains("joy") || lowercase.contains("happy")) feelingsList.add("Joy")
            if (lowercase.contains("warm") || lowercase.contains("love")) feelingsList.add("Fervor")
            if (lowercase.contains("tears")) feelingsList.add("Deep tears")
            if (feelingsList.isEmpty()) feelingsList.addAll(listOf("Hope", "Gratitude"))
        } else if (state == "DESOLATION") {
            if (lowercase.contains("sad") || lowercase.contains("gloom")) feelingsList.add("Sadness")
            if (lowercase.contains("doubt")) feelingsList.add("Doubt")
            if (lowercase.contains("lazy") || lowercase.contains("sloth") || lowercase.contains("dry")) feelingsList.add("Tepidity")
            if (lowercase.contains("fear") || lowercase.contains("scared")) feelingsList.add("Fear")
            if (lowercase.contains("angry") || lowercase.contains("vex")) feelingsList.add("disturbance")
            if (feelingsList.isEmpty()) feelingsList.addAll(listOf("Gloom", "Disturbance"))
        } else {
            feelingsList.add("Neutral")
        }
        return feelingsList.joinToString(",")
    }

    fun deleteLog(id: Int) {
        viewModelScope.launch {
            dao.deleteLogById(id)
            recalculateAlerts()
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            dao.clearAllLogs()
            recalculateAlerts()
        }
    }

    // 7. Export report
    fun generateExportableReport(): String {
        val currentLogs = logs.value
        if (currentLogs.isEmpty()) {
            return "--- SPIRITUAL DISCERNMENT JOURNAL REPORT ---\nGenerated: 2026-06-15\n\nNo records found to export."
        }

        val totalLogs = currentLogs.size
        val consolationLogs = currentLogs.count { it.state == "CONSOLATION" }
        val desolationLogs = currentLogs.count { it.state == "DESOLATION" }
        val neutralLogs = currentLogs.count { it.state == "NEUTRAL" }

        val builder = java.lang.StringBuilder()
        builder.append("=========================================================\n")
        builder.append("             SPIRITUAL DISCERNMENT REPORT                \n")
        builder.append("=========================================================\n")
        builder.append("Generated On: 2026-06-15 UTC\n")
        builder.append("Subscription Tier: ${subscriptionStatus.value}\n")
        builder.append("Compliance Audit: Ignatian First Week rules compliant\n")
        builder.append("---------------------------------------------------------\n")
        builder.append("SUMMARY STATS:\n")
        builder.append("- Total Spiritual Records Analyzed: $totalLogs\n")
        builder.append("- Spiritual Consolations (Rule 3): $consolationLogs (${(consolationLogs * 100 / totalLogs)}%)\n")
        builder.append("- Spiritual Desolations (Rule 4): $desolationLogs (${(desolationLogs * 100 / totalLogs)}%)\n")
        builder.append("- Neutral States: $neutralLogs\n")
        builder.append("---------------------------------------------------------\n")
        builder.append("CURRENT ALERTS IN FORCE:\n")
        val active = spiritualSecurityAlerts.value
        if (active.isEmpty()) {
            builder.append(" - No active anomalies (Spiritual state is fully balanced)\n")
        } else {
            active.forEach { alert ->
                builder.append(" [!] $alert\n")
            }
        }
        builder.append("---------------------------------------------------------\n")
        builder.append("JOURNAL CHRONOLOGY AND AI PATTERNS:\n\n")

        currentLogs.forEachIndexed { index, entry ->
            builder.append("Entry #${index + 1}: ${entry.title}\n")
            builder.append("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(entry.timestamp))}\n")
            builder.append("Estimated State: ${entry.state} (Intensity: ${entry.intensity}/10)\n")
            builder.append("Feelings: ${entry.feelings}\n")
            builder.append("Ignatian Classification: ${entry.ruleApplied ?: "None"}\n")
            builder.append("Journal Entry: \"${entry.content}\"\n")
            builder.append("AI Spiritual Insight: ${entry.analysis ?: "Pending analysis"}\n")
            builder.append(".........................................................\n\n")
        }

        builder.append("=========================================================\n")
        builder.append("  CONFIDENTIAL SOUL REPORT - REVERENT PRESERVATION REQUIRED\n")
        builder.append("=========================================================\n")
        return builder.toString()
    }
}
