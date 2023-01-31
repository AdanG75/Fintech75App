package com.example.fintech75.data.model

import com.google.gson.annotations.SerializedName

data class Secure(
    @SerializedName("key")
    val key: String,
    @SerializedName("iv")
    val iv: String,
    @SerializedName("block_size")
    val blockSize: Int
)

data class EncryptedResult(
    @SerializedName("data")
    val cipherText: String,
    @SerializedName("iv")
    val iv: String,
    @SerializedName("block_size")
    val blockSize: Int
)

data class PEMData(
    val pem: String
)
