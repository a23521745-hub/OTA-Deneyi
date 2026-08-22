package com.example.otadashboard.ota_updater

import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object RsaVerifier {

    /**
     * İndirilen dosyanın SHA256withRSA imzasını gerçekten doğrular.
     * Başarısız olursa (yanlış imza, bozuk dosya, geçersiz key) false döner.
     */
    fun verify(filePath: String, signatureBase64: String, publicKeyPem: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false

            val publicKey = getPublicKeyFromPem(publicKeyPem)
            val signatureBytes = Base64.decode(signatureBase64, Base64.DEFAULT)

            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(publicKey)

            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    signature.update(buffer, 0, bytesRead)
                }
            }

            // Gerçek kriptografik doğrulama burada yapılıyor.
            signature.verify(signatureBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getPublicKeyFromPem(pem: String): PublicKey {
        val cleanPem = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")

        val keyBytes = Base64.decode(cleanPem, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }
}