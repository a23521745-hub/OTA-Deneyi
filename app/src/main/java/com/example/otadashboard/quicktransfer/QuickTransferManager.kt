package com.example.otadashboard.quick_transfer

import android.content.Context
import android.net.Uri
import android.nfc.NfcAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class QuickTransferManager(
    private val context: Context
) {

    private val nfcAdapter: NfcAdapter? by lazy {
        NfcAdapter.getDefaultAdapter(context)
    }

    fun isNfcAvailable(): Boolean {
        return nfcAdapter != null
    }

    fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }

    /**
     * Seçilen dosya için aktarım oturumu oluşturur.
     *
     * Burada dosyanın tamamı belleğe alınmaz.
     * SHA-256 stream üzerinden hesaplanır.
     */
    suspend fun createSession(
        uri: Uri
    ): Result<TransferSession> = withContext(Dispatchers.IO) {

        try {
            val resolver = context.contentResolver

            val metadata = resolver.query(
                uri,
                arrayOf(
                    android.provider.OpenableColumns.DISPLAY_NAME,
                    android.provider.OpenableColumns.SIZE
                ),
                null,
                null,
                null
            )?.use { cursor ->

                if (!cursor.moveToFirst()) {
                    null
                } else {
                    val nameIndex =
                        cursor.getColumnIndex(
                            android.provider.OpenableColumns.DISPLAY_NAME
                        )

                    val sizeIndex =
                        cursor.getColumnIndex(
                            android.provider.OpenableColumns.SIZE
                        )

                    val name =
                        if (nameIndex >= 0) {
                            cursor.getString(nameIndex)
                        } else {
                            "received_file"
                        }

                    val size =
                        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                            cursor.getLong(sizeIndex)
                        } else {
                            -1L
                        }

                    name to size
                }
            }

            val filename = metadata?.first ?: "received_file"
            val size = metadata?.second ?: -1L

            if (size < 0L) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Dosya boyutu okunamadı."
                    )
                )
            }

            val mimeType =
                resolver.getType(uri)
                    ?: "application/octet-stream"

            val sha256 = calculateSha256(uri)

            Result.success(
                TransferSession(
                    filename = filename,
                    mimeType = mimeType,
                    size = size,
                    sha256 = sha256,
                    status = TransferStatus.WAITING_FOR_PEER
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Dosyanın SHA-256 hash'ini stream üzerinden hesaplar.
     */
    private fun calculateSha256(uri: Uri): String {

        val digest = MessageDigest.getInstance("SHA-256")

        context.contentResolver
            .openInputStream(uri)
            ?.use { input ->

                val buffer = ByteArray(BUFFER_SIZE)

                while (true) {
                    val read = input.read(buffer)

                    if (read == -1) {
                        break
                    }

                    if (read > 0) {
                        digest.update(buffer, 0, read)
                    }
                }
            }
            ?: throw IllegalStateException(
                "Dosya okunamadı."
            )

        return digest
            .digest()
            .joinToString("") {
                "%02x".format(it)
            }
    }

    /**
     * NFC'nin cihazda kullanılabilir olup olmadığını kontrol eder.
     */
    fun getNfcStatus(): NfcStatus {
        return when {
            nfcAdapter == null ->
                NfcStatus.NOT_SUPPORTED

            !nfcAdapter.isEnabled ->
                NfcStatus.DISABLED

            else ->
                NfcStatus.READY
        }
    }

    enum class NfcStatus {
        NOT_SUPPORTED,
        DISABLED,
        READY
    }

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
    }
}
