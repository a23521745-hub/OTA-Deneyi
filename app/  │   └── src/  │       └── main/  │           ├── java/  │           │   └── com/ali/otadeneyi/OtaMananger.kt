package com.ali.otadeneyi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class OtaUpdate(
    val version: String,
    val versionCode: Int,
    val downloadUrl: String,
    val sha256: String,
    val signature: String
)

class OtaManager {

    // Daha sonra bunu GitHub'daki gerçek update.json adresimizle değiştireceğiz.
    private val updateUrl =
        "https://raw.githubusercontent.com/a23521745-hub/OTA-Deneyi/main/ota/update.json"

    suspend fun checkForUpdate(
        currentVersionCode: Int
    ): OtaUpdate? = withContext(Dispatchers.IO) {

        try {
            val url = URL(updateUrl)

            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return@withContext null
            }

            val response = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            connection.disconnect()

            val json = JSONObject(response)

            val version = json.getString("version")
            val versionCode = json.getInt("versionCode")
            val downloadUrl = json.getString("downloadUrl")
            val sha256 = json.getString("sha256")
            val signature = json.getString("signature")

            if (versionCode > currentVersionCode) {
                OtaUpdate(
                    version = version,
                    versionCode = versionCode,
                    downloadUrl = downloadUrl,
                    sha256 = sha256,
                    signature = signature
                )
            } else {
                null
            }

        } catch (e: Exception) {
            null
        }
    }
}
