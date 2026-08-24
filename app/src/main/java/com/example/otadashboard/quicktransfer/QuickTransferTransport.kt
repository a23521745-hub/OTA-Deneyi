package com.example.otadashboard.quick_transfer

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/**
 * QuickTransferTransport
 *
 * Quick Transfer'ın dosya I/O ve bütünlük katmanıdır.
 *
 * Sorumlulukları:
 *  - Dosyayı stream halinde okumak
 *  - Aktarılabilecek parçalara bölmek
 *  - Gelen parçaları güvenli şekilde geçici dosyaya yazmak
 *  - SHA-256 bütünlük doğrulaması yapmak
 *  - Doğrulama başarısızsa geçici dosyayı silmek
 *
 * NFC/NDEF protokolü burada doğrudan uygulanmaz.
 * NFC oturum/metadata katmanı QuickTransferManager tarafından yönetilir.
 */
class QuickTransferTransport(
    private val context: Context
) {

    companion object {
        /**
         * Bellek kullanımını düşük tutmak için varsayılan chunk boyutu.
         */
        const val DEFAULT_CHUNK_SIZE = 16 * 1024

        /**
         * Transfer dosyalarının tutulacağı özel cache dizini.
         */
        private const val TRANSFER_DIRECTORY = "quick_transfer"

        /**
         * Geçici dosya suffix'i.
         */
        private const val TEMP_SUFFIX = ".part"

        /**
         * Tamamlanmış dosya suffix'i.
         */
        private const val COMPLETE_SUFFIX = ".complete"
    }

    /**
     * Aktarım sırasında gönderilecek veri parçası.
     *
     * offset:
     * Dosyanın başlangıcından itibaren chunk'ın konumu.
     */
    data class DataChunk(
        val offset: Long,
        val data: ByteArray,
        val isLast: Boolean
    )

    /**
     * Aktarım sonucu.
     */
    sealed interface TransferResult {

        data class Success(
            val file: File,
            val sha256: String,
            val bytesTransferred: Long
        ) : TransferResult

        data class Failure(
            val reason: String,
            val cause: Throwable? = null
        ) : TransferResult
    }

    /**
     * Dosyayı chunk'lara ayırarak okur.
     *
     * Dosyanın tamamını RAM'e yüklemez.
     */
    suspend fun readChunks(
        uri: Uri,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        onChunk: suspend (DataChunk) -> Unit
    ): Long = withContext(Dispatchers.IO) {

        require(chunkSize > 0) {
            "chunkSize 0'dan büyük olmalıdır."
        }

        val resolver = context.contentResolver

        var offset = 0L
        var totalBytes = 0L

        val inputStream = resolver.openInputStream(uri)
            ?: throw IOException("Dosya açılamadı: $uri")

        BufferedInputStream(inputStream, chunkSize).use { input ->

            while (true) {

                coroutineContext.ensureActive()

                val buffer = ByteArray(chunkSize)
                val read = input.read(buffer)

                if (read <= 0) {
                    break
                }

                val chunkData =
                    if (read == buffer.size) {
                        buffer
                    } else {
                        buffer.copyOf(read)
                    }

                totalBytes += read

                val chunk = DataChunk(
                    offset = offset,
                    data = chunkData,
                    isLast = false
                )

                onChunk(chunk)

                offset += read
            }
        }

        totalBytes
    }

    /**
     * Dosyanın SHA-256 değerini stream üzerinden hesaplar.
     *
     * Bellekte dosyanın tamamını tutmaz.
     */
    suspend fun calculateSha256(
        uri: Uri
    ): String = withContext(Dispatchers.IO) {

        val digest = MessageDigest.getInstance("SHA-256")

        val inputStream = context.contentResolver
            .openInputStream(uri)
            ?: throw IOException("Dosya açılamadı: $uri")

        BufferedInputStream(
            inputStream,
            DEFAULT_CHUNK_SIZE
        ).use { input ->

            val buffer = ByteArray(DEFAULT_CHUNK_SIZE)

            while (true) {

                coroutineContext.ensureActive()

                val read = input.read(buffer)

                if (read == -1) {
                    break
                }

                if (read > 0) {
                    digest.update(buffer, 0, read)
                }
            }
        }

        digest.digest().toHexString()
    }

    /**
     * Gelen aktarım için geçici dosya oluşturur.
     *
     * Dosya tamamlanana kadar .part olarak tutulur.
     */
    fun createTemporaryFile(
        fileName: String
    ): File {

        val directory = File(
            context.cacheDir,
            TRANSFER_DIRECTORY
        )

        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException(
                "Transfer dizini oluşturulamadı."
            )
        }

        val safeName = sanitizeFileName(fileName)

        return File(
            directory,
            "$safeName$TEMP_SUFFIX"
        )
    }

    /**
     * Bir chunk'ı geçici dosyaya ekler.
     *
     * Offset kontrolü sayesinde parçaların yanlış sırada
     * yazılması engellenir.
     */
    suspend fun appendChunk(
        file: File,
        chunk: DataChunk,
        expectedOffset: Long
    ): Long = withContext(Dispatchers.IO) {

        require(chunk.offset == expectedOffset) {
            "Geçersiz chunk offset. Beklenen=$expectedOffset, gelen=${chunk.offset}"
        }

        if (chunk.data.isEmpty()) {
            return@withContext expectedOffset
        }

        BufferedOutputStream(
            FileOutputStream(file, true),
            DEFAULT_CHUNK_SIZE
        ).use { output ->

            output.write(chunk.data)
            output.flush()
        }

        expectedOffset + chunk.data.size
    }

    /**
     * Aktarım tamamlandığında:
     *
     * 1. Dosya boyutunu kontrol eder.
     * 2. SHA-256 hesaplar.
     * 3. Beklenen hash ile karşılaştırır.
     * 4. Başarısızsa dosyayı siler.
     * 5. Başarılıysa .complete dosyasına dönüştürür.
     */
    suspend fun finalizeTransfer(
        temporaryFile: File,
        expectedSize: Long,
        expectedSha256: String
    ): TransferResult = withContext(Dispatchers.IO) {

        try {

            if (!temporaryFile.exists()) {
                return@withContext TransferResult.Failure(
                    reason = "Geçici aktarım dosyası bulunamadı."
                )
            }

            val actualSize = temporaryFile.length()

            if (actualSize != expectedSize) {

                temporaryFile.delete()

                return@withContext TransferResult.Failure(
                    reason = "Dosya boyutu uyuşmuyor. " +
                            "Beklenen=$expectedSize, " +
                            "alınan=$actualSize"
                )
            }

            val actualSha256 =
                calculateSha256(temporaryFile)

            if (!actualSha256.equals(
                    expectedSha256,
                    ignoreCase = true
                )
            ) {

                temporaryFile.delete()

                return@withContext TransferResult.Failure(
                    reason = "SHA-256 doğrulaması başarısız. Dosya silindi."
                )
            }

            val finalFile = File(
                temporaryFile.parentFile,
                temporaryFile.name.removeSuffix(TEMP_SUFFIX)
                    .plus(COMPLETE_SUFFIX)
            )

            if (finalFile.exists()) {
                finalFile.delete()
            }

            if (!temporaryFile.renameTo(finalFile)) {

                temporaryFile.delete()

                return@withContext TransferResult.Failure(
                    reason = "Doğrulanan dosya son konuma taşınamadı."
                )
            }

            TransferResult.Success(
                file = finalFile,
                sha256 = actualSha256,
                bytesTransferred = actualSize
            )

        } catch (e: Exception) {

            temporaryFile.delete()

            TransferResult.Failure(
                reason = e.message
                    ?: "Aktarım doğrulaması sırasında bilinmeyen hata.",
                cause = e
            )
        }
    }

    /**
     * Tamamlanmış aktarım dosyasını siler.
     */
    suspend fun deleteTransfer(
        file: File
    ): Boolean = withContext(Dispatchers.IO) {

        if (!file.exists()) {
            return@withContext true
        }

        file.delete()
    }

    /**
     * Transfer cache'ini temizler.
     */
    suspend fun clearTransferCache() =
        withContext(Dispatchers.IO) {

            val directory = File(
                context.cacheDir,
                TRANSFER_DIRECTORY
            )

            if (!directory.exists()) {
                return@withContext
            }

            directory.listFiles()?.forEach { file ->
                file.delete()
            }
        }

    /**
     * Dosya adındaki path traversal gibi tehlikeli
     * karakterleri temizler.
     */
    private fun sanitizeFileName(
        fileName: String
    ): String {

        val cleaned = fileName
            .replace("\\", "_")
            .replace("/", "_")
            .replace("..", "_")
            .replace("\u0000", "_")
            .trim()

        return if (cleaned.isBlank()) {
            "received_file"
        } else {
            cleaned.take(255)
        }
    }

    /**
     * URI yerine File üzerinden SHA-256 hesaplama.
     *
     * Alıcı tarafındaki doğrulama için kullanılır.
     */
    private suspend fun calculateSha256(
        file: File
    ): String = withContext(Dispatchers.IO) {

        val digest = MessageDigest.getInstance("SHA-256")

        BufferedInputStream(
            file.inputStream(),
            DEFAULT_CHUNK_SIZE
        ).use { input ->

            val buffer = ByteArray(DEFAULT_CHUNK_SIZE)

            while (true) {

                coroutineContext.ensureActive()

                val read = input.read(buffer)

                if (read == -1) {
                    break
                }

                if (read > 0) {
                    digest.update(buffer, 0, read)
                }
            }
        }

        digest.digest().toHexString()
    }

    /**
     * ByteArray -> lowercase hexadecimal.
     */
    private fun ByteArray.toHexString(): String =
        joinToString("") {
            "%02x".format(it)
        }
}
