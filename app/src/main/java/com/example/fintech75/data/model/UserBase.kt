package com.example.fintech75.data.model

import com.google.gson.annotations.SerializedName

data class UserBase(
    val email: String,
    val name: String,
    val phone: String,
    @SerializedName("type_user")
    val typeUser: String,
    @SerializedName("id_user")
    val idUser: Int,
    @SerializedName("created_time")
    val createdTime: String
)


data class UserBasic(
    val email: String,
    val name: String,
    val phone: String,
    @SerializedName("type_user")
    val typeUser: String,
)


data class UserAccount(
    val email: String,
    val name: String,
    val phone: String,
    @SerializedName("type_user")
    val typeUser: String,
    @SerializedName("id_user")
    val idUser: Int,
    @SerializedName("created_time")
    val createdTime: String,
    val accounts: List<AccountBase>
)


data class UserRegister(
    val email: String,
    val name: String,
    val phone: String,
    @SerializedName("type_user")
    val typeUser: String,
    val password: String,
    @SerializedName("public_key")
    val publicKey: String? = null
)
