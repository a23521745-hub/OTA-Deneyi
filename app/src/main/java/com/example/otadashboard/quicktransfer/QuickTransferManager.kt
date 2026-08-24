package com.example.otadashboard.quick_transfer

import android.content.Context
import android.net.Uri
import android.nfc.NfcAdapter
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.InputStream
import java.security.MessageDigest

/**
 * NFC ve Yüksek Hızlı Veri Aktarımı için hazırlık, meta veri çekme 
 * ve SHA-256 doğrulama işlemlerini yürüten yönetici sınıfı.
 */
class QuickTransferManager(
    private val context: Context
) {

    private val nfcAdapter: NfcAdapter? by lazy {
        NfcAdapter.getDefaultAdapter(context.applicationContext)
    }

    /**
     * Cihazın NFC donanım durumunu güvenli şekilde döndürür.
     * (Local variable binding ile Smart Cast hatası engellenmiştir)
     */
    fun getNfcStatus(): NfcStatus {
        val adapter = nfcAdapter
        return when {
            adapter == null -> NfcStatus.NOT_SUPPORTED
            !adapter.isEnabled -> NfcStatus.DISABLED
            else -> NfcStatus.READY
        }
    }

    fun isNfcReady(): Boolean = getNfcStatus() == NfcStatus.READY

    /**
     * Seçilen [Uri] için aktarım oturumu hazırlar.
     * Dosyayı RAM'e yüklemeden akış (stream) üzerinden SHA-256 hash'ini hesaplar.
     *
     * @param uri Aktarılacak dosyanın Android Uri adresi.
     * @return [Result] içinde oluşturulan [TransferSession] nesnesi.
     */
    suspend fun createSession(uri: Uri): Result<TransferSession> = withContext(Dispatchers.IO) {
        runCatching {
            val (filename, size) = fetchFileMetadata(uri)
            
            require(size > 0L) { 
                "Geçersiz dosya boyutu veya dosya boş ($size bytes)." 
            }

            val mimeType = context.contentResolver.getType(uri) ?: DEFAULT_MIME_TYPE
            val sha256Hash = calculateSha256(uri)

            TransferSession(
                filename = filename,
                mimeType = mimeType,
                size = size,
                sha256 = sha256Hash,
                status = TransferStatus.WAITING_FOR_PEER
            )
        }
    }

    /**
     * Uri üzerinden dosya adı ve boyut bilgilerini çeker.
     */
    private fun fetchFileMetadata(uri: Uri): Pair<String, Long> {
        val resolver = context.contentResolver
        
        return resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else DEFAULT_FILENAME
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1L

                name to size
            } else {
                DEFAULT_FILENAME to -1L
            }
        } ?: (DEFAULT_FILENAME to -1L)
    }

    /**
     * Dosyayı parçalar halinde okuyarak bellek tasarruflu SHA-256 hash'i üretir.
     */
    private fun calculateSha256(uri: Uri): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw FileNotFoundException("Dosya okuma akışı açılamadı: $uri")

        inputStream.use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                if (bytesRead > 0) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
        }

        return digest.digest().toHexString()
    }

    /**
     * Byte dizisini Hexadecimal String formatına çeviren dahili uzantı.
     */
    private fun ByteArray.toHexString(): String {
        return joinToString("") { byte -> "%02x".format(byte) }
    }

    enum class NfcStatus {
        NOT_SUPPORTED,
        DISABLED,
        READY
    }

    companion object {
        private const val BUFFER_SIZE = 64 * 1024 // 64 KB I/O Tamponu
        private const val HASH_ALGORITHM = "SHA-256"
        private const val DEFAULT_FILENAME = "received_file"
        private const val DEFAULT_MIME_TYPE = "application/octet-stream"
    }
}
