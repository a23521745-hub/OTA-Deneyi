package com.example.otadashboard.quick_transfer

import java.io.InputStream
import java.io.OutputStream

interface QuickTransferTransport {

    /**
     * Transport bağlantısını hazırlar.
     */
    suspend fun connect()

    /**
     * Transport bağlantısının hazır olup olmadığını döndürür.
     */
    fun isConnected(): Boolean

    /**
     * Metadata/handshake bilgisini karşı tarafa gönderir.
     */
    suspend fun sendMetadata(session: TransferSession)

    /**
     * Dosyanın byte'larını karşı tarafa aktarır.
     */
    suspend fun send(
        input: InputStream,
        output: OutputStream,
        totalBytes: Long,
        onProgress: (transferredBytes: Long, totalBytes: Long) -> Unit
    )

    /**
     * Bağlantıyı güvenli şekilde kapatır.
     */
    suspend fun disconnect()

    /**
     * Aktarım sırasında oluşan bağlantıyı iptal eder.
     */
    fun cancel()
}
