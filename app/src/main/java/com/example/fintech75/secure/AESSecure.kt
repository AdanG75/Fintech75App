package com.example.fintech75.secure

import com.example.fintech75.data.model.EncryptedResult
import java.nio.charset.Charset
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.properties.Delegates


class AESSecure {
    companion object {
        fun generateEncryptedObject(resulCipher: Map<String, String>): EncryptedResult {
            lateinit var iv: String
            lateinit var cipherData: String
            var blockSize by Delegates.notNull<Int>()

            resulCipher["iv"]?.let {
                iv = it
            }
            resulCipher["cipherData"]?.let {
                cipherData = it
            }
            resulCipher["blockSize"]?.let{
                blockSize = it.toInt()
            }

            return EncryptedResult(cipherText = cipherData, iv = iv, blockSize = blockSize)
        }

        fun encrypt(plainText: String, key: SecretKey): Map<String, String> {
            val plaintext = plainText.toByteArray(Charset.forName("UTF-8"))

            return doEncryption(plaintext = plaintext, key = key)
        }

        fun encrypt(plainText: String, key: String): Map<String, String> {
            val plaintext = plainText.toByteArray(Charset.forName("UTF-8"))
            val mySecretKey = setKey(key)

            return doEncryption(plaintext = plaintext, key = mySecretKey)
        }

        fun encrypt(plainText: ByteArray, key: SecretKey): Map<String, String> {
            return doEncryption(plaintext = plainText, key = key)
        }

        fun encrypt(plainText: ByteArray, key: String): Map<String, String> {
            val mySecretKey = setKey(key)

            return doEncryption(plaintext = plainText, key = mySecretKey)
        }

        private fun doEncryption(plaintext: ByteArray, key: SecretKey): Map<String, String> {
            try {
                val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
                cipher.init(Cipher.ENCRYPT_MODE, key)
                val ciphertext: ByteArray = cipher.doFinal(plaintext)

                val b64Ciphertext = Base64.getEncoder().encodeToString(ciphertext)
                val b64IV = Base64.getEncoder().encodeToString(cipher.iv)

                return mapOf<String, String>("iv" to b64IV, "blockSize" to "128", "cipherData" to b64Ciphertext)

            } catch (e: Exception) {

                println("Error while encrypting: $e")
            }

            throw Exception("Encrypt Error")
        }

        fun decrypt(messageToDecrypt: String, key: String, iv: String): String {
            val secretKey = setKey(key)
            val ivBytes = Base64.getDecoder().decode(iv)
            val message = Base64.getDecoder().decode(messageToDecrypt)

            return doDecrypt(message = message, key = secretKey, iv = ivBytes)
        }

        fun decrypt(messageToDecrypt: String, key: SecretKey, iv: String): String {
            val ivBytes = Base64.getDecoder().decode(iv)
            val message = Base64.getDecoder().decode(messageToDecrypt)

            return doDecrypt(message = message, key = key, iv = ivBytes)
        }

        fun decrypt(messageToDecrypt: ByteArray, key: String, iv: ByteArray): String {
            val secretKey = setKey(key)

            return doDecrypt(message = messageToDecrypt, key = secretKey, iv = iv)
        }

        fun decrypt(messageToDecrypt: ByteArray, key: SecretKey, iv: ByteArray): String {

            return doDecrypt(message = messageToDecrypt, key = key, iv = iv)
        }

        private fun doDecrypt(message: ByteArray, key: SecretKey, iv: ByteArray): String {
            try {

                val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
                val ivBytes = IvParameterSpec(iv)
                cipher.init(Cipher.DECRYPT_MODE, key, ivBytes)

                return String(cipher.doFinal(message))

            } catch (e: Exception) {
                println("Error while decrypting: $e")
            }
            throw Exception("Decrypt Error")
        }


        fun setKey(secret: String): SecretKey {
            val b64Dec = Base64.getDecoder().decode(secret)

            return SecretKeySpec(b64Dec, "AES")
        }

        fun setKey(secret: ByteArray): SecretKey = SecretKeySpec(secret, "AES")

        fun generateAESKey(): SecretKey {
            val keygen = KeyGenerator.getInstance("AES")
            keygen.init(256)

            return keygen.generateKey()
        }

        fun getKeyAsString(key: SecretKey): String = Base64.getEncoder().encodeToString(key.encoded)
    }
}