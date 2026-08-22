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
import java.net.HttpURLConnection
import java.net.URL

object ApkDownloader {

    /**
     * apkUrl'den dosyayı indirir, RsaVerifier ile imzasını doğrular.
     * Doğrulama başarılıysa kurulum ekranını açar; değilse dosyayı siler ve hata döner.
     * onResult ana thread'de çağrılır: (success, mesaj)
     */
    fun downloadAndVerifyApk(
        context: Context,
        apkUrl: String,
        expectedSignatureBase64: String,
        publicKeyPem: String,
        onResult: (Boolean, String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val outputFile = File(context.cacheDir, "update_download.apk")

            try {
                val connection = URL(apkUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "İndirme hatası: HTTP ${connection.responseCode}")
                    }
                    return@launch
                }

                connection.inputStream.use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // İmza doğrulaması — gerçek kontrol burada.
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

            } catch (e: Exception) {
                e.printStackTrace()
                outputFile.delete()
                withContext(Dispatchers.Main) {
                    onResult(false, "Hata: ${e.message}")
                }
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