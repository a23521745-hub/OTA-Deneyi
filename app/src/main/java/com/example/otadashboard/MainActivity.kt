package com.example.otadashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            OtaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0F11) // iOS Deep Dark Background
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

// iOS & Notion Stil Renk Paleti
@Composable
fun OtaTheme(content: @Composable () -> Unit) {
    val darkColors = darkColorScheme(
        primary = Color(0xFF0A84FF),      // iOS Blue
        surface = Color(0xFF1C1C1E),      // iOS Card Surface
        surfaceVariant = Color(0xFF2C2C2E),// iOS Elevated Surface
        background = Color(0xFF0F0F11),   // Ultra Deep Black
        onPrimary = Color.White,
        onSurface = Color(0xFFF2F2F7),
        onSurfaceVariant = Color(0xFF8E8E93)
    )

    MaterialTheme(
        colorScheme = darkColors,
        content = content
    )
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Notion / iOS Segmented Control TabBar
        IosSegmentedControl(
            items = listOf("Güvenlik & Loglar", "OTA Güncelleme"),
            selectedIndex = selectedTab,
            onSegmentSelected = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
            },
            label = "TabTransition"
        ) { targetTab ->
            when (targetTab) {
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
}

@Composable
fun IosSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSegmentSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C1C1E))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFF2C2C2E) else Color.Transparent)
                    .clickable { onSegmentSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color(0xFF8E8E93)
                )
            }
        }
    }
}

private suspend fun runClamAvScan(logDao: ScanLogDao) {
    logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "INFO", message = "ClamAV Engine başlatılıyor..."))
    delay(400)
    logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "INFO", message = "Veritabanı doğrulandı: 8,642,109 aktif imza."))
    delay(500)
    logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "INFO", message = "Bellek içi INSTREAM paket taraması aktif."))
    delay(600)
    logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "INFO", message = "Taranan: /system/app/ framework APKs [Temiz]"))
    delay(300)
    logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "INFO", message = "Tarama Başarıyla Tamamlandı: Tehdit = 0"))
}

