package com.example.otadashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.otadashboard.security.ScanLogDao

@Composable
fun MainScreen(
    updateJsonUrl: String,
    publicKeyPem: String,
    logDao: ScanLogDao
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Güvenlik", "Güncelleme")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F11))
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // iOS Segmented Control (Tab Bar)
        IosSegmentedControl(
            items = tabs,
            selectedIndex = selectedTab,
            onOptionSelected = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Tab İçerikleri
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> VirusScanTab(logDao = logDao)
                1 -> UpdateTab(updateJsonUrl = updateJsonUrl, publicKeyPem = publicKeyPem)
            }
        }
    }
}

@Composable
fun IosSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C1C1E))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFF2C2C2E) else Color.Transparent)
                    .clickable { onOptionSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Color(0xFF8E8E93),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun VirusScanTab(logDao: ScanLogDao) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🛡️ Güvenlik ve Tarama Paneli",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sistem Taraması Yapmaya Hazır",
            color = Color(0xFF8E8E93),
            fontSize = 14.sp
        )
    }
}

@Composable
fun UpdateTab(updateJsonUrl: String, publicKeyPem: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🚀 OTA Güncelleme Merkezi",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sisteminiz Güncel (v1.0.0)",
            color = Color(0xFF8E8E93),
            fontSize = 14.sp
        )
    }
}
