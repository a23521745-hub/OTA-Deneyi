package com.example.otadashboard.quick_transfer

import android.content.Context
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuickTransferReceiver(
    private val context: Context
) {

    suspend fun receive(
        session: TransferSession,
        input: InputStream,
        onProgress: (transferredBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<TransferSession> = withContext(Dispatchers.IO) {

        if (session.size < 0L) {
            return@withContext Result.failure(
                IllegalArgumentException("Geçersiz dosya boyutu.")
            )
        }

        val safeFilename = sanitizeFilename(session.filename)

        val transferDirectory = File(
            context.cacheDir,
            "quick_transfer"
        ).apply {
            mkdirs()
        }

        val destination = File(
            transferDirectory,
            "${session.sessionId}_$safeFilename"
        )

        var receivedBytes = 0L
        val digest = MessageDigest.getInstance("SHA-256")

        try {
            destination.outputStream().use { output ->
                val buffer = ByteArray(BUFFER_SIZE)

                while (receivedBytes < session.size) {
                    val remaining = session.size - receivedBytes
                    val requested = minOf(buffer.size.toLong(), remaining).toInt()

                    val read = input.read(buffer, 0, requested)

                    if (read == -1) {
                        throw IllegalStateException(
                            "Aktarım beklenmedik şekilde sonlandı."
                        )
                    }

                    if (read == 0) {
                        continue
                    }

                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)

                    receivedBytes += read

                    onProgress(
                        receivedBytes,
                        session.size
                    )
                }

                output.flush()
            }

            val actualHash = digest
                .digest()
                .toHexString()

            if (!actualHash.equals(session.sha256, ignoreCase = true)) {
                destination.delete()

                return@withContext Result.failure(
                    SecurityException(
                        "SHA-256 doğrulaması başarısız. Dosya silindi."
                    )
                )
            }

            val completedSession = session.copy(
                status = TransferStatus.COMPLETED,
                transferredBytes = receivedBytes,
                errorMessage = null
            )

            Result.success(completedSession)

        } catch (e: Exception) {

            destination.delete()

            Result.failure(e)
        }
    }

    private fun sanitizeFilename(filename: String): String {
        val cleaned = filename
            .replace("\\", "_")
            .replace("/", "_")
            .replace("..", "_")
            .trim()

        return if (cleaned.isBlank()) {
            "received_file"
        } else {
            cleaned.take(MAX_FILENAME_LENGTH)
        }
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { byte ->
            "%02x".format(byte)
        }
    }

    companion object {
        private const val BUFFER_SIZE = 32 * 1024
        private const val MAX_FILENAME_LENGTH = 180
    }
}
