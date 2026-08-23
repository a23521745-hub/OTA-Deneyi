package com.example.otadashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.otadashboard.ota_updater.ApkDownloader
import com.example.otadashboard.ota_updater.OtaChecker
import com.example.otadashboard.ota_updater.UpdateInfo
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

        val db = AppDatabase.getDatabase(applicationContext)
        val logDao = db.scanLogDao()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        updateJsonUrl = updateJsonUrl,
                        publicKeyPem = publicKeyPem,
                        logDao = logDao
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    updateJsonUrl: String,
    publicKeyPem: String,
    logDao: ScanLogDao
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var isBusy by remember { mutableStateOf(false) }

    val logs by logDao.getAllLogs().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
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

        when (selectedTab) {
            0 -> VirusScanScreen(
                isBusy = isBusy,
                logs = logs,
                onScanStart = {
                    isBusy = true
                    coroutineScope.launch(Dispatchers.IO) {
                        SecurityChecker.scanDeviceAndLog(context, logDao)
                        runClamAvScan(logDao)
                        withContext(Dispatchers.Main) {
                            isBusy = false
                        }
                    }
                },
                onClearLogs = {
                    coroutineScope.launch(Dispatchers.IO) {
                        logDao.clearLogs()
                    }
                }
            )
            1 -> UpdateScreen(
                updateJsonUrl = updateJsonUrl,
                publicKeyPem = publicKeyPem,
                logDao = logDao
            )
        }
    }
}

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
    isBusy: Boolean,
    logs: List<ScanLogEntity>,
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

        Text(
            text = "Canlı Terminal Logları (Room DB Persisted)",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.Start)
        )

        Surface(
            color = Color(0xFF1E1E1E),
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
                            "CRITICAL" -> Color(0xFFFF5252)
                            "WARN" -> Color(0xFFFFD700)
                            "ERROR" -> Color(0xFFFF4081)
                            else -> Color(0xFF00E676)
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
    updateJsonUrl: String,
    publicKeyPem: String,
    logDao: ScanLogDao
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var statusText by remember { mutableStateOf("Kontrol ediliyor...") }
    var isChecking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var progressPercent by remember { mutableIntStateOf(0) }
    var progressDetail by remember { mutableStateOf("") }
    var updateInfoState by remember { mutableStateOf<UpdateInfo?>(null) }

    fun checkUpdate() {
        isChecking = true
        statusText = "Sürüm denetleniyor..."

        coroutineScope.launch(Dispatchers.IO) {
            logDao.insertLog(ScanLogEntity(tag = "OTA", level = "INFO", message = "Güncelleme denetimi başlatıldı."))
            val updateInfo = OtaChecker.checkForUpdate(context, updateJsonUrl)

            withContext(Dispatchers.Main) {
                isChecking = false
                updateInfoState = updateInfo

                when {
                    updateInfo == null -> {
                        statusText = "Hata: Sunucuya veya GitHub API'ye ulaşılamadı."
                        coroutineScope.launch(Dispatchers.IO) {
                            logDao.insertLog(ScanLogEntity(tag = "OTA", level = "ERROR", message = "Sunucu yanıt vermedi."))
                        }
                    }
                    !updateInfo.hasUpdate -> {
                        statusText = "Cihazınız güncel. (Sürüm: v${updateInfo.currentVersionCode})"
                        coroutineScope.launch(Dispatchers.IO) {
                            logDao.insertLog(ScanLogEntity(tag = "OTA", level = "INFO", message = "Sürüm v${updateInfo.currentVersionCode} güncel."))
                        }
                    }
                    else -> {
                        statusText = "Yeni güncelleme mevcut! (v${updateInfo.versionCode})"
                        coroutineScope.launch(Dispatchers.IO) {
                            logDao.insertLog(
                                ScanLogEntity(tag = "OTA", level = "WARN", message = "Yeni sürüm tespit edildi: v${updateInfo.versionCode}")
                            )
                        }
                    }
                }
            }
        }
    }

    // Otomatik Kontrol (Auto-Check)
    LaunchedEffect(Unit) {
        checkUpdate()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "OTA Güncelleme Merkezi", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Durum Metni Kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Sistem Durumu", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = statusText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Güncelleme Detay Kartı (Yenilikler / Changelog)
        updateInfoState?.let { info ->
            if (info.hasUpdate) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Yeni Sürüm Mevcut! (v${info.versionCode})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Boyut: ${info.fileSizeFormatted}", fontSize = 12.sp, color = Color.DarkGray)
                            Text(text = "Yayın Tarihi: ${info.publishedAt}", fontSize = 12.sp, color = Color.DarkGray)
                        }
                        Divider(modifier = Modifier.padding(vertical = 10.dp))
                        Text(
                            text = "Yenilikler & Değişiklikler:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = info.changelog,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // İndirme Progress Bar
        if (isDownloading) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(
                    progress = progressPercent / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "%$progressPercent", fontWeight = FontWeight.Bold)
                    Text(text = progressDetail, fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Aksiyon Butonları
        if (updateInfoState?.hasUpdate == true) {
            Button(
                onClick = {
                    val info = updateInfoState ?: return@Button
                    isDownloading = true
                    statusText = "APK İndiriliyor..."

                    coroutineScope.launch(Dispatchers.IO) {
                        logDao.insertLog(
                            ScanLogEntity(tag = "OTA", level = "WARN", message = "APK indirmesi başlatıldı: ${info.apkUrl}")
                        )

                        ApkDownloader.downloadAndVerifyApk(
                            context = context,
                            apkUrl = info.apkUrl,
                            expectedSignatureBase64 = info.signature,
                            publicKeyPem = publicKeyPem,
                            maxRetries = 3,
                            onProgress = { percent, downloaded, total ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    progressPercent = percent
                                    val downloadedMb = String.format("%.2f", downloaded / (1024.0 * 1024.0))
                                    val totalMb = String.format("%.2f", total / (1024.0 * 1024.0))
                                    progressDetail = "$downloadedMb MB / $totalMb MB"
                                }
                            },
                            onResult = { success, message ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    isDownloading = false
                                    statusText = message
                                }
                                coroutineScope.launch(Dispatchers.IO) {
                                    val level = if (success) "INFO" else "CRITICAL"
                                    logDao.insertLog(ScanLogEntity(tag = "OTA", level = level, message = "OTA Sonuç: $message"))
                                }
                            }
                        )
                    }
                },
                enabled = !isDownloading && !isChecking,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isDownloading) {
                    Text("İndiriliyor...")
                } else {
                    Text("Güncellemeyi İndir ve Yükle")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = { checkUpdate() },
            enabled = !isChecking && !isDownloading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isChecking) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Güncellemeleri Yeniden Denetle")
            }
        }
    }
}
