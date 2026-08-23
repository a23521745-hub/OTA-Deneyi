package com.example.otadashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.otadashboard.ota_updater.ApkDownloader
import com.example.otadashboard.ota_updater.OtaChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val updateJsonUrl =
        "https://raw.githubusercontent.com/a23521745-hub/OTA-Deneyi/refs/heads/main/update.json"

    // Kendi gerçek RSA-4096 public key'ini buraya yapıştır (PEM formatında, X.509/SubjectPublicKeyInfo)
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
                var statusText by remember { mutableStateOf("Sistem Hazır") }
                var isBusy by remember { mutableStateOf(false) }

                // Compose tabanlı çoklu izin isteme başlatıcısı
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    // İzinler istendikten sonra güncelleme kontrolünü başlat
                    isBusy = true
                    statusText = "Denetleniyor..."
                    performUpdateCheck(
                        onStatusChange = { statusText = it },
                        onBusyChange = { isBusy = it }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardContent(
                        status = statusText,
                        isBusy = isBusy,
                        onCheckUpdate = {
                            val permissionsToRequest = mutableListOf<String>()

                            // Android 13 ve üzeri için bildirim izni kontrolü
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }

                            // Android 10 ve altı için depolama izni kontrolü
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            }

                            // Eksik izin varsa iste, yoksa doğrudan denetimi başlat
                            if (permissionsToRequest.isNotEmpty()) {
                                permissionLauncher.launch(permissionsToRequest.toTypedArray())
                            } else {
                                isBusy = true
                                statusText = "Denetleniyor..."
                                performUpdateCheck(
                                    onStatusChange = { statusText = it },
                                    onBusyChange = { isBusy = it }
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    private fun performUpdateCheck(
        onStatusChange: (String) -> Unit,
        onBusyChange: (Boolean) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val updateInfo = OtaChecker.checkForUpdate(this@MainActivity, updateJsonUrl)

            withContext(Dispatchers.Main) {
                when {
                    updateInfo == null -> {
                        onStatusChange("Hata: Sunucuya ulaşılamadı veya manifest okunamadı.")
                        onBusyChange(false)
                    }
                    !updateInfo.hasUpdate -> {
                        onStatusChange("Cihazınız güncel.")
                        onBusyChange(false)
                    }
                    else -> {
                        onStatusChange("Güncelleme bulundu, indiriliyor ve doğrulanıyor...")
                        ApkDownloader.downloadAndVerifyApk(
                            context = this@MainActivity,
                            apkUrl = updateInfo.apkUrl,
                            expectedSignatureBase64 = updateInfo.signature,
                            publicKeyPem = publicKeyPem,
                            onResult = { success, message ->
                                onStatusChange(message)
                                onBusyChange(false)
                                if (!success) {
                                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    status: String,
    isBusy: Boolean,
    onCheckUpdate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "OTA Servis Paneli", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Durum: $status", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onCheckUpdate,
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
