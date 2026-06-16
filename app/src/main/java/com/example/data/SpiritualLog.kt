package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spiritual_logs")
data class SpiritualLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val content: String,
    val state: String, // "CONSOLATION", "DESOLATION", "NEUTRAL"
    val intensity: Int, // 1 to 10
    val feelings: String, // Comma-separated list of feeling tags (e.g. "Peace,Joy,Fervor")
    val ruleApplied: String?, // Ignatian rule detected (e.g. "Rule 3: Consolation")
    val analysis: String? // AI generated insight or offline insight
)
