package com.example.fintech75.data.model

import com.google.gson.annotations.SerializedName

data class ClientBase(
    @SerializedName("id_user")
    val idUser: Int,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("id_client")
    val idClient: String,
    @SerializedName("birth_date")
    val birthDate: String,
    @SerializedName("age")
    val age: Int? = null
)


data class ClientBasic(
    @SerializedName("id_user")
    val idUser: Int,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("birth_date")
    val birthDate: String
)


data class ClientComplete(
    @SerializedName("id_user")
    val idUser: Int,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("id_client")
    val idClient: String,
    @SerializedName("birth_date")
    val birthDate: String,
    @SerializedName("age")
    val age: Int? = null,
    val addresses: List<AddressBase>,
    val fingerprints: List<FingerprintProfile>
)


data class ClientProfile(
    val user: UserBase,
    val client: ClientComplete
)


data class ClientRegister(
    val user: UserRegister,
    val client: ClientBasic,
    val address: AddressRegister
)
