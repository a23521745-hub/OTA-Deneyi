package com.example.otadashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.otadashboard.security.AppDatabase
import com.example.otadashboard.ui.MainScreen

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
        enableEdgeToEdge()

        val logDao = AppDatabase.getDatabase(applicationContext).scanLogDao()

        setContent {
            OtaDashboardTheme {
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
fun OtaDashboardTheme(content: @Composable () -> Unit) {
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
        content = content
    )
}
