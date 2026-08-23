package com.example.otadashboard.security

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_logs")
data class ScanLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String, // "SECURITY", "CLAMAV", "OTA"
    val level: String, // "INFO", "WARN", "CRITICAL"
    val message: String
)
