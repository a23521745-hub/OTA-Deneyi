package com.example.otadashboard

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.otadashboard.ota_updater.ApkDownloader
import com.example.otadashboard.ota_updater.OtaChecker
import com.example.otadashboard.security.SecurityChecker
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
    lifecycleScope: androidx.lifecycle.LifecycleCoroutineScope
) {
    var selectedTab by remember { mutableStateOf(0) }
    var statusText by remember { mutableStateOf("Sistem Hazır") }
    var isBusy by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Bar
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Virüs Taraması") }
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
                scanResult = scanResult,
                onScanStart = {
                    isBusy = true
                    scanResult = null
                    lifecycleScope.launch(Dispatchers.Default) {
                        val result = SecurityChecker.scanDevice(context)
                        withContext(Dispatchers.Main) {
                            scanResult = result
                            isBusy = false
                        }
                    }
                }
            )
            1 -> UpdateScreen(
                context = context,
                status = statusText,
                isBusy = isBusy,
                updateJsonUrl = updateJsonUrl,
                publicKeyPem = publicKeyPem,
                onStatusChange = { statusText = it },
                onBusyChange = { isBusy = it },
                lifecycleScope = lifecycleScope
            )
        }
    }
}

@Composable
fun VirusScanScreen(
    context: android.content.Context,
    isBusy: Boolean,
    scanResult: String?,
    onScanStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Virüs Taraması", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        if (isBusy) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Tarama yapılıyor...")
        } else if (scanResult != null) {
            Text(scanResult, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Text("Tarama başlatmak için butona basın", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(32.dp))
        }

        Button(
            onClick = onScanStart,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tarama Başlat")
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
                    val updateInfo = OtaChecker.checkForUpdate(context, updateJsonUrl)
                    withContext(Dispatchers.Main) {
                        when {
                            updateInfo == null -> {
                                onStatusChange("Hata: Sunucuya ulaşılamadı")
                                onBusyChange(false)
                            }
                            !updateInfo.hasUpdate -> {
                                onStatusChange("Cihazınız güncel.")
                                onBusyChange(false)
                            }
                            else -> {
                                onStatusChange("Güncelleme bulundu, indiriliyor...")
                                ApkDownloader.downloadAndVerifyApk(
                                    context = context,
                                    apkUrl = updateInfo.apkUrl,
                                    expectedSignatureBase64 = updateInfo.signature,
                                    publicKeyPem = publicKeyPem,
                                    onResult = { success, message ->
                                        onStatusChange(message)
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
