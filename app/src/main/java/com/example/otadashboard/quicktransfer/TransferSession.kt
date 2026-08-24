package com.example.otadashboard.quick_transfer

import java.util.UUID
import kotlin.math.log10
import kotlin.math.pow

/**
 * Hızlı aktarım sürecindeki bir oturumun durumunu ve meta verilerini temsil eder.
 */
enum class TransferStatus {
    CREATED,
    WAITING_FOR_PEER,
    CONNECTING,
    TRANSFERRING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED;

    /**
     * Oturumun sonlanıp sonlanmadığını gösterir.
     */
    val isTerminal: Boolean
        get() = this == COMPLETED || this == FAILED || this == CANCELLED

    /**
     * Aktarımın aktif olarak devam edip etmediğini gösterir.
     */
    val isActive: Boolean
        get() = this == CONNECTING || this == TRANSFERRING || this == VERIFYING
}

/**
 * Dosya aktarım oturumuna ait immutable (değişmez) veri modeli.
 */
data class TransferSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val filename: String,
    val mimeType: String,
    val size: Long,
    val sha256: String,
    val status: TransferStatus = TransferStatus.CREATED,
    val transferredBytes: Long = 0L,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 0.0f ile 1.0f arasında ilerleme oranı.
     */
    val progress: Float
        get() = when {
            size <= 0L -> 0f
            transferredBytes >= size -> 1f
            else -> transferredBytes.toFloat() / size.toFloat()
        }

    /**
     * Yüzdesel ilerleme değeri (0 - 100).
     */
    val progressPercentage: Int
        get() = (progress * 100).toInt()

    /**
     * Okunabilir dosya boyutu biçimi (örn: "4.2 MB", "512 KB").
     */
    val formattedSize: String
        get() = formatBytes(size)

    /**
     * Okunabilir aktarılan bayt biçimi (örn: "1.2 MB / 4.2 MB").
     */
    val formattedProgress: String
        get() = "${formatBytes(transferredBytes)} / $formattedSize"

    /**
     * İlerleme bayt miktarını günceller ve durumu otomatik TRANSFERRING olarak ayarlar.
     */
    fun updateProgress(bytes: Long): TransferSession {
        val safeBytes = bytes.coerceIn(0L, size)
        return copy(
            transferredBytes = safeBytes,
            status = if (safeBytes >= size) TransferStatus.VERIFYING else TransferStatus.TRANSFERRING
        )
    }

    /**
     * Oturumu başarıyla tamamlandı olarak işaretler.
     */
    fun markCompleted(): TransferSession {
        return copy(
            status = TransferStatus.COMPLETED,
            transferredBytes = size,
            errorMessage = null
        )
    }

    /**
     * Oturumu hata durumuyla sonlandırır.
     */
    fun markFailed(reason: String): TransferSession {
        return copy(
            status = TransferStatus.FAILED,
            errorMessage = reason
        )
    }

    /**
     * Oturumu kullanıcı/sistem iptali olarak işaretler.
     */
    fun markCancelled(): TransferSession {
        return copy(
            status = TransferStatus.CANCELLED
        )
    }

    /**
     * Genel durum değiştirme metodu.
     */
    fun withStatus(
        newStatus: TransferStatus,
        transferredBytes: Long = this.transferredBytes,
        errorMessage: String? = this.errorMessage
    ): TransferSession {
        return copy(
            status = newStatus,
            transferredBytes = transferredBytes.coerceIn(0L, size),
            errorMessage = errorMessage
        )
    }

    companion object {
        /**
         * Bayt cinsinden değeri B, KB, MB, GB formatına dönüştürür.
         */
        private fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
            val safeGroup = digitGroups.coerceIn(0, units.lastIndex)
            return String.format(
                java.util.Locale.US,
                "%.1f %s",
                bytes / 1024.0.pow(safeGroup.toDouble()),
                units[safeGroup]
            )
        }
    }
}
