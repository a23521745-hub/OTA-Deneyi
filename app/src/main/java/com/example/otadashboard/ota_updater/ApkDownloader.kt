package com.example.otadashboard.ota_updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ApkDownloader {

    /**
     * apkUrl'den dosyayı anlık progress (yüzde) bildirerek indirir, RsaVerifier ile imzasını doğrular.
     * @param onProgress (yüzde: Int, indirilenBytes: Long, toplamBytes: Long)
     * @param onResult (success: Boolean, mesaj: String)
     */
    fun downloadAndVerifyApk(
        context: Context,
        apkUrl: String,
        expectedSignatureBase64: String,
        publicKeyPem: String,
        maxRetries: Int = 3,
        onProgress: (Int, Long, Long) -> Unit = { _, _, _ -> },
        onResult: (Boolean, String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val outputFile = File(context.cacheDir, "update_download.apk")
            var attempt = 0
            var downloadSuccess = false
            var lastErrorMessage = "Bilinmeyen Hata"

            // Yeniden Deneme (Retry) Döngüsü
            while (attempt < maxRetries && !downloadSuccess) {
                attempt++
                try {
                    val url = URL(apkUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.requestMethod = "GET"
                    connection.connect()

                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        lastErrorMessage = "İndirme hatası: HTTP ${connection.responseCode} (Deneme $attempt/$maxRetries)"
                        continue
                    }

                    val totalBytes = connection.contentLengthLong
                    var downloadedBytes = 0L

                    connection.inputStream.use { input ->
                        FileOutputStream(outputFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead
                                
                                val progress = if (totalBytes > 0) {
                                    ((downloadedBytes * 100) / totalBytes).toInt()
                                } else 0

                                withContext(Dispatchers.Main) {
                                    onProgress(progress, downloadedBytes, totalBytes)
                                }
                            }
                        }
                    }

                    downloadSuccess = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    lastErrorMessage = "İndirme hatası (Deneme $attempt/$maxRetries): ${e.localizedMessage}"
                }
            }

            if (!downloadSuccess) {
                outputFile.delete()
                withContext(Dispatchers.Main) {
                    onResult(false, lastErrorMessage)
                }
                return@launch
            }

            // İmza doğrulaması
            val isValid = RsaVerifier.verify(
                filePath = outputFile.absolutePath,
                signatureBase64 = expectedSignatureBase64,
                publicKeyPem = publicKeyPem
            )

            if (!isValid) {
                outputFile.delete()
                withContext(Dispatchers.Main) {
                    onResult(false, "İmza doğrulaması başarısız! Dosya güvenilir değil, silindi.")
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                onResult(true, "Doğrulama başarılı, kurulum başlatılıyor...")
                installApk(context, outputFile)
            }
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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        context.startActivity(intent)
    }
}
