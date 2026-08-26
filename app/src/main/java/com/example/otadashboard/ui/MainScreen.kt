package com.example.otadashboard.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.otadashboard.security.ScanLogDao

@Composable
fun MainScreen(
    updateJsonUrl: String,
    publicKeyPem: String,
    logDao: ScanLogDao
) {
    // Geçici/Mevcut UI içeriğin veya ekran tab yapıların
    Text(text = "OTA Dashboard Main Screen")
}
