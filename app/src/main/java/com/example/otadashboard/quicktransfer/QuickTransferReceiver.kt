package com.example.otadashboard.quick_transfer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Akış (InputStream) üzerinden gelen veriyi bellek tasarruflu şekilde diske yazan,
 * eşzamanlı SHA-256 doğrulamasını gerçekleştirip aktarım oturumunu yöneten alıcı sınıfı.
 */
class QuickTransferReceiver(
    private val context: Context
) {

    /**
     * Gelen veri akışını [context.cacheDir] içerisine kaydeder, ilerlemeyi bildirir 
     * ve aktarım sonunda SHA-256 özetini doğrulayarak oturumu tamamlar.
     *
     * @param session Aktarımı yapılacak dosyaya ait oturum bilgisi.
     * @param input Gelen verinin [InputStream] akışı.
     * @param onProgress Aktarılan bayt ve toplam bayt miktarını bildiren geri çağırma (callback).
     * @return Doğrulanmış ve tamamlanmış [TransferSession] veya hata durumunda [Result.failure].
     */
    suspend fun receive(
        session: TransferSession,
        input: InputStream,
        onProgress: (transferredBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<TransferSession> = withContext(Dispatchers.IO) {
        runCatching {
            require(session.size > 0L) { "Geçersiz veya boş dosya boyutu: ${session.size} bytes." }

            val safeFilename = sanitizeFilename(session.filename)
            val transferDirectory = File(context.cacheDir, TRANSFER_DIR_NAME).apply {
                if (!exists()) mkdirs()
            }

            // Aktarım esnasında geçici dosya (.tmp) kullanılır
            val tempFile = File(transferDirectory, "${session.sessionId}_${safeFilename}.tmp")
            val finalDestination = File(transferDirectory, "${session.sessionId}_$safeFilename")

            val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            var receivedBytes = 0L

            try {
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)

                    while (receivedBytes < session.size) {
                        val remaining = session.size - receivedBytes
                        val requested = minOf(buffer.size.toLong(), remaining).toInt()

                        val read = input.read(buffer, 0, requested)
                        if (read == -1) {
                            throw IllegalStateException("Aktarım tamamlanmadan akış beklenmedik şekilde kesildi.")
                        }

                        if (read > 0) {
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                            receivedBytes += read
                            onProgress(receivedBytes, session.size)
                        }
                    }
                    output.flush()
                }

                val actualHash = digest.digest().toHexString()

                // SHA-256 Doğrulaması
                if (!actualHash.equals(session.sha256, ignoreCase = true)) {
                    throw SecurityException(
                        "SHA-256 doğrulama hatası! Dosya bozulmuş veya müdahale edilmiş olabilir. (Beklenen: ${session.sha256}, Alınan: $actualHash)"
                    )
                }

                // Doğrulama başarılıysa geçici dosyayı asıl ismine dönüştür
                if (!tempFile.renameTo(finalDestination)) {
                    tempFile.copyTo(finalDestination, overwrite = true)
                    tempFile.delete()
                }

                session.copy(
                    status = TransferStatus.COMPLETED,
                    transferredBytes = receivedBytes,
                    errorMessage = null
                )

            } catch (e: Exception) {
                // Hata durumunda bozuk veya yarım kalmış tüm artıkları temizle
                if (tempFile.exists()) tempFile.delete()
                if (finalDestination.exists()) finalDestination.delete()
                throw e
            }
        }
    }

    /**
     * Dosya adını dizin geçişi (path traversal) ve geçersiz karakterlerden arındırır.
     */
    private fun sanitizeFilename(filename: String): String {
        val cleaned = filename
            .replace(INVALID_FILENAME_CHARS_REGEX, "_")
            .trim()

        return if (cleaned.isBlank()) {
            DEFAULT_FILENAME
        } else {
            cleaned.take(MAX_FILENAME_LENGTH)
        }
    }

    /**
     * Byte dizisini Hexadecimal String formatına çeviren dahili uzantı.
     */
    private fun ByteArray.toHexString(): String {
        return joinToString("") { byte -> "%02x".format(byte) }
    }

    companion object {
        private const val BUFFER_SIZE = 64 * 1024 // 64 KB I/O Tamponu
        private const val MAX_FILENAME_LENGTH = 180
        private const val HASH_ALGORITHM = "SHA-256"
        private const val DEFAULT_FILENAME = "received_file"
        private const val TRANSFER_DIR_NAME = "quick_transfer"
        private val INVALID_FILENAME_CHARS_REGEX = Regex("[\\\\/:*?\"<>|]")
    }
}
