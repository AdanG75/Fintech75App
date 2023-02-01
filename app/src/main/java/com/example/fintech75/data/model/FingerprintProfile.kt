package com.example.fintech75.data.model

import com.google.gson.annotations.SerializedName

data class FingerprintProfile(
    @SerializedName("id_fingerprint")
    val idFingerprint: String,
    @SerializedName("alias_fingerprint")
    val aliasFingerprint: String,
    @SerializedName("url_fingerprint")
    val urlFingerprint: String,
    @SerializedName("main_fingerprint")
    val mainFingerprint: Boolean,
    @SerializedName("created_time")
    val createdTime: String
)


data class FingerprintBasic(
    @SerializedName("id_client")
    val idClient: String,
    @SerializedName("alias_fingerprint")
    val aliasFingerprint: String,
    @SerializedName("main_fingerprint")
    val mainFingerprint: Boolean
)


data class FingerprintSample(
    val fingerprint: String
)


data class FingerprintSamples(
    val fingerprints: List<String>
)


data class FingerprintRegister(
    val metadata: FingerprintBasic,
    val samples: FingerprintSamples
)