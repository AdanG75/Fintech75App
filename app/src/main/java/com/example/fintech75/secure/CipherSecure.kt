package com.example.fintech75.secure

import com.example.fintech75.core.GlobalSettings
import com.example.fintech75.core.Utils
import com.example.fintech75.data.model.EncryptedResult
import com.example.fintech75.data.model.Secure
import com.example.fintech75.data.model.SecureBase
import com.example.fintech75.data.model.SecureRequest
import com.google.gson.Gson
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.SecretKey


class CipherSecure {
    companion object {
        fun <T> packAndEncryptData(classToEncrypt: T, userPem: String): SecureRequest {
            val (dataEncrypted, secureEncrypted) = doPackAndEncrypt(classToEncrypt, false, null)

            return SecureRequest(data = dataEncrypted, secure = secureEncrypted, publicPem = userPem)
        }

        fun <T> packAndEncryptData(
            classToEncrypt: T,
            from_pem: Boolean = false,
            user_pem: String? = null
        ): SecureBase {
            val (dataEncrypted, secureEncrypted) = doPackAndEncrypt(classToEncrypt, from_pem, user_pem)

            return SecureBase(data = dataEncrypted, secure = secureEncrypted)
        }

        private fun <T> doPackAndEncrypt(
            classToEncrypt: T,
            from_pem: Boolean = false,
            user_pem: String? = null
        ): Pair <String, String> {
            val classJSONString: String = Utils.mapToJSON(classToEncrypt)

            // Encrypt data class
            val key: SecretKey = AESSecure.generateAESKey()
            val encryptedData: Map<String, String> = AESSecure.encrypt(classJSONString, key)
            val encryptedResult: EncryptedResult = AESSecure.generateEncryptedObject(encryptedData)

            // Load Public Key
            lateinit var publicKey: PublicKey
            var publicKeyFlag: Boolean = false
            if (from_pem) {
                user_pem?.let {
                    publicKey = RSASecure.loadPublicKeyFromPEM(it) as PublicKey
                    publicKeyFlag = true
                }
            } else {
                GlobalSettings.severPublicKey?.let {
                    publicKey = it
                    publicKeyFlag = true
                }
            }

            if (!publicKeyFlag) {
                throw Exception("No Key has been passed")
            }

            // Encrypt models.Secure
            val secureData: Secure = Secure(
                key = AESSecure.getKeyAsString(key),
                iv = encryptedResult.iv,
                blockSize = encryptedResult.blockSize
            )
            val secureJSONString: String = Utils.mapToJSON(secureData)
            val secureEncrypted: String = RSASecure.encryptMessage(secureJSONString, publicKey)

            return Pair(encryptedResult.cipherText, secureEncrypted)
        }

        inline fun <reified T> unpackAndDecryptData(
            data: SecureRequest,
            privateKey: PrivateKey? = null,
            privatePem: String? = null
        ): T {
            val secureData = SecureBase(data = data.data, secure = data.secure)
            return doUnpackAndDecrypt<T>(secureData, privateKey, privatePem)
        }

        inline fun <reified T> unpackAndDecryptData(
            data: SecureBase,
            privateKey: PrivateKey? = null,
            privatePem: String? = null
        ): T {
            return doUnpackAndDecrypt<T>(data, privateKey, privatePem)
        }

        inline fun <reified T> doUnpackAndDecrypt(data: SecureBase, privateKey: PrivateKey? = null, privatePem: String? = null): T {
            // Load private key
            lateinit var pKey: PrivateKey
            if (privateKey == null) {
                if (privatePem == null) {
                    if (GlobalSettings.mobilePrivateKey == null) {
                        throw Exception("No key has been passed")
                    } else {
                        GlobalSettings.mobilePrivateKey?.let {
                            pKey = it
                        }
                    }
                } else {
                    pKey = RSASecure.loadPrivateKeyFromPEM(privatePem)
                }
            } else {
                pKey = privateKey
            }

            // Decrypt secure
            val decryptSecure: String = RSASecure.decryptMessage(data.secure, pKey)
            val receiveSecure: Secure = Utils.jsonToClass<Secure>(decryptSecure)

            // Decrypt data
            val decryptData: String = AESSecure.decrypt(data.data, receiveSecure.key, receiveSecure.iv)

            // Obtain original data
            return Gson().fromJson(decryptData, T::class.java)
        }
    }
}