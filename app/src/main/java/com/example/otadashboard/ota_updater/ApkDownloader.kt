package com.example.otadashboard.ota_updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ApkDownloader {

    private const val USER_AGENT = "OTADashboard-App/1.0"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 15_000
    private const val BUFFER_SIZE_BYTES = 8192

    /**
     * Sunucudan APK dosyasını indirir, RSA imzasını doğrular ve başarılıysa paket kurulumunu başlatır.
     * Coroutine scope içerisinden çağrılmalıdır.
     */
    suspend fun downloadAndVerifyApk(
        context: Context,
        apkUrl: String,
        expectedSignatureBase64: String,
        publicKeyPem: String,
        maxRetries: Int = 3,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _, _ -> },
        onResult: (success: Boolean, message: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "update_download.apk")

        // 1. İndirme İletişimi ve Retry Döngüsü
        val downloadResult = executeDownloadWithRetry(
            targetUrl = apkUrl,
            destinationFile = outputFile,
            maxRetries = maxRetries,
            onProgress = onProgress
        )

        if (!downloadResult.isSuccess) {
            outputFile.delete()
            withContext(Dispatchers.Main) {
                onResult(false, downloadResult.errorMessage)
            }
            return@withContext
        }

        // 2. RSA İmza Doğrulaması
        val isSignatureValid = RsaVerifier.verify(
            filePath = outputFile.absolutePath,
            signatureBase64 = expectedSignatureBase64,
            publicKeyPem = publicKeyPem
        )

        if (!isSignatureValid) {
            outputFile.delete()
            withContext(Dispatchers.Main) {
                onResult(false, "İmza doğrulaması başarısız! Dosya güvenilir değil, silindi.")
            }
            return@withContext
        }

        // 3. Paket Yükleyiciye Yönlendirme
        withContext(Dispatchers.Main) {
            onResult(true, "Doğrulama başarılı, kurulum başlatılıyor...")
            installApk(context, outputFile)
        }
    }

    private data class DownloadResult(
        val isSuccess: Boolean,
        val errorMessage: String = ""
    )

    private suspend fun executeDownloadWithRetry(
        targetUrl: String,
        destinationFile: File,
        maxRetries: Int,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): DownloadResult {
        var attempt = 0
        var lastError = "Bilinmeyen İndirme Hatası"

        while (attempt < maxRetries) {
            attempt++
            var connection: HttpURLConnection? = null
            try {
                connection = createConnection(targetUrl)
                var responseCode = connection.responseCode

                // GitHub Releases redirect (301/302/307) takibi
                if (responseCode in 300..399) {
                    val redirectUrl = connection.getHeaderField("Location")
                    if (!redirectUrl.isNullOrEmpty()) {
                        connection.disconnect()
                        connection = createConnection(redirectUrl)
                        responseCode = connection.responseCode
                    }
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    lastError = "Sunucu Yanıtı: HTTP $responseCode (Deneme $attempt/$maxRetries)"
                    continue
                }

                val totalBytes = connection.contentLengthLong
                var downloadedBytes = 0L
                var lastReportedPercent = -1

                connection.inputStream.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE_BYTES)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val percent = if (totalBytes > 0) {
                                ((downloadedBytes * 100) / totalBytes).toInt()
                            } else 0

                            // Sadece yüzdelik dilim değiştiğinde Main Thread'i tetikle
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                withContext(Dispatchers.Main) {
                                    onProgress(percent, downloadedBytes, totalBytes)
                                }
                            }
                        }
                    }
                }

                return DownloadResult(isSuccess = true)

            } catch (e: Exception) {
                e.printStackTrace()
                lastError = "Ağ Hatası (Deneme $attempt/$maxRetries): ${e.localizedMessage ?: "Bağlantı kesildi"}"
            } finally {
                connection?.disconnect()
            }
        }

        return DownloadResult(isSuccess = false, errorMessage = lastError)
    }

    private fun createConnection(urlString: String): HttpURLConnection {
        return (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "*/*")
        }
    }

    private fun installApk(context: Context, file: File) {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }
}
