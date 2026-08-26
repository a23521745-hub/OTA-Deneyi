package com.example.otadashboard.ui.health

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.otadashboard.health.DeviceHealth
import com.example.otadashboard.health.HealthViewModel

@Composable
fun HealthDashboardScreen(viewModel: HealthViewModel) {
    val health by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = "Pil",
            value = "%${health.batteryPercent}" + if (health.isCharging) " · Şarj oluyor" else "",
            detail = health.batteryTempC?.let { "Sıcaklık: ${"%.1f".format(it)} °C" } ?: "Sıcaklık okunamadı",
            progress = health.batteryPercent / 100f
        )
        MetricCard(
            title = "Bellek (RAM)",
            value = "${health.availRamMb} MB boş / ${health.totalRamMb} MB",
            detail = "Kullanılan: ${usedPercent(health.totalRamMb - health.availRamMb, health.totalRamMb)}%",
            progress = usedRatio(health.totalRamMb - health.availRamMb, health.totalRamMb)
        )
        MetricCard(
            title = "Depolama",
            value = "${"%.1f".format(health.availStorageGb)} GB boş / ${"%.1f".format(health.totalStorageGb)} GB",
            detail = "Kullanılan: ${usedPercent(health.totalStorageGb - health.availStorageGb, health.totalStorageGb)}%",
            progress = usedRatio(health.totalStorageGb - health.availStorageGb, health.totalStorageGb)
        )
        InfoCard(health)

        Button(
            onClick = { viewModel.refresh() },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Yenile", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    detail: String,
    progress: Float
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = Color(0xFF0A84FF),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoCard(health: DeviceHealth) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Cihaz Bilgileri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            InfoRow("Model", health.deviceModel)
            InfoRow("Yazılım", health.androidVersion)
            InfoRow("Güvenlik Yaması", health.securityPatch)
            InfoRow("Ağ Bağlantısı", health.networkType)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

private fun usedPercent(used: Long, total: Long): Int {
    if (total <= 0) return 0
    return ((used.toDouble() / total.toDouble()) * 100).toInt()
}

private fun usedPercent(used: Double, total: Double): Int {
    if (total <= 0.0) return 0
    return ((used / total) * 100).toInt()
}

private fun usedRatio(used: Long, total: Long): Float {
    if (total <= 0) return 0f
    return (used.toFloat() / total.toFloat())
}

private fun usedRatio(used: Double, total: Double): Float {
    if (total <= 0.0) return 0f
    return (used / total).toFloat()
}
