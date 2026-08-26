package com.example.otadashboard.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.otadashboard.MainActivity

class OtaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            OtaWidgetContent()
        }
    }

    @Composable
    private fun OtaWidgetContent() {
        // iOS Koyu Tema Renk Paleti
        val cardBackground = Color(0xFF1C1C1E)
        val primaryBlue = Color(0xFF0A84FF)
        val successGreen = Color(0xFF30D158)
        val textSecondary = ColorProvider(Color(0xFF8E8E93))
        val textPrimary = ColorProvider(Color(0xFFFFFFFF))

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(cardBackground)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header Row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OTA DASHBOARD",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    // Aktif yeşil durum noktası
                    Box(
                        modifier = GlanceModifier
                            .size(8.dp)
                            .background(successGreen)
                    ) {}
                }

                Spacer(modifier = GlanceModifier.height(12.dp))

                // Status Content
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "%100 Korumalı",
                            style = TextStyle(
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "Sistem Güncel • v1.0.0",
                            style = TextStyle(
                                color = textSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }

                    // Skor Rozeti
                    Box(
                        modifier = GlanceModifier
                            .background(primaryBlue)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "100",
                            style = TextStyle(
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

class OtaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OtaWidget()
}
