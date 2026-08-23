package com.example.otadashboard.ota_updater

import android.content.Context
import android.content.pm.PackageManager
import org.json.JSONObject
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val currentVersionCode: Int,
    val apkUrl: String,
    val signature: String,
    val changelog: String,
    val fileSizeFormatted: String,
    val publishedAt: String,
    val hasUpdate: Boolean
)

object OtaChecker {

    fun checkForUpdate(context: Context, jsonUrl: String): UpdateInfo? {
        return try {
            // 1. update.json'ı çek
            val url = URL(jsonUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)

            val signature = json.optString("signature", "")
            val changelog = json.optString("changelog", "Değişiklik detayı belirtilmedi.")

            // 2. GitHub API'den latest release'i çek
            val apiUrl = "https://api.github.com/repos/a23521745-hub/OTA-Deneyi/releases/latest"
            val apiConnection = URL(apiUrl).openConnection()
            apiConnection.connectTimeout = 15000
            apiConnection.readTimeout = 15000
            val apiResponse = apiConnection.inputStream.bufferedReader().readText()
            val releaseJson = JSONObject(apiResponse)

            // Tag name'den version code çıkar (örn: v2 → 2)
            val tagName = releaseJson.optString("tag_name", "v1")
            val newVersionCode = tagName.filter { it.isDigit() }.toIntOrNull() ?: 1
            val publishedAt = releaseJson.optString("published_at", "Bilinmiyor").take(10)

            // Latest release'deki ilk .apk dosyasını ve boyutunu bul
            val assets = releaseJson.getJSONArray("assets")
            var apkDownloadUrl = ""
            var fileSizeBytes = 0L

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    apkDownloadUrl = asset.getString("browser_download_url")
                    fileSizeBytes = asset.optLong("size", 0L)
                    break
                }
            }

            if (apkDownloadUrl.isEmpty()) {
                return null
            }

            val currentVersionCode = getCurrentVersionCode(context)
            val hasUpdate = newVersionCode > currentVersionCode
            val fileSizeFormatted = formatFileSize(fileSizeBytes)

            UpdateInfo(
                versionCode = newVersionCode,
                currentVersionCode = currentVersionCode,
                apkUrl = apkDownloadUrl,
                signature = signature,
                changelog = changelog,
                fileSizeFormatted = fileSizeFormatted,
                publishedAt = publishedAt,
                hasUpdate = hasUpdate
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getCurrentVersionCode(context: Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "Bilinmiyor"
        val mb = bytes / (1024.0 * 1024.0)
        return String.format("%.2f MB", mb)
    }
}
