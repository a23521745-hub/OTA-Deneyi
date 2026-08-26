package com.example.otadashboard.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ClamScanResult(
    val appName: String,
    val packageName: String,
    val isMalicious: Boolean,
    val virusName: String? = null,
    val hash: String = ""
)

class ClamAvEngine {

    /**
     * APK Dosyasının SHA-256 Hash'ini hesaplar
     */
    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Cihazdaki üçüncü taraf APK'ları ClamAV veritabanı ile karşılaştırır
     */
    suspend fun scanInstalledApps(
        context: Context,
        dbInputStream: InputStream,
        onLog: suspend (level: String, message: String) -> Unit
    ): List<ClamScanResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ClamScanResult>()

        // 1. Imza veritabanını belleğe yükle (Format -> HASH:SIZE:VIRUS_NAME)
        onLog("INFO", "ClamAV veritabanı belleğe yükleniyor...")
        val signatureMap = mutableMapOf<String, String>()
        
        dbInputStream.bufferedReader().useLines { lines ->
            for (line in lines) {
                if (line.isBlank() || line.startsWith("#")) continue
                val parts = line.split(":")
                if (parts.isNotEmpty()) {
                    val hash = parts[0].trim().lowercase()
                    val virusName = if (parts.size >= 3) parts[2].trim() else "Android.Threat.ClamAV"
                    signatureMap[hash] = virusName
                }
            }
        }
        onLog("INFO", "Veritabanı yüklendi. Aktif imza sayısı: ${signatureMap.size}")

        // 2. Yüklü kullanıcı uygulamalarını tespit et (Sistem uygulamaları hariç)
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }

        onLog("INFO", "Toplam ${installedApps.size} adet üçüncü taraf APK taranacak.")

        // 3. Taramayı başlat
        installedApps.forEachIndexed { index, appInfo ->
            val appName = pm.getApplicationLabel(appInfo).toString()
            val apkFile = File(appInfo.sourceDir)

            if (apkFile.exists()) {
                val sha256 = calculateSha256(apkFile).lowercase()
                val isThreat = signatureMap.containsKey(sha256)
                val threatName = signatureMap[sha256]

                if (isThreat) {
                    onLog("CRITICAL", "TEHDİT BULUNDU! Uygulama: $appName | Zararlı: $threatName")
                    results.add(ClamScanResult(appName, appInfo.packageName, true, threatName, sha256))
                } else {
                    onLog("INFO", "Taranıyor (${index + 1}/${installedApps.size}): $appName -> [TEMİZ]")
                    results.add(ClamScanResult(appName, appInfo.packageName, false, null, sha256))
                }
            }
        }

        return@withContext results
    }
}
