package com.example.otadashboard.ota_updater

import android.content.Context
import android.content.pm.PackageManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Güncelleme verisini tutan data class
data class UpdateInfo(
    val hasUpdate: Boolean,
    val apkUrl: String,
    val signature: String,
    val changelog: String
)

object OtaChecker {

    // Doğru ve onaylanmış Raw linkini buraya sabitliyoruz
    private const val DEFAULT_JSON_URL = "https://raw.githubusercontent.com/a23521745-hub/OTA-Deneyi/refs/heads/main/update.json"

    fun checkForUpdate(context: Context, jsonUrl: String = DEFAULT_JSON_URL): UpdateInfo? {
        return try {
            // 1. Mevcut sürümü al
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                pInfo.versionCode
            }

            // 2. Sunucudan JSON'u çek
            val url = URL(jsonUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                val serverVersion = json.getInt("versionCode")
                val apkUrl = json.getString("apkUrl")
                val signature = json.getString("signature")
                val changelog = json.getString("changelog")

                // 3. Sürüm karşılaştırması
                if (serverVersion > currentVersionCode) {
                    UpdateInfo(true, apkUrl, signature, changelog)
                } else {
                    UpdateInfo(false, "", "", "")
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
