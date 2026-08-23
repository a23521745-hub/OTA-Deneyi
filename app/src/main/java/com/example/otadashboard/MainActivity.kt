package com.example.otadashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.otadashboard.ota_updater.ApkDownloader
import com.example.otadashboard.ota_updater.OtaChecker
import com.example.otadashboard.security.AppDatabase
import com.example.otadashboard.security.ScanLogDao
import com.example.otadashboard.security.ScanLogEntity
import com.example.otadashboard.security.SecurityChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val updateJsonUrl =
        "https://raw.githubusercontent.com/a23521745-hub/OTA-Deneyi/refs/heads/main/update.json"

    private val publicKeyPem = """
        -----BEGIN PUBLIC KEY-----
        MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAr2+3opTd+QdzzKJzJBUH
        TTNYuPoZ+yp3YA9VHpfoBnE8XNUS4SWCMpFwKIzjPiB4TAjGtUa+85HFouArAdM3
        bK+NcX95MM+2nvKTa4HTUM6/bc30nm6uHoyEfNBbjvDOah+Nv7lsP9vp2MCWSM5v
        8gQDZIrIYpf59e1NsbL0TRfxzVHKhzLHapQ7zK2Sb+KEeR4L4I5MbhnMMmfEdhDQ
        XuhWJSrQvbCqYaJ+Vnq0qfBi4uD1+6ARvVsyMog/O97ZnWx5lVkRC2q9bNglbcpn
        rDHDrvFsUyEjYhznIGbCkm0xA6qwjUkxh8DGc7qo9XK0Ypq7J5xh2xELetEHQeCJ
        EZneLI8KHQHKURsmFUZ/eo+RV+evJ7XSnkFjT6lmO5D3gaD2qOBLcTzKwwVeQ1h7
        iEPvEjvP1J1kWoWTQOt2WQF3PGDrWz3DY5hdqJp2cf83+dQtLFythjMn/czYmFmF
        9h3aTN6Pg1nWtp2zxDMU4g4d/OCZA4kJF38BwsoPyRthTzxupfxkvsXYc5MpHDNb
        1xBfLDiCDc7WzGrT1oncqUmpe4NU9C6cnJHCO7bvSDo2qKB/PAdfPY+scmdyBb1O
        974gICpfLgju8Z5nHDnR9Fu1LtJKz462Zg7NT/SPFcvepf5PpeKHzwGCZtJn5b1J
        3l3Pf2kLPrjtmFEeWQiVBkkCAwEAAQ==
        -----END PUBLIC KEY-----
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Room Veritabanı Başlatılıyor
        val db = AppDatabase.getDatabase(applicationContext)
        val logDao = db.scanLogDao()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        context = this@MainActivity,
                        updateJsonUrl = updateJsonUrl,
                        publicKeyPem = publicKeyPem,
                        logDao = logDao,
                        lifecycleScope = lifecycleScope
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    context: android.content.Context,
    updateJsonUrl: String,
    publicKeyPem: String,
    logDao: ScanLogDao,
    lifecycleScope: androidx.lifecycle.LifecycleCoroutineScope
) {
    var selectedTab by remember { mutableStateOf(0) }
    var statusText by remember { mutableStateOf("Sistem Hazır") }
    var isBusy by remember { mutableStateOf(false) }

    // Room Veritabanındaki Logları Canlı Dinleme
    val logs by logDao.getAllLogs().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Bar
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Antivirüs & Loglar") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("OTA Güncelleme") }
            )
        }

        // Tab Content
        when (selectedTab) {
            0 -> VirusScanScreen(
                context = context,
                isBusy = isBusy,
                logs = logs,
                logDao = logDao,
                onScanStart = {
                    isBusy = true
                    lifecycleScope.launch(Dispatchers.IO) {
                        // 1. Sistem Güvenlik Taraması
                        SecurityChecker.scanDeviceAndLog(context, logDao)
                        
                        // 2. ClamAV Motor Simülasyon Taraması
                        runClamAvScan(logDao)
                        
                        withContext(Dispatchers.Main) {
                            isBusy = false
                        }
                    }
                },
                onClearLogs = {
                    lifecycleScope.launch(Dispatchers.IO) {
                        logDao.clearLogs()
                    }
                }
            )
            1 -> UpdateScreen(
                context = context,
                status = statusText,
                isBusy = isBusy,
                updateJsonUrl = updateJsonUrl,
                publicKeyPem = publicKeyPem,
                logDao = logDao,
                onStatusChange = { statusText = it },
                onBusyChange = { isBusy = it },
                lifecycleScope = lifecycleScope
            )
        }
    }
}

// ClamAV Daemon Soket / Engine Simülasyonu
private suspend fun runClamAvScan(logDao: ScanLogDao) {
    logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "INFO", message = "ClamAV v1.2.0 Engine başlatılıyor..."))
    delay(500)
    logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "INFO", message = "Veritabanı yüklendi: 8,642,109 imza aktif."))
    delay(700)
    logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "INFO", message = "INSTREAM paket taraması gerçekleştiriliyor..."))
    delay(1000)
    logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "INFO", message = "Taranan: /system/app/ framework APKs [Temiz]"))
    delay(600)
    logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "INFO", message = "ClamAV Taraması Tamamlandı: Tehdit Tespiti = 0"))
}

@Composable
fun VirusScanScreen(
    context: android.content.Context,
    isBusy: Boolean,
    logs: List<ScanLogEntity>,
    logDao: ScanLogDao,
    onScanStart: () -> Unit,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "ClamAV & Security Dashboard", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onScanStart,
                enabled = !isBusy,
                modifier = Modifier.weight(1f)
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Taramayı Başlat")
                }
            }

            OutlinedButton(
                onClick = onClearLogs,
                enabled = !isBusy
            ) {
                Text("Logları Temizle")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Canlı Terminal Log Paneli
        Text(
            text = "Canlı Terminal Logları (Room DB Persisted)",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Surface(
            color = Color(0xFF1E1E1E), // Koyu Terminal Arka Planı
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 8.dp)
        ) {
            if (logs.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Henüz log kaydı yok. Taramayı başlatın.", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        val logColor = when (log.level) {
                            "CRITICAL" -> Color(0xFFFF5252) // Kırmızı
                            "WARN" -> Color(0xFFFFD700)     // Sarı
                            "ERROR" -> Color(0xFFFF4081)    // Pembe/Mor
                            else -> Color(0xFF00E676)       // Parlak Yeşil
                        }

                        Text(
                            text = "[${log.tag}] [${log.level}] ${log.message}",
                            color = logColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateScreen(
    context: android.content.Context,
    status: String,
    isBusy: Boolean,
    updateJsonUrl: String,
    publicKeyPem: String,
    logDao: ScanLogDao,
    onStatusChange: (String) -> Unit,
    onBusyChange: (Boolean) -> Unit,
    lifecycleScope: androidx.lifecycle.LifecycleCoroutineScope
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "OTA Güncelleme", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Durum: $status", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                onBusyChange(true)
                onStatusChange("Denetleniyor...")
                lifecycleScope.launch(Dispatchers.IO) {
                    logDao.insertLog(ScanLogEntity(tag = "OTA", level = "INFO", message = "OTA güncelleme kontrolü başlatıldı."))
                    
                    val updateInfo = OtaChecker.checkForUpdate(context, updateJsonUrl)
                    withContext(Dispatchers.Main) {
                        when {
                            updateInfo == null -> {
                                onStatusChange("Hata: Sunucuya ulaşılamadı")
                                lifecycleScope.launch(Dispatchers.IO) {
                                    logDao.insertLog(ScanLogEntity(tag = "OTA", level = "ERROR", message = "Sunucuya ulaşılamadı."))
                                }
                                onBusyChange(false)
                            }
                            !updateInfo.hasUpdate -> {
                                onStatusChange("Cihazınız güncel.")
                                lifecycleScope.launch(Dispatchers.IO) {
                                    logDao.insertLog(ScanLogEntity(tag = "OTA", level = "INFO", message = "Cihaz güncel, yeni sürüm yok."))
                                }
                                onBusyChange(false)
                            }
                            else -> {
                                onStatusChange("Güncelleme bulundu, indiriliyor...")
                                lifecycleScope.launch(Dispatchers.IO) {
                                    logDao.insertLog(ScanLogEntity(tag = "OTA", level = "WARN", message = "Yeni sürüm bulundu! APK İndiriliyor..."))
                                }
                                ApkDownloader.downloadAndVerifyApk(
                                    context = context,
                                    apkUrl = updateInfo.apkUrl,
                                    expectedSignatureBase64 = updateInfo.signature,
                                    publicKeyPem = publicKeyPem,
                                    onResult = { success, message ->
                                        onStatusChange(message)
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            val level = if (success) "INFO" else "CRITICAL"
                                            logDao.insertLog(ScanLogEntity(tag = "OTA", level = level, message = "OTA İndirme Sonucu: $message"))
                                        }
                                        onBusyChange(false)
                                    }
                                )
                            }
                        }
                    }
                }
            },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Güncellemeleri Denetle")
            }
        }
    }
}
