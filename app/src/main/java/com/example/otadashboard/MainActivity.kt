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
import com.example.otadashboard.security.ClamAvEngine
import com.example.otadashboard.security.ScanLogDao
import com.example.otadashboard.security.ScanLogEntity
import com.example.otadashboard.security.SecurityChecker
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
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
                    color = MaterialTheme.colorScheme.surfaceVariant
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
fun OtaTheme(content: @Composable () -> Unit) {
    val darkColors = darkColorScheme(
        primary = Color(0xFF0A84FF),
        surface = Color(0xFF1C1C1E),
        surfaceVariant = Color(0xFF2C2C2E),
        background = Color(0xFF0F0F11),
        onPrimary = Color.White,
        onSurface = Color(0xFFF2F2F7),
        onSurfaceVariant = Color(0xFF8E8E93)
    )

    MaterialTheme(
        colorScheme = darkColors,
        typography = Typography(
            displayLarge = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
            displayMedium = MaterialTheme.typography.displayMedium.copy(fontSize = 18.sp),
            displaySmall = MaterialTheme.typography.displaySmall.copy(fontSize = 16.sp),
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontSize = 16.sp),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontSize = 14.sp),
            headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontSize = 12.sp),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
            titleMedium = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp),
            titleSmall = MaterialTheme.typography.titleSmall.copy(fontSize = 10.sp),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            bodySmall = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            labelLarge = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            labelMedium = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
            labelSmall = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
        ),
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
    var securityScore by remember { mutableIntStateOf(100) }

    val logs by logDao.getAllLogs().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        IosSegmentedControl(
            items = listOf("Güvenlik & Loglar", "OTA Güncelleme"),
            selectedIndex = selectedTab,
            onSegmentSelected = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = spring(stiffness = 200f)) togetherWith fadeOut(animationSpec = spring(stiffness = 200f))
            },
            label = "TabTransition"
        ) { targetTab ->
            when (targetTab) {
                0 -> VirusScanScreen(
                    isBusy = isBusy,
                    score = securityScore,
                    logs = logs,
                    onScanStart = {
                        isBusy = true
                        coroutineScope.launch(Dispatchers.IO) {
                            // 1. Sistem güvenlik denetimi
                            SecurityChecker.scanDeviceAndLog(context, logDao)
                            
                            // 2. ClamAV veritabanını internetten güncelle
                            downloadClamAvDb(context, logDao)
                            
                            // 3. Gerçek ClamAV Hash taraması çalıştır
                            val threatCount = executeRealClamAvScan(context, logDao)
                            
                            // 4. Güvenlik skorunu hesapla
                            withContext(Dispatchers.Main) {
                                securityScore = if (threatCount > 0) maxOf(0, 100 - (threatCount * 30)) else 100
                                isBusy = false
                            }
                        }
                    },
                    onClearLogs = {
                        coroutineScope.launch(Dispatchers.IO) {
                            logDao.clearLogs()
                            withContext(Dispatchers.Main) { securityScore = 100 }
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSegmentSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * ONLINE CLAMAV İMZA VERİTABANI İNDİRİCİ
 */
private suspend fun downloadClamAvDb(context: Context, logDao: ScanLogDao) = withContext(Dispatchers.IO) {
    val dbFile = File(context.filesDir, "clamav_db.txt")
    val dbUrl = "https://raw.githubusercontent.com/MaintainTeam/HypatiaDatabases/main/daily.hsb"

    try {
        logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "INFO", message = "Güncel ClamAV veritabanı indiriliyor..."))
        val url = URL(dbUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 12000
        connection.readTimeout = 12000

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream.use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "INFO", message = "Güncel imza veritabanı cihaza kaydedildi."))
        }
    } catch (e: Exception) {
        logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "WARN", message = "Veritabanı indirilemedi, yerel dosya kullanılacak: ${e.localizedMessage}"))
    }
}

/**
 * GERÇEK CLAMAV MOTORU ÇAĞRISI
 */
private suspend fun executeRealClamAvScan(context: Context, logDao: ScanLogDao): Int {
    logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "INFO", message = "ClamAV Engine (GPLv3) başlatılıyor..."))

    val engine = ClamAvEngine()
    val localDbFile = File(context.filesDir, "clamav_db.txt")

    val inputStream: java.io.InputStream = if (localDbFile.exists()) {
        logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "INFO", message = "Cihazdaki yerel veritabanı yükleniyor."))
        java.io.FileInputStream(localDbFile)
    } else {
        logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "WARN", message = "Yerel veritabanı yok, gömülü assets/clamav_db.txt yükleniyor."))
        try {
            context.assets.open("clamav_db.txt")
        } catch (e: Exception) {
            logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "ERROR", message = "Assets veritabanı bulunamadı!"))
            return 0
        }
    }

    val results = engine.scanInstalledApps(context, inputStream) { level, message ->
        logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = level, message = message))
    }

    val threats = results.count { it.isMalicious }
    if (threats == 0) {
        logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "INFO", message = "Tarama Tamamlandı: Sistem Temiz!"))
    } else {
        logDao.insertLog(ScanLogEntity(tag = "CLAMAV", level = "CRITICAL", message = "KRİTİK UYARI: $threats Adet Tehdit Tespit Edildi!"))
    }

    return threats
}

@Composable
fun VirusScanScreen(
    isBusy: Boolean,
    score: Int,
    logs: List<ScanLogEntity>,
    onScanStart: () -> Unit,
    onClearLogs: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Dynamic Security Score Panel
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Güvenlik Skoru",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isBusy) "Analiz Ediliyor..." else if (score == 100) "%100 Korumalı" else "%$score Tehdit Riski!",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isBusy) Color(0xFFFF9F0A) else if (score < 100) Color(0xFFFF453A) else Color(0xFF30D158)
                        )
                    }

                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = if (isBusy) 0.5f else (score / 100f),
                            modifier = Modifier.size(42.dp),
                            color = if (isBusy) Color(0xFFFF9F0A) else if (score < 100) Color(0xFFFF453A) else Color(0xFF30D158),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        if (!isBusy) {
                            Text(text = "$score", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
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
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Temizle", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Terminal Log Console
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF000000),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${logs.size} Kayıt",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                if (logs.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Oturum kaydı yok. Taramayı çalıştırın.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sistem Durumu",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

        updateInfoState?.let { info ->
            if (info.hasUpdate) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "YENİLİKLER VE DEĞİŞİKLİKLER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = info.changelog,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        if (isDownloading) {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = progressPercent / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Color(0xFF0A84FF),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "%$progressPercent", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(text = progressDetail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (updateInfoState?.hasUpdate == true) {
            Button(
                onClick = {
                    val info = updateInfoState ?: return@Button

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
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
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
