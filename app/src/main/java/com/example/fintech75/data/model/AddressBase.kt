package com.example.fintech75.data.model

import com.google.gson.annotations.SerializedName

data class AddressBase(
    @SerializedName("id_address")
    val idAddress: Int,
    @SerializedName("zip_code")
    val zipCode: String,
    val state: String,
    val city: String? = null,
    val neighborhood: String? = null,
    val street: String? = null,
    @SerializedName("ext_number")
    val extNumber: String,
    @SerializedName("inner_number")
    val innerNumber: String? = null
)


data class AddressRegister(
    @SerializedName("id_branch")
    val idBranch: String? = null,
    @SerializedName("id_client")
    val idClient: String? = null,
    @SerializedName("type_owner")
    val typeOwner: String,
    @SerializedName("is_main")
    val isMain: Boolean,
    @SerializedName("zip_code")
    val zipCode: String,
    val state: String,
    val city: String? = null,
    val neighborhood: String? = null,
    val street: String? = null,
    @SerializedName("ext_number")
    val extNumber: String,
    @SerializedName("inner_number")
    val innerNumber: String? = null
)
