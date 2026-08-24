package com.example.otadashboard.quick_transfer

import android.content.Context
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

/**
 * QuickTransferManager
 *
 * Hızlı Aktarım sisteminin NFC tarafını yönetir.
 *
 * NFC'nin görevi:
 *  - Karşı cihazı keşfetmek
 *  - Aktarım oturumunu başlatmak
 *  - Dosya metadata bilgisini paylaşmak
 *
 * Dosyanın kendisi için daha sonra ayrı bir transport katmanı
 * eklenebilir.
 */
class QuickTransferManager(
    private val context: Context
) {

    private val nfcAdapter: NfcAdapter? =
        NfcAdapter.getDefaultAdapter(context)

    /**
     * Cihaz NFC destekliyor mu?
     */
    fun isNfcSupported(): Boolean {
        return nfcAdapter != null
    }

    /**
     * NFC açık mı?
     */
    fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }

    /**
     * NFC kullanılabilir durumda mı?
     */
    fun isAvailable(): Boolean {
        return isNfcSupported() && isNfcEnabled()
    }

    /**
     * Dosya metadata modeli.
     */
    data class TransferMetadata(
        val fileName: String,
        val fileSize: Long,
        val sha256: String
    )

    /**
     * Seçilen dosyanın SHA-256 hash'ini hesaplar.
     *
     * Hash hiçbir zaman dosyanın kendisi yerine geçmez.
     * Aktarım sonunda bütünlük kontrolü için kullanılır.
     */
    suspend fun calculateSha256(uri: Uri): String =
        withContext(Dispatchers.IO) {

            val digest = MessageDigest.getInstance("SHA-256")

            context.contentResolver.openInputStream(uri)?.use { input ->

                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

                while (true) {
                    val read = input.read(buffer)

                    if (read == -1) {
                        break
                    }

                    digest.update(buffer, 0, read)
                }

            } ?: throw IllegalArgumentException(
                "Dosya okunamadı."
            )

            digest.digest()
                .joinToString("") { byte ->
                    "%02x".format(byte)
                }
        }

    /**
     * Dosyanın aktarım metadata bilgisini oluşturur.
     */
    suspend fun createTransferMetadata(
        uri: Uri,
        fileName: String
    ): TransferMetadata =
        withContext(Dispatchers.IO) {

            val size = getFileSize(uri)
            val hash = calculateSha256(uri)

            TransferMetadata(
                fileName = fileName,
                fileSize = size,
                sha256 = hash
            )
        }

    /**
     * Dosya boyutunu öğrenir.
     */
    private fun getFileSize(uri: Uri): Long {

        context.contentResolver
            .query(
                uri,
                arrayOf(android.provider.OpenableColumns.SIZE),
                null,
                null,
                null
            )
            ?.use { cursor ->

                val sizeIndex =
                    cursor.getColumnIndex(
                        android.provider.OpenableColumns.SIZE
                    )

                if (sizeIndex >= 0 && cursor.moveToFirst()) {
                    return cursor.getLong(sizeIndex)
                }
            }

        return context.contentResolver
            .openAssetFileDescriptor(uri, "r")
            ?.use { it.length }
            ?: -1L
    }

    /**
     * NFC Tag üzerinden karşı cihaz algılandığında çağrılabilir.
     */
    fun onTagDiscovered(tag: Tag): Boolean {
        return tag.techList.isNotEmpty()
    }

    /**
     * Aktarım metadata bilgisini NFC NDEF mesajına dönüştürür.
     *
     * DİKKAT:
     * Burada dosyanın byte'larını göndermiyoruz.
     * Sadece metadata/oturum bilgisi gönderilir.
     */
    fun createNdefMessage(
        metadata: TransferMetadata
    ): NdefMessage {

        val payload = buildString {
            append("OTA_DENeyi_QUICK_TRANSFER\n")
            append("name=")
            append(metadata.fileName)
            append('\n')
            append("size=")
            append(metadata.fileSize)
            append('\n')
            append("sha256=")
            append(metadata.sha256)
        }.toByteArray(Charsets.UTF_8)

        val record = NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            MIME_TYPE.toByteArray(Charsets.US_ASCII),
            ByteArray(0),
            payload
        )

        return NdefMessage(
            arrayOf(record)
        )
    }

    /**
     * NFC mesajından metadata çıkarır.
     */
    fun parseNdefMessage(
        message: NdefMessage
    ): TransferMetadata? {

        val record = message.records.firstOrNull()
            ?: return null

        if (!record.type.contentEquals(
                MIME_TYPE.toByteArray(Charsets.US_ASCII)
            )
        ) {
            return null
        }

        val text = String(
            record.payload,
            Charsets.UTF_8
        )

        if (!text.startsWith("OTA_DENeyi_QUICK_TRANSFER")) {
            return null
        }

        val values = mutableMapOf<String, String>()

        text.lines().forEach { line ->

            val separator = line.indexOf('=')

            if (separator > 0) {
                val key = line.substring(
                    0,
                    separator
                )

                val value = line.substring(
                    separator + 1
                )

                values[key] = value
            }
        }

        val fileName = values["name"]
            ?: return null

        val fileSize = values["size"]
            ?.toLongOrNull()
            ?: return null

        val sha256 = values["sha256"]
            ?: return null

        return TransferMetadata(
            fileName = fileName,
            fileSize = fileSize,
            sha256 = sha256
        )
    }

    /**
     * Aktarım sonrasında alınan dosyanın hash'ini
     * beklenen hash ile karşılaştırır.
     */
    suspend fun verifyFile(
        uri: Uri,
        expectedSha256: String
    ): Boolean {

        val actualSha256 =
            calculateSha256(uri)

        return actualSha256.equals(
            expectedSha256,
            ignoreCase = true
        )
    }

    companion object {

        private const val MIME_TYPE =
            "application/vnd.otadeneyi.quicktransfer"

    }
}
