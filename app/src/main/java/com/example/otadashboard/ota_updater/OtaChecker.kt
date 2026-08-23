package com.example.otadashboard.ota_updater

import android.content.Context
import android.content.pm.PackageManager
import org.json.JSONObject
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val apkUrl: String,
    val signature: String,
    val changelog: String,
    val hasUpdate: Boolean
)

object OtaChecker {

    fun checkForUpdate(context: Context, jsonUrl: String): UpdateInfo? {
        return try {
            // update.json'ı çek
            val url = URL(jsonUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)

            val signature = json.getString("signature")
            val changelog = json.getString("changelog")

            // GitHub API'den latest release'i çek
            val apiUrl = "https://api.github.com/repos/a23521745-hub/OTA-Deneyi/releases/latest"
            val apiConnection = URL(apiUrl).openConnection()
            apiConnection.connectTimeout = 15000
            apiConnection.readTimeout = 15000
            val apiResponse = apiConnection.inputStream.bufferedReader().readText()
            val releaseJson = JSONObject(apiResponse)

            // Tag name'den version code çıkar (örn: v2 → 2)
            val tagName = releaseJson.getString("tag_name")
            val newVersionCode = tagName.filter { it.isDigit() }.toIntOrNull() ?: 1

            // Latest release'deki ilk .apk dosyasını bul
            val assets = releaseJson.getJSONArray("assets")
            var apkDownloadUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    apkDownloadUrl = asset.getString("browser_download_url")
                    break
                }
            }

            if (apkDownloadUrl.isEmpty()) {
                return null
            }

            val currentVersionCode = getCurrentVersionCode(context)
            val hasUpdate = newVersionCode > currentVersionCode

            UpdateInfo(
                versionCode = newVersionCode,
                apkUrl = apkDownloadUrl,
                signature = signature,
                changelog = changelog,
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
            packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
    }
}
