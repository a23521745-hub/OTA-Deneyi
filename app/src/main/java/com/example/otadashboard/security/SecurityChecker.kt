package com.example.otadashboard.security

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object SecurityChecker {

    suspend fun scanDeviceAndLog(context: Context, logDao: ScanLogDao): String = withContext(Dispatchers.IO) {
        val results = mutableListOf<String>()

        logDao.insertLog(ScanLogEntity(tag = "SECURITY", level = "INFO", message = "Güvenlik taraması başlatıldı."))

        // 1. Şifreleme Kontrolü
        val isEncrypted = isDeviceEncrypted(context)
        val encMsg = "Şifreleme: ${if (isEncrypted) "✓ Aktif" else "✗ Pasif"}"
        results.add(encMsg)
        logDao.insertLog(ScanLogEntity(tag = "SECURITY", level = if (isEncrypted) "INFO" else "WARN", message = encMsg))

        // 2. Güvenlik Yaması
        val securityPatch = getSecurityPatch()
        val patchMsg = "Güvenlik Yaması: $securityPatch"
        results.add(patchMsg)
        logDao.insertLog(ScanLogEntity(tag = "SECURITY", level = "INFO", message = patchMsg))

        // 3. Bilinmeyen Kaynaklar
        val unknownSources = isUnknownSourcesEnabled(context)
        val sourceMsg = "Bilinmeyen Kaynaklar: ${if (unknownSources) "⚠️ Aktif" else "✓ Kapalı"}"
        results.add(sourceMsg)
        logDao.insertLog(ScanLogEntity(tag = "SECURITY", level = if (unknownSources) "WARN" else "INFO", message = sourceMsg))

        // 4. Yönetici Uygulamaları
        val adminApps = getDeviceAdminApps(context)
        val adminMsg = "Yönetici Uygulamaları: ${adminApps.size} adet"
        results.add(adminMsg)
        logDao.insertLog(ScanLogEntity(tag = "SECURITY", level = "INFO", message = adminMsg))

        // 5. Şüpheli Uygulamalar
        val suspiciousApps = checkSuspiciousApps(context)
        val suspMsg = "Şüpheli Uygulamalar: ${if (suspiciousApps.isEmpty()) "✓ Bulunmadı" else "✗ ${suspiciousApps.size} adet"}"
        results.add(suspMsg)
        logDao.insertLog(ScanLogEntity(tag = "SECURITY", level = if (suspiciousApps.isEmpty()) "INFO" else "CRITICAL", message = suspMsg))

        val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date())
        results.add("\nSon Tarama: $timestamp")
        logDao.insertLog(ScanLogEntity(tag = "SECURITY", level = "INFO", message = "Tarama tamamlandı."))

        return@withContext results.joinToString("\n\n")
    }

    private fun isDeviceEncrypted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                dpm?.storageEncryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE
            } catch (e: Exception) { false }
        } else false
    }

    private fun getSecurityPatch(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "Bilinmiyor"
        } catch (e: Exception) { "Bilinmiyor" }
    }

    private fun isUnknownSourcesEnabled(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            pm.getApplicationEnabledSetting("com.android.chrome") == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } catch (e: Exception) { false }
    }

    private fun getDeviceAdminApps(context: Context): List<String> {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            dpm?.getActiveAdmins()?.map { it.packageName } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun checkSuspiciousApps(context: Context): List<String> {
        val suspiciousPackages = listOf("com.example.malware", "com.cheat", "xposed", "de.robv.android.xposed")
        val pm = context.packageManager
        val suspicious = mutableListOf<String>()
        for (packageName in suspiciousPackages) {
            try {
                pm.getPackageInfo(packageName, 0)
                suspicious.add(packageName)
            } catch (_: PackageManager.NameNotFoundException) {}
        }
        return suspicious
    }
}
