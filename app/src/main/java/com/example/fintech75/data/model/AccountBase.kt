package com.example.fintech75.data.model

import com.google.gson.annotations.SerializedName

data class AccountBase(
    @SerializedName("id_account")
    val idAccount: Int,
    @SerializedName("alias_account")
    val aliasAccount: String,
    @SerializedName("paypal_email")
    val paypalEmail: String,
    @SerializedName("main_account")
    val mainAccount: Boolean
)


data class AccountRegister(
    @SerializedName("id_user")
    val idUser: Int,
    @SerializedName("alias_account")
    val aliasAccount: String,
    @SerializedName("paypal_email")
    val paypalEmail: String,
    @SerializedName("paypal_id_client")
    val paypalIdClient: String,
    @SerializedName("paypal_secret")
    val paypalSecret: String,
    @SerializedName("type_owner")
    val typeOwner: String,
    @SerializedName("main_account")
    val mainAccount: Boolean
)
