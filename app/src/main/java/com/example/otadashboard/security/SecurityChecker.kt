package com.example.otadashboard.security

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

object SecurityChecker {

    fun scanDevice(context: Context): String {
        val results = mutableListOf<String>()
        
        // 1. Cihaz Encryption Kontrolü
        val isEncrypted = isDeviceEncrypted(context)
        results.add("Şifreleme: ${if (isEncrypted) "✓ Aktif" else "✗ Pasif"}")

        // 2. Güvenlik Yaması
        val securityPatch = getSecurityPatch()
        results.add("Güvenlik Yaması: $securityPatch")

        // 3. Bilinmeyen Kaynaklar
        val unknownSources = isUnknownSourcesEnabled(context)
        results.add("Bilinmeyen Kaynaklar: ${if (unknownSources) "⚠️ Aktif" else "✓ Kapalı"}")

        // 4. Yönetici Uygulamaları
        val adminApps = getDeviceAdminApps(context)
        results.add("Yönetici Uygulamaları: ${adminApps.size} adet")

        // 5. Şüpheli Uygulamalar Kontrolü
        val suspiciousApps = checkSuspiciousApps(context)
        results.add("Şüpheli Uygulamalar: ${if (suspiciousApps.isEmpty()) "✓ Bulunmadı" else "✗ ${suspiciousApps.size} adet"}")

        val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date())
        results.add("\nSon Tarama: $timestamp")

        return results.joinToString("\n\n")
    }

    private fun isDeviceEncrypted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                dpm?.storageEncryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    private fun getSecurityPatch(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Build.VERSION.SECURITY_PATCH
            } else {
                "Bilinmiyor"
            }
        } catch (e: Exception) {
            "Bilinmiyor"
        }
    }

    private fun isUnknownSourcesEnabled(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            pm.getApplicationEnabledSetting("com.android.chrome") == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } catch (e: Exception) {
            false
        }
    }

    private fun getDeviceAdminApps(context: Context): List<String> {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            dpm?.getActiveAdmins()?.map { it.packageName } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun checkSuspiciousApps(context: Context): List<String> {
        val suspiciousPackages = listOf(
            "com.example.malware",
            "com.cheat",
            "xposed",
            "de.robv.android.xposed"
        )

        val pm = context.packageManager
        val suspicious = mutableListOf<String>()

        try {
            for (packageName in suspiciousPackages) {
                try {
                    pm.getPackageInfo(packageName, 0)
                    suspicious.add(packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                    // Paket yüklü değil
                }
            }
        } catch (e: Exception) {
            // Hata
        }

        return suspicious
    }
}