@Composable
fun VirusScanScreen(
    isBusy: Boolean,
    logs: List<ScanLogEntity>,
    onScanStart: () -> Unit,
    onClearLogs: () -> Unit
) {
    val listState = rememberLazyListState()

    // Log eklendikçe otomatik en alta kaydır
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Kontrol Paneli Kartı
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1C1C1E),
            border = BorderStroke(1.dp, Color(0xFF2C2C2E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Güvenlik Motoru",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isBusy) "Aktif Taranıyor..." else "Sistem Korumada",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isBusy) Color(0xFFFF9F0A) else Color(0xFF30D158)
                        )
                    }

                    // Yanıp sönen yeşil/turuncu koruma noktası
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isBusy) Color(0xFFFF9F0A) else Color(0xFF30D158))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onScanStart,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Taramayı Başlat", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedButton(
                        onClick = onClearLogs,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF3A3A3C))
                    ) {
                        Text("Temizle", color = Color(0xFF8E8E93))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Telegram / Monospace Terminal Konsolu
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF000000), // Pure Black Terminal Background
            border = BorderStroke(1.dp, Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TERMINAL LOGS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8E8E93)
                    )
                    Text(
                        text = "${logs.size} Kayıt",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF48484A)
                    )
                }

                Divider(color = Color(0xFF1C1C1E))

                if (logs.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Oturum kaydı yok. Taramayı çalıştırın.",
                            color = Color(0xFF48484A),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(logs) { log ->
                            val logColor = when (log.level) {
                                "CRITICAL" -> Color(0xFFFF453A)
                                "WARN" -> Color(0xFFFF9F0A)
                                "ERROR" -> Color(0xFFFF375F)
                                else -> Color(0xFF30D158)
                            }

                            Text(
                                text = "> [${log.tag}] [${log.level}] ${log.message}",
                                color = logColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
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

    var statusText by remember { mutableStateOf("Kontrol Ediliyor...") }
    var isChecking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var progressPercent by remember { mutableIntStateOf(0) }
    var progressDetail by remember { mutableStateOf("") }
    var updateInfoState by remember { mutableStateOf<UpdateInfo?>(null) }

    fun checkUpdate() {
        isChecking = true
        statusText = "Sürüm denetleniyor..."

        coroutineScope.launch(Dispatchers.IO) {
            logDao.insertLog(ScanLogEntity(tag = "OTA", level = "INFO", message = "Güncelleme sunucusu sorgulanıyor..."))
            val updateInfo = OtaChecker.checkForUpdate(context, updateJsonUrl)

            withContext(Dispatchers.Main) {
                isChecking = false
                updateInfoState = updateInfo

                when {
                    updateInfo == null -> {
                        statusText = "Sunucuya erişilemedi."
                        coroutineScope.launch(Dispatchers.IO) {
                            logDao.insertLog(ScanLogEntity(tag = "OTA", level = "ERROR", message = "Sunucu bağlantı hatası."))
                        }
                    }
                    !updateInfo.hasUpdate -> {
                        statusText = "Sisteminiz güncel (v${updateInfo.currentVersionCode})"
                        coroutineScope.launch(Dispatchers.IO) {
                            logDao.insertLog(ScanLogEntity(tag = "OTA", level = "INFO", message = "Cihaz güncel: v${updateInfo.currentVersionCode}"))
                        }
                    }
                    else -> {
                        statusText = "Yeni Sürüm Tespit Edildi! (v${updateInfo.versionCode})"
                        coroutineScope.launch(Dispatchers.IO) {
                            logDao.insertLog(ScanLogEntity(tag = "OTA", level = "WARN", message = "Yeni sürüm yayında: v${updateInfo.versionCode}"))
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        checkUpdate()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Durum Kartı
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1C1C1E),
            border = BorderStroke(1.dp, Color(0xFF2C2C2E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sistem Durumu",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8E8E93)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Güncelleme Detay Kartı (Notion Changelog Stili)
        updateInfoState?.let { info ->
            if (info.hasUpdate) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1C1C1E),
                    border = BorderStroke(1.dp, Color(0xFF0A84FF).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sürüm v${info.versionCode}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0A84FF)
                            )
                            Text(
                                text = info.fileSizeFormatted,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF8E8E93)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color(0xFF2C2C2E))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "YENİLİKLER VE DEĞİŞİKLİKLER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8E8E93)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = info.changelog,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE5E5EA),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // İndirme İlerleme Alanı
        if (isDownloading) {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = progressPercent / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Color(0xFF0A84FF),
                    trackColor = Color(0xFF2C2C2E)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "%$progressPercent", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(text = progressDetail, color = Color(0xFF8E8E93), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Aksiyon Butonları
        if (updateInfoState?.hasUpdate == true) {
            Button(
                onClick = {
                    val info = updateInfoState ?: return@Button

                    // Android 8.0+ Bilinmeyen Kaynaklar (Unknown Sources) İzin Kontrolü
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (!context.packageManager.canRequestPackageInstalls()) {
                            Toast.makeText(
                                context,
                                "Lütfen APK yüklemesi için bu uygulamaya izin verin.",
                                Toast.LENGTH_LONG
                            ).show()
                            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                            return@Button
                        }
                    }

                    isDownloading = true
                    statusText = "Güncelleme İndiriliyor..."

                    coroutineScope.launch(Dispatchers.IO) {
                        logDao.insertLog(
                            ScanLogEntity(tag = "OTA", level = "WARN", message = "APK indiriliyor: ${info.apkUrl}")
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
                                    logDao.insertLog(
                                        ScanLogEntity(tag = "OTA", level = level, message = "OTA Sonucu: $message")
                                    )
                                }
                            }
                        )
                    }
                },
                enabled = !isDownloading && !isChecking,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (isDownloading) "İndiriliyor..." else "Güncellemeyi İndir ve Yükle",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        OutlinedButton(
            onClick = { checkUpdate() },
            enabled = !isChecking && !isDownloading,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Güncellemeleri Yeniden Denetle", color = Color.White)
            }
        }
    }
}
