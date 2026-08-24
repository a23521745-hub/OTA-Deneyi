package com.example.otadashboard.quick_transfer

import java.util.UUID

enum class TransferStatus {
    CREATED,
    WAITING_FOR_PEER,
    CONNECTING,
    TRANSFERRING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class TransferSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val filename: String,
    val mimeType: String,
    val size: Long,
    val sha256: String,
    val status: TransferStatus = TransferStatus.CREATED,
    val transferredBytes: Long = 0L,
    val errorMessage: String? = null
) {
    val progress: Float
        get() = when {
            size <= 0L -> 0f
            transferredBytes >= size -> 1f
            else -> transferredBytes.toFloat() / size.toFloat()
        }

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
}
