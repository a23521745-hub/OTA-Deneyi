package com.example.otadashboard.quick_transfer

import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.io.OutputStream

/**
 * NFC el sıkışması (Handshake) sonrası Wi-Fi Direct, TCP/Socket veya Bluetooth 
 * gibi taşıma protokollerinin uygulaması gereken soyut arayüz (Transport Contract).
 */
interface QuickTransferTransport {

    /**
     * Taşıma katmanının anlık durumunu reaktif olarak gözlemlemek için [StateFlow].
     */
    val state: StateFlow<TransportState>

    /**
     * Bağlantının aktif ve veri transferine hazır olup olmadığını döndürür.
     */
    fun isConnected(): Boolean

    /**
     * Karşı cihaz ile ağ/soket seviyesinde bağlantı kurar.
     */
    suspend fun connect()

    /**
     * Aktarım öncesinde dosya bilgilerini (ad, boyut, SHA-256) karşı tarafa iletir.
     *
     * @param session Gönderilecek [TransferSession] meta verisi.
     */
    suspend fun sendMetadata(session: TransferSession)

    /**
     * Karşı taraftan gönderilen dosya meta verisini (oturum bilgisini) dinler ve okur.
     *
     * @return Karşı taraftan alınan [TransferSession] nesnesi.
     */
    suspend fun receiveMetadata(): TransferSession

    /**
     * Giriş akışından ([input]) okunan dosya baytlarını çıkış akışına ([output])
     * aktarır ve ilerleme durumunu [onProgress] ile bildirir.
     *
     * @param input Okunacak dosya/veri akışı.
     * @param output Yazılacak soket/ağ akışı.
     * @param totalBytes Aktarılacak toplam bayt miktarı.
     * @param onProgress Aktarılan bayt ve toplam bayt miktarını bildiren callback.
     */
    suspend fun sendPayload(
        input: InputStream,
        output: OutputStream,
        totalBytes: Long,
        onProgress: (transferredBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    )

    /**
     * Bağlantıyı, soketleri ve açık akışları (stream) güvenli bir şekilde kapatır.
     */
    suspend fun disconnect()

    /**
     * Devam eden aktarım veya bağlantı sürecini anında iptal eder.
     */
    fun cancel()

    /**
     * Taşıma katmanının yaşam döngüsü durumlarını temsil eder.
     */
    enum class TransportState {
        IDLE,
        CONNECTING,
        CONNECTED,
        TRANSFERRING,
        DISCONNECTED,
        ERROR
    }
}
