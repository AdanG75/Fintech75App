package com.example.fintech75.secure

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.security.*
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

private const val HEADER_PUBLIC_KEY: String = "-----BEGIN PUBLIC KEY-----"
private const val FOOTER_PUBLIC_KEY: String = "-----END PUBLIC KEY-----"
private const val HEADER_PRIVATE_KEY: String = "-----BEGIN PRIVATE KEY-----"
private const val FOOTER_PRIVATE_KEY: String = "-----END PRIVATE KEY-----"

/*
 * RSA Key Size: 2048
 * Cipher Type: RSA/ECB/OAEPWithSHA-256AndMGF1Padding
 */

class RSASecure {
    companion object {
        // Generate private and public key with length of 2048 bits
        fun generatePublicAndPrivateKey(): Map<String, Key> {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048)
            val pair = keyGen.generateKeyPair()
            val privateKey = pair.private
            val publicKey = pair.public

            return mapOf("privateKey" to privateKey, "publicKey" to publicKey)
        }

        // convert String publickey to Key object
        @Throws(GeneralSecurityException::class, IOException::class)
        fun loadPublicKey(stored: String): Key {
            val data: ByteArray = Base64.getDecoder().decode(stored.toByteArray())
            val spec = X509EncodedKeySpec(data)
            val fact = KeyFactory.getInstance("RSA")
            return fact.generatePublic(spec)
        }

        @Throws(Exception::class)
        fun loadPublicKeyFromPEM(pemKeyData: String): Key {
            val keyWithoutHeaders = clearPEMFormat(pemKeyData, "public")

            return loadPublicKey(keyWithoutHeaders)
        }

        @Throws(Exception::class)
        private fun encryptRSA(data: String, publicKey: Key): String {
            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            val oaepParams = OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec("SHA-256"),
                PSource.PSpecified.DEFAULT
            )
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams)

            return Base64.getEncoder().encodeToString(cipher.doFinal(data.toByteArray()))
        }

        // Encrypt using publickey as string
        @Throws(Exception::class)
        fun encryptMessage(plainText: String, publickey: String): String {
            val key: Key = loadPublicKey(publickey)

            return encryptRSA(plainText, key)
        }

        // Encrypt using publickey
        @Throws(Exception::class)
        fun encryptMessage(plainText: String, publicKey: Key): String {
            return encryptRSA(plainText, publicKey)
        }

        // Decrypt using privatekey
        @Throws(Exception::class)
        private fun decryptRSA(data: String, privateKey: PrivateKey): String {
            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            val oaepParams = OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec("SHA-256"),
                PSource.PSpecified.DEFAULT
            )
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams)

            return String( cipher.doFinal(Base64.getDecoder().decode(data)) )
        }


        // Decrypt using privatekey as string
        @Throws(Exception::class)
        fun decryptMessage(encryptedText: String?, privateKey: String): String {
            val key = loadPrivateKey(privateKey)
            encryptedText?.let {
                return decryptRSA(encryptedText, key)
            }

            return ""
        }

        // Decrypt using privatekey
        @Throws(Exception::class)
        fun decryptMessage(encryptedText: String?, privateKey: PrivateKey): String {
            encryptedText?.let {
                return decryptRSA(encryptedText, privateKey)
            }

            return ""
        }

        @Throws(Exception::class)
        private fun clearPEMFormat(pemKeyData: String, keyType: String): String {

            val cleanData: String = if (keyType.lowercase() == "public") {
                pemKeyData
                    .replace(HEADER_PUBLIC_KEY, "")
                    .replace("\n", "")
                    .replace(FOOTER_PUBLIC_KEY, "")
            } else {
                pemKeyData
                    .replace(HEADER_PRIVATE_KEY, "")
                    .replace("\n", "")
                    .replace(FOOTER_PRIVATE_KEY, "")
            }

            return cleanData
        }

        // Convert String private key to privateKey object
        @Throws(GeneralSecurityException::class)
        fun loadPrivateKey(key64: String): PrivateKey {
            val clear: ByteArray = Base64.getDecoder().decode(key64.toByteArray())
            val keySpec = PKCS8EncodedKeySpec(clear)
            val fact = KeyFactory.getInstance("RSA")
            val priv = fact.generatePrivate(keySpec)
            Arrays.fill(clear, 0.toByte())
            return priv
        }

        // Convert Private Key PEM to privateKey object
        @Throws(GeneralSecurityException::class)
        fun loadPrivateKeyFromPEM(pemKeyData: String): PrivateKey {
            val keyWithoutHeaders = clearPEMFormat(pemKeyData, "private")

            return loadPrivateKey(keyWithoutHeaders)
        }

        private fun generatePEM(key: String, keyType: String): String {
            var pem: String = ""
            if (keyType.lowercase() == "public") {
                pem += HEADER_PUBLIC_KEY
                pem += splitKey(key)
                pem += if (pem.endsWith("\n")) {
                    FOOTER_PUBLIC_KEY
                } else {
                    "\n$FOOTER_PUBLIC_KEY"
                }
            } else if (keyType.lowercase() == "private") {
                pem += HEADER_PRIVATE_KEY
                pem += splitKey(key)
                pem += if (pem.endsWith("\n")) {
                    FOOTER_PRIVATE_KEY
                } else {
                    "\n$FOOTER_PRIVATE_KEY"
                }
            } else {
                throw InvalidParameterException("Type $keyType not found")
            }

            return pem
        }

        private fun splitKey(key: String): String {
            var dividedKey: String = ""

            var count: Int = 0
            for (letter in key) {
                if (count % 64 == 0) {
                    dividedKey += "\n$letter"
                } else {
                    dividedKey += letter
                }
                ++count
            }

            return dividedKey
        }

        @Throws(Exception::class)
        fun generateRSAPEMFormat(key: String, keyType: String): String = generatePEM(key, keyType)

        @Throws(Exception::class)
        fun generateRSAPEMFormat(key: Key, keyType: String): String {
            val keyString = Base64.getEncoder().encodeToString(key.encoded)

            return generatePEM(keyString, keyType)
        }

        @Throws(Exception::class)
        fun savePEM(pem: String, path: String = "./", fileName: String){
            var fullName: String = ""

            fullName = if (!fileName.endsWith(".pem")){ "$fileName.pem" } else { fileName }
            fullName = if (!path.endsWith('/')) { "$path/$fullName" } else { "$path$fullName" }

            val file: File = File(fullName)
            val isFileCreated :Boolean = file.createNewFile()

            if(isFileCreated){
                println("$fileName was created successfully.")
                file.writeText(pem, Charsets.UTF_8)
            } else{
                println("$fileName already exists.")
                throw FileAlreadyExistsException(file = file, reason = "File $fullName already exist.")
            }
        }

        @Throws(Exception::class)
        fun getPEMFromFile(path: String): String {
            val file: File = File(path)
            if (!file.exists()) {
                throw FileNotFoundException("File $path not found.")
            }

            return file.readText(Charsets.UTF_8)
        }
    }
}