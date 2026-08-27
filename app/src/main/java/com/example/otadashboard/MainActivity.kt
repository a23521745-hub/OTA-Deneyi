package com.example.otadashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.otadashboard.security.AppDatabase
import com.example.otadashboard.ui.MainScreen
import com.example.otadashboard.ui.theme.OtaDashboardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge ekran tasarımı
        enableEdgeToEdge()

        // Veritabanı ve parametreler
        val db = AppDatabase.getDatabase(applicationContext)
        val logDao = db.scanLogDao()
        val updateJsonUrl = "https://example.com/update.json"
        val publicKeyPem = "        -----BEGIN PUBLIC KEY-----
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
        -----END PUBLIC KEY-----"

        setContent {
            OtaDashboardTheme {
                MainScreen(
                    updateJsonUrl = updateJsonUrl,
                    publicKeyPem = publicKeyPem,
                    logDao = logDao
                )
            }
        }
    }
}
