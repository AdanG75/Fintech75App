package com.example.fintech75.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

data class TokenBase(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("user_id")
    val userID: Int,
    @SerializedName("type_user")
    val typeUser: String,
    @SerializedName("id_type")
    val idType: String
)

@Parcelize
data class UserCredential(
    val token: String,
    val userID: Int,
    val typeUser: String,
    val idType: String,
    val email: String = "N/A"
): Parcelable
