package com.ali.otadeneyi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                OtaScreen()
            }
        }
    }
}

@Composable
fun OtaScreen() {

    var currentVersion by remember {
        mutableStateOf("1.0")
    }

    var updateAvailable by remember {
        mutableStateOf(false)
    }

    var statusText by remember {
        mutableStateOf("GÜNCELLEME YOK")
    }

    val squareColor =
        if (currentVersion == "2.0")
            Color(0xFF2196F3)
        else
            Color(0xFF4CAF50)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "OTA DENeyi",
            fontSize = 26.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "OTA Durumu: $statusText",
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(60.dp))

        Box(
            modifier = Modifier
                .size(180.dp)
                .background(
                    color = squareColor,
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "v$currentVersion",
                color = Color.White,
                fontSize = 32.sp
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Yüklü sürüm: v$currentVersion",
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        if (updateAvailable) {

            Button(
                onClick = {

                    // Şimdilik OTA simülasyonu.
                    currentVersion = "2.0"
                    updateAvailable = false
                    statusText = "GÜNCELLEME YOK"
                }
            ) {
                Text("UYGULAMAYI GÜNCELLE")
            }

        } else {

            Button(
                onClick = {

                    // Şimdilik test amacıyla
                    // sunucuda v2.0 bulunduğunu varsayıyoruz.
                    updateAvailable = true
                    statusText = "GÜNCELLEME VAR"
                }
            ) {
                Text("GÜNCELLEME KONTROL ET")
            }
        }
    }
}
