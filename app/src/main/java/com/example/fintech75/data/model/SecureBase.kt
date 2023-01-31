package com.example.fintech75.data.model

import com.google.gson.annotations.SerializedName

data class SecureBase(
    @SerializedName("data")
    val data: String,
    @SerializedName("secure")
    val secure: String
)

data class SecureRequest(
    @SerializedName("data")
    val data: String,
    @SerializedName("secure")
    val secure: String,
    @SerializedName("public_pem")
    val publicPem: String? = null
)

