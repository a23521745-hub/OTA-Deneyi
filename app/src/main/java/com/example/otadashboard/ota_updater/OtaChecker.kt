package com.example.otadashboard.ota_updater

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object OtaChecker {

    suspend fun checkForUpdate(context: Context, jsonUrl: String): UpdateInfo? {
        return try {
            val url = URL(jsonUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                // GitHub isteği reddetmesin diye User-Agent ekliyoruz
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                instanceFollowRedirects = true
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val stream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(stream))
                val content = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    content.append(line)
                }

                reader.close()
                stream.close()
                connection.disconnect()

                parseJson(context, content.toString())
            } else {
                // Sunucu 403, 404, 500 gibi bir hata döndürdüğünde Logcat'ten görülebilir
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseJson(context: Context, jsonStr: String): UpdateInfo {
        val json = JSONObject(jsonStr)
        val latestVersionCode = json.getInt("versionCode")
        val apkUrl = json.getString("apkUrl")
        val signature = json.getString("signature")
        val changelog = json.optString("changelog", "Değişiklik belirtilmedi.")
        val fileSizeFormatted = json.optString("fileSizeFormatted", "Bilinmiyor")
        val publishedAt = json.optString("publishedAt", "Bilinmiyor")

        val currentVersionCode = getAppVersionCode(context)
        val hasUpdate = latestVersionCode > currentVersionCode

        return UpdateInfo(
            versionCode = latestVersionCode,
            currentVersionCode = currentVersionCode,
            apkUrl = apkUrl,
            signature = signature,
            changelog = changelog,
            fileSizeFormatted = fileSizeFormatted,
            publishedAt = publishedAt,
            hasUpdate = hasUpdate
        )
    }

    private fun getAppVersionCode(context: Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }
}
